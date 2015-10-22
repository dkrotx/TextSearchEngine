import org.bytesoft.tsengine.text.TextTokenizer;
import org.junit.*;
import static org.junit.Assert.*;

public class TextTokenizerTest {
    private void compareTokens(String text, String[] expected_tokens) {
        TextTokenizer tok = new TextTokenizer();

        tok.TokenizeText(text);

        for(String t: expected_tokens) {
            assertTrue(tok.hasNextToken());
            assertEquals(t, tok.getNextToken());
        }

        assertFalse(tok.hasNextToken());
    }

    private void compareTokensV(String text, String ... expected_tokens) {
        compareTokens(text, expected_tokens);
    }

    @Test
    public void TestInitialState() {
        TextTokenizer tok = new TextTokenizer();

        assertFalse(tok.hasNextToken());
        assertNull(tok.getNextToken());
    }

    @Test
    public void CheckDifferentTexts() {
        compareTokensV("turn off TV",
                "turn", "off", "TV");
        compareTokensV("turn         off\t\tTV",
                "turn", "off", "TV");
        compareTokensV("может, как в Sony, сделают вход сбоку?",
                "может", "как", "в", "Sony", "сделают", "вход", "сбоку");

        compareTokensV("Perhaps (или нет?): какие-то ёжики уснут(зимой)",
                "Perhaps", "или", "нет", "какие", "то", "ёжики", "уснут", "зимой");
    }
}
