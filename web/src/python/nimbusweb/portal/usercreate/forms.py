from django import forms

class NewUserForm(forms.Form):
    username = forms.CharField()
    firstname = forms.CharField()
    lastname = forms.CharField()
    email = forms.EmailField()

class CertKeyForm(NewUserForm): 
    cert = forms.FileField()
    key = forms.FileField()

class DNForm(NewUserForm):
    DN = forms.CharField()

class AutoCreateForm(NewUserForm):
    pass
