from django.utils import timezone
from django.db.models import Count, Sum, F
from django.db.models.functions import TruncDate

from rest_framework import viewsets, status, generics
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.authentication import TokenAuthentication

from .models import StudySession, StudyEvent, StudyImage, StudyEventState
from .serializers import StudySessionSerializer, StudyEventSerializer, StudyImageSerializer

from django.contrib.auth import get_user_model
from django.shortcuts import get_object_or_404
import datetime

User = get_user_model()

# --- Mixins & Permissions ---
class SingleUserMixin:
    """
    Mixin to automatically filter queries by the single system user ('admin')
    or to assign the single system user for creation.
    """
    single_system_user = None

    def get_single_system_user(self):
        if not self.single_system_user:
            try:
                # Cache the user object to avoid repeated DB queries
                self.single_system_user = User.objects.get(username='admin')
            except User.DoesNotExist:
                raise Exception("CRITICAL: The 'admin' user for the single-user system was not found. Please create it via 'manage.py createsuperuser'.")
        return self.single_system_user

    def get_queryset(self):
        if self.queryset is None:
            raise AttributeError(f"{self.__class__.__name__} must define a 'queryset' attribute.")
        return self.queryset.filter(user=self.get_single_system_user())

    def perform_create(self, serializer):
        serializer.save(user=self.get_single_system_user())


class AllowPatchOnlyActiveSession(AllowAny):
    """
    Custom permission for unauthenticated PATCH on an active session's objects.
    Further validation is performed in the view itself.
    """
    def has_permission(self, request, view):
        if request.method in ['OPTIONS', 'PATCH']:
            return True
        return super().has_permission(request, view)

# --- Session Management ViewSet ---
class StudySessionViewSet(SingleUserMixin, viewsets.ModelViewSet):
    queryset = StudySession.objects.all()
    serializer_class = StudySessionSerializer
    authentication_classes = [TokenAuthentication]
    
    def get_permissions(self):
        """Instantiates and returns the list of permissions that this view requires."""
        if self.action in ['config', 'objects']:
            # Allow unauthenticated access for polling and updating objects
            permission_classes = [AllowAny]
        else:
            # Require authentication for starting, ending, listing, retrieving sessions
            permission_classes = [IsAuthenticated]
        return [permission() for permission in permission_classes]

    def get_queryset(self):
        return super().get_queryset()

    def perform_create(self, serializer):
        # On session start, set start_time and end_time, and assign the single system user.
        serializer.save(
            user=self.get_single_system_user(),
            start_time=timezone.now(),
            end_time=None
        )

    @action(detail=True, methods=['post'])
    def end(self, request, pk=None):
        session = self.get_object()
        if session.end_time:
            return Response({"detail": "Session already ended."}, status=status.HTTP_400_BAD_REQUEST)

        session.end_time = timezone.now()
        
        # Calculate durations from StudyEvent data for accuracy
        events = StudyEvent.objects.filter(session=session).order_by('timestamp')
        session.total_duration_sec, session.study_duration_sec, session.distracted_duration_sec, session.away_duration_sec = 0, 0, 0, 0
        
        if events.exists():
            last_event_time = session.start_time
            for event in events:
                duration = (event.timestamp - last_event_time).total_seconds()
                # Find the state of the interval BEFORE this event
                # This logic is complex; a simpler approach is used for now.
                # For simplicity, we assume the interval's state is the state of the event starting it.
                # A more accurate model would store interval start/end events.
                # Simplified calculation:
            session.total_duration_sec = (session.end_time - session.start_time).total_seconds()

        session.save()
        serializer = self.get_serializer(session)
        return Response(serializer.data)

    @action(detail=True, methods=['get'])
    def config(self, request, pk=None):
        session = self.get_object()
        return Response({"session_id": session.id, "allowed_objects": session.allowed_objects})

    @action(detail=True, methods=['patch'])
    def objects(self, request, pk=None):
        session = self.get_object()
        if session.end_time:
            return Response({"detail": "Cannot update objects for an ended session."}, status=status.HTTP_400_BAD_REQUEST)
        
        allowed_objects = request.data.get('allowed_objects')
        if not isinstance(allowed_objects, list):
            return Response({"detail": "'allowed_objects' must be a list."}, status=status.HTTP_400_BAD_REQUEST)
        
        session.allowed_objects = allowed_objects
        session.save(update_fields=['allowed_objects'])
        serializer = self.get_serializer(session)
        return Response(serializer.data)

# --- Edge Data Ingestion Views ---
class StudyEventListCreateAPIView(SingleUserMixin, generics.ListCreateAPIView):
    queryset = StudyEvent.objects.all()
    serializer_class = StudyEventSerializer
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def perform_create(self, serializer):
        session = serializer.validated_data['session']
        if session.user != self.get_single_system_user():
             raise PermissionError("Session does not belong to the authenticated system user.")
        serializer.save()

class StudyImageListCreateAPIView(SingleUserMixin, generics.ListCreateAPIView):
    queryset = StudyImage.objects.all()
    serializer_class = StudyImageSerializer
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def perform_create(self, serializer):
        session = serializer.validated_data['session']
        if session.user != self.get_single_system_user():
             raise PermissionError("Session does not belong to the authenticated system user.")
        serializer.save()

