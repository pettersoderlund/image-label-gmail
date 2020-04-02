import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

class GmailImageAnalyzerAndLabellerTest {

    @Test
    void testIsAnimalLowerCase() throws IOException {
        String animal = "deer";
        Assertions.assertTrue(GmailImageAnalyzerAndLabeller.isAnimal(animal));
    }

    @Test
    void testIsAnimalCamelCase() throws IOException {
        String object = "Moose";
        Assertions.assertTrue(GmailImageAnalyzerAndLabeller.isAnimal(object));
    }

    @Test
    void testIsAnimalMixedCase() throws IOException {
        String object = "FoX";
        Assertions.assertTrue(GmailImageAnalyzerAndLabeller.isAnimal(object));
    }

    @Test
    void testIsAnimalNotAnimal() throws IOException {
        String object = "Person";
        Assertions.assertFalse(GmailImageAnalyzerAndLabeller.isAnimal(object));
    }

    @Test
    void testIsAnimalWhitspace() throws IOException {
        String object = "    ape  ";
        Assertions.assertTrue(GmailImageAnalyzerAndLabeller.isAnimal(object));
    }

    @Test
    void testClassifyImageObjectAsCat() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream= classLoader.getResourceAsStream("cat.jpg");

        BufferedImage bImage = ImageIO.read(inputStream);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(bImage, "jpg", bos );
        byte [] data = bos.toByteArray();
        Assertions.assertEquals("Cat", GmailImageAnalyzerAndLabeller.detectLocalizedObjects(data).get(0).getName());
    }

    @Test
    void testClassifyImageObjectAsCatFromUrl() throws IOException {
        URL url = new URL("https://live.staticflickr.com/7142/6437955381_8bbcb5952f_b.jpg");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = url.openStream ();
            byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
            int n;

            while ( (n = is.read(byteChunk)) > 0 ) {
                baos.write(byteChunk, 0, n);
            }
        }
        catch (IOException e) {
            System.err.printf ("Failed while reading bytes from %s: %s", url.toExternalForm(), e.getMessage());
            e.printStackTrace ();
            // Perform any other exception handling that's appropriate.
        }
        finally {
            if (is != null) { is.close(); }
        }
        byte [] data = baos.toByteArray();
        Assertions.assertEquals("Cat", GmailImageAnalyzerAndLabeller.detectLocalizedObjects(data).get(0).getName());
    }
}