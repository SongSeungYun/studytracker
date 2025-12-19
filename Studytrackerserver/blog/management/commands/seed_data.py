import random
from datetime import timedelta
from django.core.management.base import BaseCommand
from django.utils import timezone
from django.contrib.auth import get_user_model
from blog.models import StudySession, StudyEvent, StudyEventState

User = get_user_model()

class Command(BaseCommand):
    help = 'Seeds the database with dummy study session data.'

    def handle(self, *args, **kwargs):
        self.stdout.write("Deleting old data...")
        # 기존 데이터 삭제 (새로운 테스트를 위해)
        StudySession.objects.all().delete()
        StudyEvent.objects.all().delete()

        # 'admin' 사용자 가져오기
        try:
            user = User.objects.get(username='admin')
        except User.DoesNotExist:
            self.stdout.write(self.style.ERROR("Admin user not found. Please create it first using 'createsuperuser'."))
            return

        self.stdout.write("Seeding database with new dummy data...")

        # 지난 15일간의 데이터 생성
        now = timezone.now()
        for i in range(15):
            # 하루에 1~3개의 세션 생성
            for _ in range(random.randint(1, 3)):
                session_start_time = now - timedelta(days=i, hours=random.randint(0, 12))
                session_end_time = session_start_time + timedelta(minutes=random.randint(30, 180))
                
                session = StudySession.objects.create(
                    user=user,
                    start_time=session_start_time,
                    end_time=session_end_time,
                    allowed_objects=['book', 'laptop'] # 예시
                )

                # 세션 내 이벤트 생성
                total_study = 0
                total_distracted = 0
                total_away = 0
                
                event_time = session_start_time
                while event_time < session_end_time:
                    state = random.choice([
                        StudyEventState.STUDY, 
                        StudyEventState.STUDY, 
                        StudyEventState.STUDY, # 공부 확률 높게
                        StudyEventState.DISTRACTED, 
                        StudyEventState.AWAY
                    ])
                    
                    # 다음 이벤트까지의 시간 (5분 ~ 20분 사이)
                    duration_minutes = random.randint(5, 20)
                    next_event_time = event_time + timedelta(minutes=duration_minutes)
                    
                    # 마지막 이벤트가 세션 종료 시간을 넘지 않도록 조정
                    if next_event_time > session_end_time:
                        next_event_time = session_end_time
                        duration_minutes = (next_event_time - event_time).total_seconds() / 60

                    if duration_minutes <= 0:
                        break

                    StudyEvent.objects.create(
                        session=session,
                        timestamp=event_time,
                        state=state,
                        confidence=random.uniform(0.85, 0.99)
                    )

                    # 상태별 시간 누적
                    duration_seconds = duration_minutes * 60
                    if state == StudyEventState.STUDY:
                        total_study += duration_seconds
                    elif state == StudyEventState.DISTRACTED:
                        total_distracted += duration_seconds
                    elif state == StudyEventState.AWAY:
                        total_away += duration_seconds
                    
                    event_time = next_event_time

                # 세션에 최종 집계 시간 저장
                session.study_duration_sec = int(total_study)
                session.distracted_duration_sec = int(total_distracted)
                session.away_duration_sec = int(total_away)
                session.total_duration_sec = int(total_study + total_distracted + total_away)
                session.save()

        self.stdout.write(self.style.SUCCESS("Successfully seeded the database."))
