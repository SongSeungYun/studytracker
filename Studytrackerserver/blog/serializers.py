from rest_framework import serializers
from .models import StudySession, StudyEvent, StudyImage, StudyEventState
from django.utils import timezone

class StudySessionSerializer(serializers.ModelSerializer):
    user = serializers.PrimaryKeyRelatedField(read_only=True, default=serializers.CurrentUserDefault())
    current_status = serializers.SerializerMethodField()
    current_total_duration_sec = serializers.SerializerMethodField()
    current_study_duration_sec = serializers.SerializerMethodField()
    current_distracted_duration_sec = serializers.SerializerMethodField()
    current_away_duration_sec = serializers.SerializerMethodField()

    class Meta:
        model = StudySession
        fields = [
            'id', 'user', 'start_time', 'end_time', 'allowed_objects',
            'total_duration_sec', 'study_duration_sec', 'distracted_duration_sec',
            'away_duration_sec', 'created_at',
            'current_status', 'current_total_duration_sec', 'current_study_duration_sec',
            'current_distracted_duration_sec', 'current_away_duration_sec'
        ]
        read_only_fields = [
            'id', 'start_time', 'created_at', 'total_duration_sec', # These stored fields are only final upon session end
            'study_duration_sec', 'distracted_duration_sec', 'away_duration_sec'
        ]
    
    def get_current_status(self, obj):
        latest_event = obj.events.order_by('-timestamp').first()
        return latest_event.state if latest_event else StudyEventState.AWAY.value # Default to AWAY if no events yet

    def _calculate_durations_for_active_session(self, obj):
        if obj.end_time is not None: # If session has ended, use the stored final values
            return {
                'total': obj.total_duration_sec,
                'study': obj.study_duration_sec,
                'distracted': obj.distracted_duration_sec,
                'away': obj.away_duration_sec
            }
        
        # Session is ongoing, calculate dynamically up to now
        current_time = timezone.now()
        events = obj.events.filter(timestamp__lte=current_time).order_by('timestamp')
        
        total_duration = (current_time - obj.start_time).total_seconds()
        study_duration = 0
        distracted_duration = 0
        away_duration = 0

        if not events.exists():
            # If no events yet, assume AWAY for the entire duration since session start
            away_duration = total_duration
            return {
                'total': int(total_duration),
                'study': 0,
                'distracted': 0,
                'away': int(away_duration)
            }

        last_time_point = obj.start_time
        last_state = StudyEventState.AWAY.value # Default state before the first event or if session starts with no objects

        for event in events:
            if event.timestamp > last_time_point:
                duration = (event.timestamp - last_time_point).total_seconds()
                if last_state == StudyEventState.STUDY.value:
                    study_duration += duration
                elif last_state == StudyEventState.DISTRACTED.value:
                    distracted_duration += duration
                elif last_state == StudyEventState.AWAY.value:
                    away_duration += duration
            last_time_point = event.timestamp
            last_state = event.state # State for the next interval (starting from this event's timestamp)

        # Add duration from the last event to now
        duration_to_now = (current_time - last_time_point).total_seconds()
        if last_state == StudyEventState.STUDY.value:
            study_duration += duration_to_now
        elif last_state == StudyEventState.DISTRACTED.value:
            distracted_duration += duration_to_now
        elif last_state == StudyEventState.AWAY.value:
            away_duration += duration_to_now

        return {
            'total': int(total_duration),
            'study': int(study_duration),
            'distracted': int(distracted_duration),
            'away': int(away_duration)
        }

    def get_current_total_duration_sec(self, obj):
        return self._calculate_durations_for_active_session(obj)['total']

    def get_current_study_duration_sec(self, obj):
        return self._calculate_durations_for_active_session(obj)['study']

    def get_current_distracted_duration_sec(self, obj):
        return self._calculate_durations_for_active_session(obj)['distracted']

    def get_current_away_duration_sec(self, obj):
        return self._calculate_durations_for_active_session(obj)['away']


class StudyEventSerializer(serializers.ModelSerializer):
    class Meta:
        model = StudyEvent
        fields = '__all__' # id, session, timestamp, state, confidence
        read_only_fields = ['id'] # id는 자동으로 생성

class StudyImageSerializer(serializers.ModelSerializer):
    class Meta:
        model = StudyImage
        fields = '__all__' # id, session, event, image, captured_at
        read_only_fields = ['id', 'captured_at'] # id와 captured_at은 자동으로 생성
