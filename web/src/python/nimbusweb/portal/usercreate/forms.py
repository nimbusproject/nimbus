from django import forms

class NewUserForm(forms.Form):
    username = forms.CharField()
    firstname = forms.CharField()
    lastname = forms.CharField()
    email = forms.EmailField()

class CertForm(NewUserForm): 
    cert = forms.FileField()

class DNForm(NewUserForm):
    DN = forms.CharField()

class AutoCreateForm(NewUserForm):
    pass
