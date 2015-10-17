package org.bytesoft.tsengine.qparse;


public class QueryTreeNode {
    public ExprToken value;

    public QueryTreeNode left = null;
    public QueryTreeNode right = null;

    QueryTreeNode(ExprToken val) {
        value = val;
    }

    static private void dumpSubtree(QueryTreeNode node, StringBuilder buf, int level) {
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
