import os
import cv2
import pathlib
import requests
import time
from datetime import datetime
import json


class ChangeDetection:
    # HOST = "http://127.0.0.1:8000"  # Assumes Django server runs on this address
    HOST = "https://seungyun3.pythonanywhere.com"

    username = "admin"             # Hardcoded admin username
    password = "1234"              # Admin user's password

    _last_known_state = None
    _last_detected_study_objects = set()

    session_id = None
    token = None
    allowed_objects = []

    last_poll_time = time.time()
    POLL_INTERVAL_SECONDS = 5

    def __init__(self, detected_object_names_from_yolo):
        self.all_yolo_object_names = detected_object_names_from_yolo
        self._authenticate()
        # self._start_study_session()  # Removed: App will start session
        self._connect_to_active_session()  # Connect to app-started session

        self._last_known_state = "UNKNOWN"
        self._last_detected_study_objects = set()

    def _authenticate(self):
        print(f"Authenticating with server {self.HOST}/api/token/...")
        try:
            res = requests.post(
                f"{self.HOST}/api/token/",
                {
                    "username": self.username,
                    "password": self.password,
                },
            )
            res.raise_for_status()
            self.token = res.json()["token"]
            print("Authentication successful. Token obtained.")
        except requests.exceptions.RequestException as e:
            print(f"Authentication failed: {e}")
            print(
                "Please ensure the Django server is running and "
                "'admin' user exists with correct password."
            )
            exit(1)

    def _connect_to_active_session(self):
        """Polls for an active session started by the Android app."""
        print("Checking for active session started by the app...")
        headers = {
            "Authorization": f"Token {self.token}",
            "Accept": "application/json",
        }
        try:
            res = requests.get(
                f"{self.HOST}/api/study/sessions/active/",
                headers=headers,
            )
            res.raise_for_status()
            session_data = res.json()
            self.session_id = session_data["id"]
            self.allowed_objects = session_data.get("allowed_objects", [])
            print(
                f"Connected to active session {self.session_id}. "
                f"Initial allowed_objects: {self.allowed_objects}"
            )
            return True
        except requests.exceptions.RequestException as e:
            if e.response is not None and e.response.status_code == 404:
                print("No active session found. Waiting for app to start one.")
            else:
                print(f"Failed to connect to active session: {e}")
            self.session_id = None
            self.allowed_objects = []
            return False

    def _get_current_allowed_objects(self):
        if not self.session_id:
            if not self._connect_to_active_session():
                return

        headers = {
            "Authorization": f"Token {self.token}",
            "Accept": "application/json",
        }
        try:
            res = requests.get(
                f"{self.HOST}/api/study/sessions/{self.session_id}/config/",
                headers=headers,
            )
            res.raise_for_status()
            config_data = res.json()
            self.allowed_objects = config_data.get("allowed_objects", [])
        except requests.exceptions.RequestException as e:
            print(
                f"Failed to poll allowed_objects for session "
                f"{self.session_id}: {e}. Keeping previous list."
            )
            self.allowed_objects = []

    def check_for_object_updates(self):
        current_time = time.time()
        if (current_time - self.last_poll_time) > self.POLL_INTERVAL_SECONDS:
            self._get_current_allowed_objects()
            self.last_poll_time = current_time

    def process_frame(
        self,
        detected_objects_in_frame,
        confidence_scores_in_frame,
        save_dir,
        image_data,
    ):
        if not self.session_id:
            print("No active session. Attempting to connect...")
            if not self._connect_to_active_session():
                time.sleep(1)
                return

        self.check_for_object_updates()

        current_study_objects_set = {
            obj for obj in detected_objects_in_frame
            if obj in self.allowed_objects
        }

        current_state = self._determine_current_state(
            detected_objects_in_frame,
            current_study_objects_set,
        )

        send_image_and_event = False
        event_reason = ""

        if current_state != self._last_known_state:
            event_reason += (
                f"State changed: {self._last_known_state} -> {current_state}. "
            )
            send_image_and_event = True
            self._last_known_state = current_state

        newly_appeared_study_objects = (
            current_study_objects_set - self._last_detected_study_objects
        )
        if newly_appeared_study_objects:
            event_reason += (
                "New study object(s) appeared: "
                f"{', '.join(newly_appeared_study_objects)}. "
            )
            send_image_and_event = True

        self._last_detected_study_objects = current_study_objects_set

        if send_image_and_event:
            avg_confidence = (
                sum(confidence_scores_in_frame) / len(confidence_scores_in_frame)
                if confidence_scores_in_frame
                else 0.0
            )
            self._send_event_and_image(
                current_state,
                avg_confidence,
                save_dir,
                image_data,
                event_reason.strip(),
            )

    def _determine_current_state(
        self,
        all_detected_objects_list,
        current_study_objects_set,
    ):
        person_detected = "person" in all_detected_objects_list

        cell_phone_detected_and_not_allowed = (
            "cell phone" in all_detected_objects_list
            and "cell phone" not in self.allowed_objects
        )

        if not person_detected:
            return "AWAY"

        if cell_phone_detected_and_not_allowed:
            return "DISTRACTED"

        if current_study_objects_set:
            return "STUDY"

        return "DISTRACTED"

    def _send_event_and_image(
        self,
        state,
        confidence,
        save_dir,
        image_data,
        reason_message="",
    ):
        print(f"Sending event '{state}' (Reason: {reason_message}) and image...")
        event_timestamp = datetime.now()

        event_data = {
            "session": self.session_id,
            "timestamp": event_timestamp.isoformat(timespec="seconds"),
            "state": state,
            "confidence": round(confidence, 4),
        }

        headers = {
            "Authorization": f"Token {self.token}",
            "Content-Type": "application/json",
        }

        try:
            event_res = requests.post(
                f"{self.HOST}/api/edge/events/",
                headers=headers,
                data=json.dumps(event_data),
            )
            event_res.raise_for_status()
            event_id = event_res.json().get("id")
            print(
                f"Event '{state}' sent. Event ID: {event_id}. "
                f"Response: {event_res.status_code}"
            )
        except requests.exceptions.RequestException as e:
            print(
                f"Failed to send study event: {e}. "
                f"Response: {e.response.text if e.response else 'No response'}"
            )
            event_id = None

        save_path_base = pathlib.Path(os.getcwd()) / save_dir
        local_image_save_path = (
            save_path_base
            / "studytracker_images_local_cache"
            / str(event_timestamp.year)
            / str(event_timestamp.month).zfill(2)
            / str(event_timestamp.day).zfill(2)
        )
        local_image_save_path.mkdir(parents=True, exist_ok=True)

        image_filename = (
            f"session_{self.session_id}_"
            f"{event_timestamp.strftime('%Y%m%d_%H%M%S%f')}.jpg"
        )
        full_local_image_path = local_image_save_path / image_filename

        if image_data is not None and image_data.size > 0:
            resized_image = cv2.resize(
                image_data,
                dsize=(320, 240),
                interpolation=cv2.INTER_AREA,
            )
            cv2.imwrite(str(full_local_image_path), resized_image)
            print(f"Image saved locally to cache: {full_local_image_path}")
        else:
            print("No image data provided to save locally.")
            full_local_image_path = None

        if full_local_image_path and full_local_image_path.exists():
            image_data_payload = {
                "session": self.session_id,
                "event": event_id,
                "captured_at": event_timestamp.isoformat(timespec="seconds"),
            }
            image_headers = {
                "Authorization": f"Token {self.token}",
            }

            try:
                with open(full_local_image_path, "rb") as f:
                    files = {
                        "image": (image_filename, f, "image/jpeg"),
                    }
                    image_res = requests.post(
                        f"{self.HOST}/api/edge/images/",
                        headers=image_headers,
                        data=image_data_payload,
                        files=files,
                    )
                    image_res.raise_for_status()
                    print(
                        f"Study image sent to server. "
                        f"Response: {image_res.status_code}, {image_res.json()}"
                    )
            except requests.exceptions.RequestException as e:
                print(
                    f"Failed to send study image: {e}. "
                    f"Response: {e.response.text if e.response else 'No response'}"
                )
            except FileNotFoundError:
                print(
                    f"Local image file not found for sending: "
                    f"{full_local_image_path}"
                )
        else:
            print(
                "Skipping image send to server as no local image was available "
                "or saved."
            )
