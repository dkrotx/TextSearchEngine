package org.bytesoft.tsengine.qparse;


public class QueryTreeNode {
    public ExprToken value;

    public QueryTreeNode left = null;
    public QueryTreeNode right = null;

    QueryTreeNode(ExprToken val) {
        value = val;
    }
}
