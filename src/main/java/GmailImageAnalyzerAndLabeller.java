import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.cloud.vision.v1.*;
import com.google.common.collect.ImmutableList;
import org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;


import com.google.cloud.vision.v1.Feature.Type;
import com.google.protobuf.ByteString;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Imports the Google Cloud client library

public class GmailImageAnalyzerAndLabeller {
    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String ANALYZED_PARENT_LABEL = "Z";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = ImmutableList.of(GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_MODIFY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String ANIMALS_FILE_PATH = "animals.csv";


    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GmailImageAnalyzerAndLabeller.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }


    /**
     * List all Messages of the user's mailbox matching the query.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param query String used to filter the Messages listed.
     * @throws IOException
     */
    public static List<Message> listMessagesMatchingQuery(Gmail service, String userId,
                                                          String query) throws IOException {
        ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();

        List<Message> messages = new ArrayList<Message>();
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = service.users().messages().list(userId).setQ(query)
                        .setPageToken(pageToken).execute();
            } else {
                break;
            }
        }
        return messages;
    }

    public static List<byte[]> getAttachmentsBytes(Gmail service, String userId, String messageId)
            throws IOException {
        Message message = service.users().messages().get(userId, messageId).execute();
        List<MessagePart> parts = message.getPayload().getParts();
        List<byte[]> attachmentImageByteArrays = new ArrayList<>();
        for (MessagePart part : parts) {
            // maybe check file ending if its a pic ?
            if (part.getFilename() != null && part.getFilename().length() > 0) {
                String filename = part.getFilename();
                String attId = part.getBody().getAttachmentId();
                MessagePartBody attachPart = service.users().messages().attachments().
                        get(userId, messageId, attId).execute();

                Base64 base64Url = new Base64(true);
                byte[] fileByteArray = base64Url.decodeBase64(attachPart.getData());
                attachmentImageByteArrays.add(fileByteArray);
            }
        }
        return attachmentImageByteArrays;
    }
    /**
     * Detects localized objects in the specified local image.
     *
     * @param imgByteArray
     * @throws Exception on errors while closing the client.
     * @throws IOException on Input/Output errors.
     * @return List<EntityAnnotation> annotations of the analyzed image
     */
    public static List<LocalizedObjectAnnotation> detectLocalizedObjects(byte[] imgByteArray)
            throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ByteString imgBytes = ByteString.copyFrom(imgByteArray);

        Image img = Image.newBuilder().setContent(imgBytes).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder()
                        .addFeatures(Feature.newBuilder().setType(Type.OBJECT_LOCALIZATION))
                        .setImage(img)
                        .build();
        requests.add(request);

        // Perform the request
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            // get first response (only sent in one image in the batch so should only be one response)
            // Possible to do is to handle emails with several image attachments.
            AnnotateImageResponse res = responses.get(0);

            if (res.hasError()) {
                System.out.printf("Error: %s\n", res.getError().getMessage());
            }

            // Display the results
            for (AnnotateImageResponse resu : responses) {
                for (LocalizedObjectAnnotation entity : resu.getLocalizedObjectAnnotationsList()) {
                    System.out.println("Object name: " + entity.getName());
                    System.out.println("Confidence: " + entity.getScore());
                }
            }

            return res.getLocalizedObjectAnnotationsList();

        }
    }
    public static List<EntityAnnotation> annotateImageWithGoogleVision(byte[] imgByteArray) throws IOException {

    try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
        ByteString imgBytes = ByteString.copyFrom(imgByteArray);

        // Builds the image annotation request
        List<AnnotateImageRequest> requests = new ArrayList<>();
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.LABEL_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();
        requests.add(request);

        // Performs label detection on the image file
        BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
        List<AnnotateImageResponse> responses = response.getResponsesList();

        // get first response (only sent in one image in the batch so should only be one response)
        // Possible to do is to handle emails with several image attachments.
        AnnotateImageResponse res = responses.get(0);

        if (res.hasError()) {
            System.out.printf("Error: %s\n", res.getError().getMessage());
        }

        return res.getLabelAnnotationsList();
    }}

    /**
     * Modify the labels a message is associated with.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param messageId ID of Message to Modify.
     * @param labelsToAdd List of label ids to add.
     * @param labelsToRemove List of label ids to remove.
     * @throws IOException
     */
    public static void modifyMessage(Gmail service, String userId, String messageId,
                                     List<String> labelsToAdd, List<String> labelsToRemove) throws IOException {
        ModifyMessageRequest mods = new ModifyMessageRequest().setAddLabelIds(labelsToAdd)
                .setRemoveLabelIds(labelsToRemove);
        Message message = service.users().messages().modify(userId, messageId, mods).execute();
    }

     /**
     * List the Labels in the user's mailbox.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @throws IOException
     */
    public static List<Label> listLabels(Gmail service, String userId) throws IOException {
        ListLabelsResponse response = service.users().labels().list(userId).execute();
        List<Label> labels = response.getLabels();

        return labels;
    }

    /**
     * Add a new Label to user's inbox.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param newLabelName Name of the new label.
     * @throws IOException
     */
    public static Label createLabel(Gmail service, String userId, String newLabelName)
            throws IOException {
        Label label = new Label()
                .setName(newLabelName)
                .setLabelListVisibility("labelShow")
                .setMessageListVisibility("show");

        label = service.users().labels().create(userId, label).execute();

        return label;
    }


    public static Label getLabelByName(Gmail service, String userId, String labelName) throws IOException {
        // Get all labels in account
        List<Label> labelsInAccount = listLabels(service, userId);
        // See if the label name exists
        for (Label label : labelsInAccount) {
            if(label.getName().equals(labelName)){
                return label;
            }
        }
        // Label does not seem to exist? -> create the label
        return createLabel(service, userId, labelName);
    }

    public static void labelEmailAndMarkAsProcessed(Gmail service, String userId, String emailId, List<String> labelNamesToAddToEmail)
            throws IOException {
        List<String> labelIdsToAdd = new ArrayList<String>();

        for (String labelNameToAdd : labelNamesToAddToEmail) {
            // add labels as children under parent label
            labelIdsToAdd.add(getLabelByName(service, userId,
                    ANALYZED_PARENT_LABEL+"/"+labelNameToAdd).getId());
        }

        // always add the "analyzed parent label"
        labelIdsToAdd.add(getLabelByName(service, userId, ANALYZED_PARENT_LABEL).getId());

        modifyMessage(service, userId, emailId, labelIdsToAdd, null);
    }

    public static EntityAnnotation getHighestScoringAnnotation(List<EntityAnnotation> annotations){
        double highestScore = 0;
        EntityAnnotation highestScoredAnnotation = null;
        if(!annotations.isEmpty()) {
            for (EntityAnnotation annotation : annotations) {
                if (annotation.getScore() > highestScore){
                    highestScoredAnnotation = annotation;
                    highestScore = annotation.getScore();
                }
            }
        }
        return highestScoredAnnotation;
    }

    public static List<String> getListOfAnimals() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream= classLoader.getResourceAsStream(ANIMALS_FILE_PATH);
        Reader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader csvReader = new BufferedReader(inputStreamReader);

        List<String> animals = new ArrayList<String>();
        String row = "";
        while ((row = csvReader.readLine()) != null) {
            animals.add(row.toLowerCase().trim());
        }
        csvReader.close();

        return animals;
    }

    public static void main(String... args) throws Exception {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Print the labels in the user's account.
        String user = "me";

        // Find emails
        // this is the same search string as a search would result in gmail ui
        String gmailSearchString = "filename:PIC.jpg newer_than:2d";


        List<Message> messages = listMessagesMatchingQuery(service, user, gmailSearchString
                        .concat(" AND NOT label:\"").concat(ANALYZED_PARENT_LABEL).concat("\"")
                );

        if(!messages.isEmpty()) {
            System.out.println("Number of messages fetched: " + messages.size());
        }
        // Process emails
        for (Message message : messages) {
            // download attachment
            System.out.println("Downloading attachment!");
            List<byte[]> imageByteArrays = getAttachmentsBytes(service, user, message.getId());

            for (byte[] imageByteArray:imageByteArrays) {
                System.out.println("Analyzing attachment for labels!");
                // Analyze attachment (google vision)
                List<EntityAnnotation> annotations = annotateImageWithGoogleVision(imageByteArray);
                for(EntityAnnotation annotation : annotations) {
                    System.out.println(annotation.toString());
                }

                // is any of the labels an animal!?
                // check against the list of animals!
                List<String> labels = new ArrayList<>();
                List<EntityAnnotation> animalAnnotations = getAnimalAnnotations(annotations);

                if(animalAnnotations!= null) {
                    for (EntityAnnotation annotation: animalAnnotations) {
                        labels.add(annotation.getDescription());
                        System.out.println("Spotted a " + annotation.getDescription() +"!");
                    }
                } else {
                    // no animals found :(
                    // take the highest scoring label
                    labels.add(getHighestScoringAnnotation(annotations).getDescription());
                }

                System.out.println("Analyzing attachment for objects!");
                List<LocalizedObjectAnnotation> objectAnnotations = detectLocalizedObjects(imageByteArray);
                for(LocalizedObjectAnnotation annotation : objectAnnotations) {
                    System.out.println(annotation.toString());
                    labels.add(annotation.getName());
                }

                labelEmailAndMarkAsProcessed(service, user, message.getId(), labels);

            }
        }
    }

    private static List<EntityAnnotation> getAnimalAnnotations(List<EntityAnnotation> annotations) throws IOException {
        List<EntityAnnotation> animalAnnotations = new ArrayList<>();

        for(EntityAnnotation annotation : annotations) {
            if(isAnimal(annotation.getDescription())){
                animalAnnotations.add(annotation);
            }
        }
        return animalAnnotations;
    }

    public static boolean isAnimal(String description) throws IOException {
        List<String> animals = getListOfAnimals();
        return animals.contains(description.toLowerCase().trim());
    }
}