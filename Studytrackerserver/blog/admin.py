from django.contrib import admin
from .models import StudySession, StudyEvent, StudyImage

# Register your models here to make them accessible in the Django Admin.
admin.site.register(StudySession)
admin.site.register(StudyEvent)
admin.site.register(StudyImage)

