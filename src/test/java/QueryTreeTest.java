import org.junit.*;
import static org.junit.Assert.*;
import org.bytesoft.tsengine.qparse.*;

import java.text.ParseException;


public class QueryTreeTest {

    private void dumpSubtree(QueryTreeNode node, StringBuilder buf, int level) {
        boolean print_parenthesis = (level > 0 && (node.left != null && node.right != null));
        ExprToken value = node.value;

        if (print_parenthesis)
            buf.append('(');

        if (node.left != null)
            dumpSubtree(node.left, buf, level+1);

        if (value.IsOperator() && value.GetOperator() != '!')
            buf.append(" " + node.value.toString() + " ");
        else
            buf.append(node.value.toString());

        if (node.right != null)
            dumpSubtree(node.right, buf, level+1);

        if (print_parenthesis)
            buf.append(')');
    }

    private String dumpQueryTree(QueryTreeNode root) {
        StringBuilder buf = new StringBuilder();
        dumpSubtree(root, buf, 0);

        return buf.toString();
    }

    private String parseQuery(String query) {
        QueryTreeNode tree;

        try {
            ExprToken[] tokens = ExpressionTokenizer.Tokenize(query);
            tree = QueryTreeBuilder.BuildExpressionTree(tokens);
        }
        catch(ParseException e) {
            return "Exception:" + e.getMessage();
        }

        return dumpQueryTree(tree);
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
