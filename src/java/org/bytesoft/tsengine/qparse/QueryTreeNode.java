package org.bytesoft.tsengine.qparse;


public class QueryTreeNode {
    public ExprToken value;

    public QueryTreeNode left = null;
    public QueryTreeNode right = null;

    QueryTreeNode(ExprToken val) {
        value = val;
    }

    public final boolean IsBinary() {
        return left != null && right != null;
    }

    public final boolean IsLeaf() {
        return left == null && right == null;
    }

    public final boolean IsUnary() {
        return !IsBinary() && !IsLeaf();
    }

    static private void dumpSubtree(QueryTreeNode node, StringBuilder buf, int level) {
        boolean print_parenthesis = node.IsBinary() && level > 0;
        ExprToken value = node.value;

        if (print_parenthesis)
            buf.append('(');

        if (node.left != null)
            dumpSubtree(node.left, buf, level+1);

        if (value.IsOperator() && value.GetOperator() != '!')
            buf.append(" " + value.toString() + " ");
        else
            buf.append(value.toString());

        if (node.right != null)
            dumpSubtree(node.right, buf, level+1);

        if (print_parenthesis)
            buf.append(')');
    }

    /**
     * Print query tree in pretty, human-readable form:
     *   - do not print parenthesis each time
     *   - print spaces when needed
     *
     * @param root root of tree (or subtree)
     * @return string representation of given tree
     */
    static public String dumpQueryTree(QueryTreeNode root) {
        StringBuilder buf = new StringBuilder();
        dumpSubtree(root, buf, 0);

        return buf.toString();
    }

    @Override
    public String toString() {
        return dumpQueryTree(this);
    }
}
