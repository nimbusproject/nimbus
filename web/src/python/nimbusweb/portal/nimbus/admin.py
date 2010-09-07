# customizes the django admin app to include our objects

from django.contrib import admin
from django.contrib.auth.models import User
from models import UserProfile

class UserProfileInline(admin.StackedInline):
    model = UserProfile

class UserAdmin(admin.ModelAdmin):
    inlines = [ UserProfileInline ]

admin.site.unregister(User)
admin.site.register(User, UserAdmin)
