package org.bytesoft.tsengine.qparse;

import java.text.ParseException;
import java.util.ArrayList;

public class ExpressionTokenizer {

    /**
     * Tokenize given query string
     *
     * @param expr arbitrary query in generic form "word1 & (word2 | word3) & !word4"
     * @return list of tokens
     */
    public static ExprToken[] Tokenize(String expr) throws ParseException {
        ArrayList<ExprToken> res = new ArrayList<>();
        int i = 0;

        while (i < expr.length()) {
            char c = expr.charAt(i);

            if (Character.isSpaceChar(c)) {
                i++;
                continue;
            }

            if (GetOpPrio(c) != -1) {
                res.add(new ExprToken(ExprToken.Type.Operator, new Character(c)));
                i++;
            } else if (Character.isLetterOrDigit(c)) {
                int start = i++;

                while (i < expr.length() && Character.isLetterOrDigit(expr.charAt(i)))
                    i++;

                res.add(new ExprToken(ExprToken.Type.Word, expr.substring(start, i)));
            } else if (c == '(') {
                res.add(new ExprToken(ExprToken.Type.Bracket, '('));
                i++;
            } else if (c == ')') {
                res.add(new ExprToken(ExprToken.Type.Bracket, ')'));
                i++;
            } else {
                throw new ParseException("Unexpected character (" + c + ")", i);
            }
        }

        return res.toArray(new ExprToken[res.size()]);
    }

    public static int GetOpPrio(char op) {
        switch (op) {
            case '|':
                return 1;
            case '&':
                return 2;
            case '!':
                return 3;
        }

        return -1;
    }

}