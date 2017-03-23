package org.bytesoft.tsengine.qparse;

/**
 * Build query tree from given tokenizer
 */
public class QueryTreeBuilder {

    private static int findMinPriorityOperator(ExprToken[] tokens, int start, int end) {
        int element = -1;
        int minprio = -1, minfolding = -1;
        int folding = 0;

        for(int i = start; i <= end; i++) {
            ExprToken tk = tokens[i];

            if (tk.IsBracket()) {
                if (tk.GetBracket() == '(')  folding++;
                else folding--;
            }

            if (tk.IsOperator()) {
                int priority = ExpressionTokenizer.GetOpPrio(tk.GetOperator());

                if ( element == -1 || folding < minfolding || (folding == minfolding && priority <= minprio) ) {
                    minfolding = folding;
                    minprio = priority;
                    element = i;
                }
            }
        }

        return element;
    }

    private static QueryTreeNode nodeFromFirstWord(ExprToken[] tokens, int start, int end) {
        for(int i = start; i <= end; i++) {
            if (tokens[i].IsWord())
                return new QueryTreeNode(tokens[i]);
        }

        return null;
    }

    /**
     * Build subtree (recursively)
     * @param tokens tokens of query
     * @param start  first token
     * @param end    last token (exclude)
     * @return root of subtree
     */
    private static QueryTreeNode buildSubtree(ExprToken[] tokens, int start, int end) {
        int minprio_op = findMinPriorityOperator(tokens, start, end);

        if (minprio_op == -1)
            return nodeFromFirstWord(tokens, start, end);

        QueryTreeNode node = new QueryTreeNode(tokens[minprio_op]);
        node.left  = buildSubtree(tokens, start, minprio_op - 1);
        node.right = buildSubtree(tokens, minprio_op + 1, end);

        return node;
    }

    /**
     * Build expression tree from tokens
     * @return root of expression tree
     */
    public static QueryTreeNode BuildExpressionTree(ExprToken[] tokens) {
        return buildSubtree(tokens, 0, tokens.length - 1);
    }
}
