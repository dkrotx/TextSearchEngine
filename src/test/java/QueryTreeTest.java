import org.bytesoft.tsengine.qparse.ExprToken;
import org.bytesoft.tsengine.qparse.ExpressionTokenizer;
import org.bytesoft.tsengine.qparse.QueryTreeBuilder;
import org.bytesoft.tsengine.qparse.QueryTreeNode;
import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class QueryTreeTest {

    private String parseQuery(String query) {
        QueryTreeNode tree;

        try {
            ExprToken[] tokens = ExpressionTokenizer.Tokenize(query);
            tree = QueryTreeBuilder.BuildExpressionTree(tokens);
        }
        catch(ParseException e) {
            return "Exception:" + e.getMessage();
        }

        return tree.toString();
    }

    @Test
    public void TestTokenizer() throws ParseException {
        {
            ExprToken[] expected = {
                    new ExprToken(ExprToken.Type.Word, "w1")
            };

            assertArrayEquals(expected, ExpressionTokenizer.Tokenize("w1"));
            assertArrayEquals("Single token with space", expected, ExpressionTokenizer.Tokenize("w1 "));
            assertArrayEquals("Single token with spaces", expected, ExpressionTokenizer.Tokenize("w1     "));
        }

        {
            ExprToken[] expected = {
                    new ExprToken(ExprToken.Type.Word, "w1"),
                    new ExprToken(ExprToken.Type.Operator, '&'),
                    new ExprToken(ExprToken.Type.Word, "w2")
            };

            assertArrayEquals(expected, ExpressionTokenizer.Tokenize("w1 & w2"));
            assertArrayEquals("Without spaces", expected, ExpressionTokenizer.Tokenize("w1&w2"));
        }

        {
            ExprToken[] expected = {
                    new ExprToken(ExprToken.Type.Bracket, '('),
                    new ExprToken(ExprToken.Type.Word, "w1"),
                    new ExprToken(ExprToken.Type.Operator, '&'),
                    new ExprToken(ExprToken.Type.Word, "w2"),
                    new ExprToken(ExprToken.Type.Bracket, ')'),
                    new ExprToken(ExprToken.Type.Operator, '|'),
                    new ExprToken(ExprToken.Type.Operator, '!'),
                    new ExprToken(ExprToken.Type.Word, "w3")
            };

            assertArrayEquals(expected, ExpressionTokenizer.Tokenize("(w1 & w2) | !w3"));
            assertArrayEquals("Without spaces", expected, ExpressionTokenizer.Tokenize("(w1&w2)|!w3"));
            assertArrayEquals("With strange spaces", expected, ExpressionTokenizer.Tokenize("( w1&    w2)|!     w3"));
        }
    }

    @Test
    public void testSingleWord() {
        assertEquals("word", parseQuery("word"));
        assertEquals("word123", parseQuery("word123"));
        assertEquals("слово", parseQuery("слово"));
        assertEquals("слово123", parseQuery("слово123"));
    }

    @Test
    public void testComplexQuery() {
        assertEquals("(w1 & w2) | w3", parseQuery("w1 & w2 | w3"));
        assertEquals("((w1 | w2) | w3) | w4", parseQuery("w1 | w2 | w3 | w4"));
        assertEquals("(w1 & w2) | (w3 & w4)", parseQuery("w1 & w2 | w3 & w4"));
        assertEquals("(w1 & w2) | !w3", parseQuery("w1 & w2 | !w3"));
        assertEquals("(w1 & w2) | (!w3 & !w4)", parseQuery("w1 & w2 | !w3 & !w4"));
    }

    @Test
    public void testParenthesis() {
        assertEquals("w1 & (w2 | w3)", parseQuery("w1 & (w2 | w3)"));
        assertEquals("w1 & (w2 | !w3)", parseQuery("w1 & (w2 | !w3)"));
        assertEquals("w1 & !(w2 & w3)", parseQuery("w1 & !(w2 & w3)"));
        assertEquals("w1 & !(w2 | w3)", parseQuery("w1 & !(w2 | w3)"));
    }

    @Test
    public void testHardCases() {
        assertEquals("w1 & !(w2 | (w3 & ((w4 | w5) | (!w6 & w7))))",
                parseQuery("w1 & !(w2 | w3 & (w4 | w5 | !(w6) & w7))"));
    }
}
