import sys
from django.conf import settings
from nimbusrest.connection import Connection
import models 

def nimbus_user_create_remote(user_instance, path="/users/create", nimbus_key=None, nimbus_secret=None):
    """Use the Nimbus API to register a new Nimbus User.
    
    `user_instance` is a `Django User` instance.
    """
    service_uri = getattr(settings, "NIMBUS_SERVICE_URI", "http://some.sensible.default/")
    conn = Connection(service_uri, nimbus_key, nimbus_secret)
    body = {"username":user_instance.username} #XXX what else is needed?
    response = conn.post_json(path, body) #let caller handle errors
    return response

def nimbus_user_create(sender, instance, **kwargs):
    """Django User model `post_save` function.

    Save response data from a successful `Nimbus User`
    creation attempt, or, in the case of failure, remove 
    the recently created `Django User` and send failure 
    message back to User create Form.
    """
    print "complete_nimbus_user_create => ", sender, instance, kwargs
    remote_user_creator = kwargs.get("remote_user_creator")
    if remote_user_creator is None:
        remote_user_creator = nimbus_user_create_remote
    # only attempt to create Nimbus User on Django User creation.
    if kwargs.get('created'):
        response = remote_user_creator(instance)
        try:
            response = remote_user_creator(instance)
        except:
            #Nimbus User failed to be created, delete Django User
            instance.delete() #XXX  how to handle this?
            raise Exception(sys.exc_type)
        up = models.UserProfile.objects.get(user=instance)
        up.nimbus_userid = response["nimbus_userid"]
        up.save()
        return True
 
