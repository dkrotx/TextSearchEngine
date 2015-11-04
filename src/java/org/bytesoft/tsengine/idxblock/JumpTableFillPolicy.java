package org.bytesoft.tsengine.idxblock;

public interface JumpTableFillPolicy {
    boolean enough(int continuous_elems, int continuous_size);

    class ByNumberOfEntries implements JumpTableFillPolicy {
        int max_entries;

        public ByNumberOfEntries(int max_entries) {
            this.max_entries = max_entries;
        }

        @Override
        public boolean enough(int continuous_elems, int continuous_size) {
            return continuous_elems >= max_entries;
        }
    }

    class Empty implements JumpTableFillPolicy {
        @Override
        public boolean enough(int continuous_elems, int continuous_size) {
            return false;
        }
    }
}