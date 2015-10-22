package org.bytesoft.tsengine.text;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextTokenizer {
    static Pattern word_expr = Pattern.compile("\\p{L}+");
    Matcher matcher;
    String input_text;

    public void TokenizeText(String text) {
        // TODO: now TokenizeText() will break previous match (should be continued)
        input_text = text;
        matcher = word_expr.matcher(input_text);
    }

    public boolean hasNextToken() {
        return (matcher != null && matcher.find());
    }

    public String getNextToken() {
        return matcher == null ? null : matcher.group();
    }
}
