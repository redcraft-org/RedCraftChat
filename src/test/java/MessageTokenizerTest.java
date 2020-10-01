import junit.framework.*;

import java.util.HashMap;

import org.redcraft.redcraftbungeechat.models.translate.TokenizedMessage;
import org.redcraft.redcraftbungeechat.tokenizer.TokenizerManager;

public class MessageTokenizerTest extends TestCase {

    public void testTokenizer() {
        String testMessage = "Hello <@lululombard#1337>, can you check `/contact.html` on https://redcraft.org please?";

        String expectedRawTokenizedMessage = "Hello 280aa0a, can you check dc20cb6 on 1d36664 please?";
        HashMap<String, String> expectedRawTokenizedElements = new HashMap<String, String>();
        expectedRawTokenizedElements.put("1d36664", "https://redcraft.org");
        expectedRawTokenizedElements.put("280aa0a", "<@lululombard#1337>");
        expectedRawTokenizedElements.put("dc20cb6", "`/contact.html`");

        TokenizedMessage expectedTokenizedMessage = new TokenizedMessage(expectedRawTokenizedMessage, expectedRawTokenizedElements);
        TokenizedMessage actualTokenizedMessage = TokenizerManager.tokenizeElements(testMessage, false);
        assertEquals(expectedTokenizedMessage.toString(), actualTokenizedMessage.toString());
    }

    public void testUntokenizer() {
        String testRawMessage = "Hello 280aa0a, can you check dc20cb6 on 1d36664 please?";

        HashMap<String, String> testRawTokenizedElements = new HashMap<String, String>();
        testRawTokenizedElements.put("1d36664", "https://redcraft.org");
        testRawTokenizedElements.put("280aa0a", "<@lululombard#1337>");
        testRawTokenizedElements.put("dc20cb6", "`/contact.html`");
        TokenizedMessage testTokenizedMessage = new TokenizedMessage(testRawMessage, testRawTokenizedElements);

        String expectedUntokenizedMessage = "Hello <@lululombard#1337>, can you check `/contact.html` on https://redcraft.org please?";
        String actualUntokenizedMessage = TokenizerManager.untokenizeElements(testTokenizedMessage);
        assertEquals(expectedUntokenizedMessage, actualUntokenizedMessage);
    }
}
