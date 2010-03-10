import sys
from django.conf import settings
from nimbusrest.admin.connection import AdminConnection
import models 


def nimbus_user_create_remote(user_instance, nimbus_key=None, nimbus_secret=None):
    """Use the Nimbus API to register a new Nimbus User.
    
    `user_instance` is a `Django User` instance.
    """
    service_uri = getattr(settings, "NIMBUS_SERVICE_URL", None)
    conn = AdminConnection(service_uri, nimbus_key, nimbus_secret)
    nimbus_user = conn.add_user(user_instance)
    return nimbus_user

def nimbus_user_create(sender, instance, **kwargs):
    """Django User model `post_save` function.

    Save response data from a successful `Nimbus User`
    creation attempt, or, in the case of failure, remove 
    the recently created `Django User` and send failure 
    message back to User create Form.
    
    Notes:
        - Only attempt to create Nimbus User on Django User creation.
    """
    if kwargs.get('created') and instance.id != 1:
        remote_user_creator = kwargs.get("remote_user_creator")
        if remote_user_creator is None:
            remote_user_creator = nimbus_user_create_remote
        try:
            nimbus_user = remote_user_creator(instance)
        except:
            #Nimbus User failed to be created, delete Django User
            instance.delete() #XXX  how to handle this?
            raise Exception(sys.exc_info())
        up, created = models.UserProfile.objects.get_or_create(user=instance)
        up.nimbus_userid = nimbus_user.user_id
        up.save()
        return True
