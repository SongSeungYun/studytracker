from django.db import models
from django.conf import settings
from django.utils import timezone
from django.core.validators import MinValueValidator, MaxValueValidator

# Enum-like choices for StudyEvent state
class StudyEventState(models.TextChoices):
    STUDY = 'STUDY', 'Study'
    DISTRACTED = 'DISTRACTED', 'Distracted'
    AWAY = 'AWAY', 'Away'

class StudySession(models.Model):
    """
    Represents a single study session for a user.
    Includes the specific allowed objects for this session.
    """
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='study_sessions'
    )
    start_time = models.DateTimeField()
    end_time = models.DateTimeField(null=True, blank=True) # Can be null if session is ongoing
    allowed_objects = models.JSONField(
        default=list,
        help_text="JSON list of object names considered 'study' objects for this session."
    )
    total_duration_sec = models.IntegerField(default=0)
    study_duration_sec = models.IntegerField(default=0)
    distracted_duration_sec = models.IntegerField(default=0)
    away_duration_sec = models.IntegerField(default=0)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Session {self.id} by {self.user.username} from {self.start_time.strftime('%Y-%m-%d %H:%M')}"

    class Meta:
        ordering = ['-created_at']

class StudyEvent(models.Model):
    """
    Records a state change event within a study session.
    """
    session = models.ForeignKey(
        StudySession,
        on_delete=models.CASCADE,
        related_name='events'
    )
    timestamp = models.DateTimeField()
    state = models.CharField(
        max_length=20,
        choices=StudyEventState.choices,
        default=StudyEventState.STUDY
    )
    confidence = models.FloatField(
        null=True, blank=True,
        validators=[MinValueValidator(0.0), MaxValueValidator(1.0)],
        help_text="Confidence score of the YOLO detection for this event."
    )

    def __str__(self):
        return f"Event {self.id} - {self.state} in Session {self.session.id} at {self.timestamp.strftime('%H:%M:%S')}"

    class Meta:
        ordering = ['timestamp']
        unique_together = ('session', 'timestamp') # Ensure no duplicate events at same time for same session

class StudyImage(models.Model):
    """
    Stores metadata for images captured during a study session,
    linked to a specific event if applicable.
    """
    session = models.ForeignKey(
        StudySession,
        on_delete=models.CASCADE,
        related_name='images'
    )
    event = models.ForeignKey(
        StudyEvent,
        on_delete=models.SET_NULL, # If an event is deleted, don't delete the image metadata
        null=True, blank=True,
        related_name='captured_images'
    )
    image = models.ImageField(
        upload_to='studytracker_images/%Y/%m/%d/', # Consistent media path
        help_text="Captured image related to a study event or session."
    )
    captured_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Image {self.id} for Session {self.session.id} at {self.captured_at.strftime('%Y-%m-%d %H:%M:%S')}"

    class Meta:
        ordering = ['-captured_at']
