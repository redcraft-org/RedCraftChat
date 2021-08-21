import junit.framework.*;

import java.util.HashMap;

import org.redcraft.redcraftchat.models.translate.TokenizedMessage;
import org.redcraft.redcraftchat.tokenizer.TokenizerManager;

public class MessageTokenizerTest extends TestCase {

    public void testTokenizer() {
        String testMessage = "Hello <@lululombard#1337>, can you check `/contact.html` on https://redcraft.org please? You can also use /join if you want to";

        String expectedRawTokenizedMessage = "Hello 280aa0a, can you check dc20cb6 on 1d36664 please? You can also use 3eccd38 if you want to";
        HashMap<String, String> expectedRawTokenizedElements = new HashMap<String, String>();
        expectedRawTokenizedElements.put("1d36664", "https://redcraft.org");
        expectedRawTokenizedElements.put("280aa0a", "<@lululombard#1337>");
        expectedRawTokenizedElements.put("dc20cb6", "`/contact.html`");
        expectedRawTokenizedElements.put("3eccd38", "/join");

        TokenizedMessage expectedTokenizedMessage = new TokenizedMessage(expectedRawTokenizedMessage, expectedRawTokenizedElements);
        TokenizedMessage actualTokenizedMessage = TokenizerManager.tokenizeElements(testMessage, false);
        assertEquals(expectedTokenizedMessage.toString(), actualTokenizedMessage.toString());
    }

    public void testUntokenizer() {
        String testRawMessage = "Hello 280aa0a, can you check dc20cb6 on 1d36664 please? You can also use 3eccd38 if you want to";

        HashMap<String, String> testRawTokenizedElements = new HashMap<String, String>();
        testRawTokenizedElements.put("1d36664", "https://redcraft.org");
        testRawTokenizedElements.put("280aa0a", "<@lululombard#1337>");
        testRawTokenizedElements.put("dc20cb6", "`/contact.html`");
        testRawTokenizedElements.put("3eccd38", "/join");
        TokenizedMessage testTokenizedMessage = new TokenizedMessage(testRawMessage, testRawTokenizedElements);

        String expectedUntokenizedMessage = "Hello <@lululombard#1337>, can you check `/contact.html` on https://redcraft.org please? You can also use /join if you want to";
        String actualUntokenizedMessage = TokenizerManager.untokenizeElements(testTokenizedMessage);
        assertEquals(expectedUntokenizedMessage, actualUntokenizedMessage);
    }
}
