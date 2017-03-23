package org.bytesoft.tsengine;

import org.bytesoft.tsengine.idxblock.IdxBlocksIterator;
import org.bytesoft.tsengine.qparse.QueryTreeNode;

/**
 * Class for execution query tree on index block.
 * Build QTreePerformer from original QueryTree (connect index blocks),
 * and then get documents, one by one.
 */
public class QTreePerformer {
    public class UnsupportedQTreeOperator extends Exception {
        UnsupportedQTreeOperator(String msg) { super(msg); }
    }

    public interface Word2BlockConverter {
        /**
         * @return index block iterator for word or -1
         */
        IdxBlocksIterator GetIndexBlockIterator(String word);
    }

    private Word2BlockConverter word2block;

    private interface NodeExecutor {
        /**
         * Evaluate this node:
         * - perform given operation for operators or
         * - just return current value for index block
         *
         * @return docid that matches subtree
         */
        int EvaluateNode();

        /**
         * Move to document id >= given ID
         * @param id upper bound
         * @return
         */
        void GotoDocID(int id);
    }

    /**
     * special case of non-existing terminal
     */
    static private class NullNode implements NodeExecutor {
        @Override
        public void GotoDocID(int id) {

        }

        @Override
        public int EvaluateNode() {
            return IdxBlocksIterator.OMEGA_ID;
        }
    }

    /**
     * Terminal node - deals with index block iterator
     */
    static private class TerminalNode implements NodeExecutor {
        IdxBlocksIterator it;

        public TerminalNode(IdxBlocksIterator it) {
            this.it = it;
        }

        @Override
        public int EvaluateNode() {
            return it.GetCurrentDocID();
        }

        @Override
        public void GotoDocID(int id) {
            it.GotoDocID(id);
        }
    }

    /**
     * Conjunction node: left & right
     * Extract numbers which are in left and in right nodes simultaneously
     */
    static private class AndNode implements NodeExecutor {
        private NodeExecutor left;
        private NodeExecutor right;


        public AndNode(NodeExecutor left, NodeExecutor right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public int EvaluateNode() {
            int lval = left.EvaluateNode();
            int rval = right.EvaluateNode();

            while (lval != rval && (lval != IdxBlocksIterator.OMEGA_ID && rval != IdxBlocksIterator.OMEGA_ID)) {
                if (lval < rval) {
                    left.GotoDocID(rval);
                    lval = left.EvaluateNode();
                }
                else {
                    right.GotoDocID(lval);
                    rval = right.EvaluateNode();
                }
            }

            if (lval == IdxBlocksIterator.OMEGA_ID || rval == IdxBlocksIterator.OMEGA_ID)
                return IdxBlocksIterator.OMEGA_ID;

            assert(lval == rval);

            return lval;
        }

        @Override
        public void GotoDocID(int id) {
            left.GotoDocID(id);
            right.GotoDocID(id);
        }
    }

    /**
     * Disjunction node: left | right
     * Join numbers from left and right node
     */
    static private class OrNode implements NodeExecutor {
        private NodeExecutor left;
        private NodeExecutor right;


        public OrNode(NodeExecutor left, NodeExecutor right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public int EvaluateNode() {
            int lval = left.EvaluateNode();
            int rval = right.EvaluateNode();

            if (lval == IdxBlocksIterator.OMEGA_ID) return rval;
            if (rval == IdxBlocksIterator.OMEGA_ID) return lval;

            return Math.min(lval, rval);
        }

        @Override
        public void GotoDocID(int id) {
            left.GotoDocID(id);
            right.GotoDocID(id);
        }
    }

    /**
     * Negation node - provide numbers which are absent in `operand` node
     * For example:
     *   - [0, 1, 3] => [2, 4, ...]
     *   - [3] => [0, 1, 2, 4, ...]
     *   - []  => [0, 1, ...]
     */
    static private class NegationNode implements NodeExecutor {
        private NodeExecutor operand;
        private int virtual_doc_id = IdxBlocksIterator.ALPHA_ID;

        public NegationNode(NodeExecutor operand) {
            this.operand = operand;
        }

        @Override
        public int EvaluateNode() {
            while (virtual_doc_id == operand.EvaluateNode()) {
                operand.GotoDocID(++virtual_doc_id);
            }

            return virtual_doc_id;
        }

        @Override
        public void GotoDocID(int id) {
            operand.GotoDocID(id);
            virtual_doc_id = id;
        }
    }

    private NodeExecutor root;
    private int last_doc_id = -1;

    private NodeExecutor deepCopy(QueryTreeNode node) throws UnsupportedQTreeOperator {
        if (node.IsLeaf()) {
            IdxBlocksIterator it = word2block.GetIndexBlockIterator(node.value.GetWord());
            return (it==null ? new NullNode() : new TerminalNode(it));
        }

        assert(node.value.IsOperator());

        switch(node.value.GetOperator()) {
            case '&':
                return new AndNode(deepCopy(node.left), deepCopy(node.right));
            case '|':
                return new OrNode(deepCopy(node.left), deepCopy(node.right));
            case '!':
                return new NegationNode(deepCopy(node.GetUnaryOperand()));
        }

        throw new UnsupportedQTreeOperator("Unsupported opeator: " + node.value);
    }

    public QTreePerformer(QueryTreeNode qtree, Word2BlockConverter word2block) throws UnsupportedQTreeOperator {
        this.word2block = word2block;
        root = deepCopy(qtree);
    }

    public boolean Finished() {
        return last_doc_id == IdxBlocksIterator.OMEGA_ID;
    }

    public int GetNextDocument() {
        if (Finished())
            return IdxBlocksIterator.OMEGA_ID;

        root.GotoDocID(last_doc_id + 1);
        last_doc_id = root.EvaluateNode();
        return last_doc_id;
    }

}