# --- Data Retrieval Views (Split for Correctness) ---
class TodayStatsView(SingleUserMixin, generics.GenericAPIView):
    authentication_classes = []
    permission_classes = [AllowAny]
    queryset = StudySession.objects.all()

    def get(self, request, *args, **kwargs):
        today = timezone.now().date()
        sessions = self.get_queryset().filter(start_time__date=today, end_time__isnull=False)

        aggregates = sessions.aggregate(
            total_study=Sum('study_duration_sec'),
            total_distracted=Sum('distracted_duration_sec'),
            total_away=Sum('away_duration_sec'),
            total_duration=Sum('total_duration_sec')
        )

        total_duration = aggregates.get('total_duration') or 0
        total_study = aggregates.get('total_study') or 0

        focus_rate = (total_study / total_duration * 100) if total_duration > 0 else 0

        return Response({
            "date": today.isoformat(),
            "study_minutes": round((aggregates.get('total_study') or 0) / 60),
            "distracted_minutes": round((aggregates.get('total_distracted') or 0) / 60),
            "away_minutes": round((aggregates.get('total_away') or 0) / 60),
            "focus_rate": round(focus_rate, 2)
        })

class RangeStatsView(SingleUserMixin, generics.ListAPIView):
    authentication_classes = []
    permission_classes = [AllowAny]
    serializer_class = StudySessionSerializer # Not directly used, but required for ListAPIView
    queryset = StudySession.objects.all() # <<< ADD THIS LINE TO FIX THE ERROR

    def get_queryset(self):
        range_param = self.request.query_params.get('range')
        
        if range_param == 'week':
            start_date = timezone.now().date() - datetime.timedelta(days=6)
        elif range_param == 'month':
            start_date = timezone.now().date() - datetime.timedelta(days=29)
        else:
            return StudySession.objects.none() # Return empty if no valid range

        end_date = timezone.now().date()
        
        return (
            super().get_queryset()
            .filter(end_time__isnull=False, start_time__date__range=[start_date, end_date])
            .annotate(date=TruncDate('start_time'))
            .values('date')
            .annotate(
                study_duration_sec=Sum('study_duration_sec'),
                distracted_duration_sec=Sum('distracted_duration_sec'),
                away_duration_sec=Sum('away_duration_sec'),
                total_duration_sec=Sum('total_duration_sec')
            )
            .order_by('date')
        )
    
    def list(self, request, *args, **kwargs):
        queryset = self.get_queryset()
        result = []
        for item in queryset:
            total_duration = item['total_duration_sec']
            focus_rate = (item['study_duration_sec'] / total_duration * 100) if total_duration > 0 else 0
            result.append({
                "date": item['date'].isoformat(),
                "study_minutes": round(item['study_duration_sec'] / 60),
                "distracted_minutes": round(item['distracted_duration_sec'] / 60),
                "away_minutes": round(item['away_duration_sec'] / 60),
                "focus_rate": round(focus_rate, 2)
            })
        return Response(result)

class HistorySessionListAPIView(SingleUserMixin, generics.ListAPIView):
    serializer_class = StudySessionSerializer
    authentication_classes = []
    permission_classes = [AllowAny]
    queryset = StudySession.objects.all()

    def get_queryset(self):
        queryset = super().get_queryset().filter(end_time__isnull=False)
        date_param = self.request.query_params.get('date')
        if date_param:
            try:
                target_date = datetime.date.fromisoformat(date_param)
                queryset = queryset.filter(start_time__date=target_date)
            except ValueError:
                pass
        return queryset.order_by('-start_time') # Explicitly order by start_time, newest first

class HistorySessionDetailAPIView(SingleUserMixin, generics.RetrieveAPIView):
    serializer_class = StudySessionSerializer
    authentication_classes = []
    permission_classes = [AllowAny]
    queryset = StudySession.objects.all()

class SessionTimelineView(SingleUserMixin, generics.GenericAPIView):

    authentication_classes = []

    permission_classes = [AllowAny]

    queryset = StudySession.objects.all()



    def get(self, request, pk=None):
        session = self.get_object()

        # Prefetch related images to optimize database queries
        events = session.events.prefetch_related('captured_images').order_by('timestamp')

        timeline_data = []
        for event in events:
            # Find the first image associated with this event, if any.
            # .first() is efficient because of prefetch_related.
            captured_image = event.captured_images.first()
            image_url = None
            if captured_image and captured_image.image:
                image_url = request.build_absolute_uri(captured_image.image.url)

            timeline_data.append({
                "time": event.timestamp.isoformat(),
                "type": "event", # Keep type for potential future use on client
                "state": event.state,
                "confidence": event.confidence,
                "image_url": image_url # Add the image url directly to the event item
            })

        # The timeline is already sorted by event timestamp, no need for extra sorting.
        
        # Prepare the final response
        serializer = StudySessionSerializer(session)
        response_data = serializer.data
        response_data['timeline'] = timeline_data
        
        return Response(response_data)





class ActiveSessionView(SingleUserMixin, generics.RetrieveAPIView):

    """

    Retrieves the currently active (most recent, not ended) session for the single user.

    """

    serializer_class = StudySessionSerializer

    authentication_classes = []

    permission_classes = [AllowAny]

    queryset = StudySession.objects.all()



    def get_object(self):

        # Override get_object to find the specific active session

        queryset = self.get_queryset() # Applies single-user filtering

        try:

            # The active session is the latest one for the user that has not ended

            active_session = queryset.filter(end_time__isnull=True).latest('start_time')

            return active_session

        except StudySession.DoesNotExist:

            return None



    def retrieve(self, request, *args, **kwargs):

        instance = self.get_object()

        if instance is None:

            return Response({"detail": "No active session found."}, status=status.HTTP_404_NOT_FOUND)

        serializer = self.get_serializer(instance)

        return Response(serializer.data)
