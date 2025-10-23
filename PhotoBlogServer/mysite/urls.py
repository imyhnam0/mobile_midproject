from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static
from blog import views

urlpatterns = [
    path('admin/', admin.site.urls),
    path('', include('blog.urls')),
    path('', views.post_list, name='post_list'),
] + static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
