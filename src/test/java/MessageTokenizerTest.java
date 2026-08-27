import junit.framework.*;

import java.util.HashMap;

import org.redcraft.redcraftchat.models.translate.TokenizedMessage;
import org.redcraft.redcraftchat.tokenizer.TokenizerManager;

public class MessageTokenizerTest extends TestCase {

    public void testTokenizer() {
        String testMessage = "Hello <@lululombard#1337>, can you check `/contact.html` on https://redcraft.org please? 😎 You can also use /join if you want to 🤷‍♀️";

        String expectedRawTokenizedMessage = "Hello 280aa0a, can you check dc20cb6 on 1d36664 please? 6d8f62c You can also use 3eccd38 if you want to 05319c3";
        HashMap<String, String> expectedRawTokenizedElements = new HashMap<String, String>();
        expectedRawTokenizedElements.put("1d36664", "https://redcraft.org");
        expectedRawTokenizedElements.put("280aa0a", "<@lululombard#1337>");
        expectedRawTokenizedElements.put("dc20cb6", "`/contact.html`");
        expectedRawTokenizedElements.put("6d8f62c", ":sunglasses:");
        expectedRawTokenizedElements.put("3eccd38", "/join");
        expectedRawTokenizedElements.put("05319c3", ":female_shrug:");

        TokenizedMessage expectedTokenizedMessage = new TokenizedMessage(expectedRawTokenizedMessage, expectedRawTokenizedElements);
        TokenizedMessage actualTokenizedMessage = TokenizerManager.tokenizeElements(testMessage, false);
        assertEquals(expectedTokenizedMessage.toString(), actualTokenizedMessage.toString());
    }

    public void testLineBreaksReachTheEngine() {
        // Line returns used to be tokenized like everything else, which left
        // the hash glued to the words on either side and hid the line
        // structure from the engine exactly when it mattered most: a multi
        // line message is translated as one text so the lines give each other
        // context, and it can only keep them apart if it can see them
        String testMessage = "Welcome to the\nCreative Build\nserver!";

        TokenizedMessage tokenized = TokenizerManager.tokenizeElements(testMessage, false);

        assertEquals(testMessage, tokenized.getOriginalTokenizedMessage());
        assertEquals(3, tokenized.getOriginalTokenizedMessage().split("\n", -1).length);
        assertEquals(testMessage, TokenizerManager.untokenizeElements(tokenized));
    }

    public void testUntokenizer() {
        String testRawMessage = "Hello 280aa0a, can you check dc20cb6 on 1d36664 please? 6d8f62c You can also use 3eccd38 if you want to 🤷‍♀️";

        HashMap<String, String> testRawTokenizedElements = new HashMap<String, String>();
        testRawTokenizedElements.put("1d36664", "https://redcraft.org");
        testRawTokenizedElements.put("280aa0a", "<@lululombard#1337>");
        testRawTokenizedElements.put("dc20cb6", "`/contact.html`");
        testRawTokenizedElements.put("6d8f62c", ":sunglasses:");
        testRawTokenizedElements.put("3eccd38", "/join");
        testRawTokenizedElements.put("05319c3", ":female_shrug:");
        TokenizedMessage testTokenizedMessage = new TokenizedMessage(testRawMessage, testRawTokenizedElements);

        String expectedUntokenizedMessage = "Hello <@lululombard#1337>, can you check `/contact.html` on https://redcraft.org please? 😎 You can also use /join if you want to 🤷‍♀️";
        String actualUntokenizedMessage = TokenizerManager.untokenizeElements(testTokenizedMessage);
        assertEquals(expectedUntokenizedMessage, actualUntokenizedMessage);
    }
}
