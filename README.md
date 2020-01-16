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


# Building
mvn clean compile assembly:single

Resulting jar pops out in target/GmailImageAnalyzerAndLabeller.jar

# How to deploy / run
## Running the first time 
- Set GOOGLE_APPLICATION_CREDENTIALS to the service account connected to your GCP project.
- $java -jar target/GmailImageAnalyzerAndLabeller.jar  
- Fill out the Oauth prompt to grant the application (i.e. your GCP project) access to the gmail acount you want to use.  

Finished! the program should run. 

## Running with saved token (running consequent times after first)
Make sure tokens folder is in the same dir as current dir. 

$java -jar GmailImageAnalyzerAndLabeller.jar