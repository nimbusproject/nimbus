from django import forms

class NewUserForm(forms.Form):
    username = forms.CharField()
    firstname = forms.CharField()
    lastname = forms.CharField()
    email = forms.EmailField()
    cert = forms.FileField(required=False)
    key = forms.FileField(required=False)
    
    class KEYS:
        """don't screw up typing strings in multiple places"""
        username = "username"
        firstname = "firstname"
        lastname = "lastname"
        email = "email"
        cert = "cert"
        key = "key"

    def clean_common(self, name):
        """Adds strip() to string validation.  
        The django cleaning functions accept extraneous spaces, even strings that are just spaces.  (email validation does not)
        """
        x = self.cleaned_data[name]
        x = x.strip()
        if not x:
            raise forms.ValidationError("Must contain actual characters")
        return x

    def clean_username(self):
        return self.clean_common(self.KEYS.username)

    def clean_firstname(self):
        return self.clean_common(self.KEYS.firstname)
        
    def clean_lastname(self):
        return self.clean_common(self.KEYS.lastname)
