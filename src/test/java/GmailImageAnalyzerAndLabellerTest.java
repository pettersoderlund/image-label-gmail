import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GmailImageAnalyzerAndLabellerTest {

    @Test
    void testIsAnimalLowerCase() throws IOException {
        String animal = "deer";
        assertTrue(GmailImageAnalyzerAndLabeller.isAnimal(animal));
    }

    @Test
    void testIsAnimalCamelCase() throws IOException {
        String object = "Moose";
        assertTrue(GmailImageAnalyzerAndLabeller.isAnimal(object));
    }

    @Test
    void testIsAnimalMixedCase() throws IOException {
        String object = "FoX";
        assertTrue(GmailImageAnalyzerAndLabeller.isAnimal(object));
    }

    @Test
    void testIsAnimalNotAnimal() throws IOException {
        String object = "Person";
        assertFalse(GmailImageAnalyzerAndLabeller.isAnimal(object));
    }

    @Test
    void testIsAnimalWhitspace() throws IOException {
        String object = "    ape  ";
        assertTrue(GmailImageAnalyzerAndLabeller.isAnimal(object));
    }
}