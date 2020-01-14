# image-label-gmail
This program labels emails in Gmail after analyzing attached images with Google Vision. 

# GCP APIs 
In order to use the program you need a GCP project with the following APIs enabled:  
 
 - Google Vision API
 - GMail API 
 
# Credentials for GCP
Service account with access for Google Vision set env. variable: GOOGLE_APPLICATION_CREDENTIALS.

Store the credentials for OAuth 2.0 client ID (for the GCP project) in file "src/main/resources/credentials.json"

# OAuth Authentication
The first time you run the program a web browser will open where you choose which Gmail account the program 
should get access to - this generates a token which is stored in "tokens".   
