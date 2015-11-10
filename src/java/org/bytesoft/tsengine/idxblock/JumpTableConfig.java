package org.bytesoft.tsengine.idxblock;


import org.bytesoft.tsengine.IndexingConfig;

public class JumpTableConfig {
    public int rindex_step;
    public int jump_table_step;

    public JumpTableConfig(int rindex_step, int jump_table_step) {
        this.rindex_step = rindex_step;
        this.jump_table_step = jump_table_step;
    }

    public static JumpTableConfig fromIndexingConfig(IndexingConfig cfg) {
        return new JumpTableConfig(cfg.GetJumpTableDirectStep(), cfg.GetJumpTableIndirectStep());
    }

    public static JumpTableConfig makeEmptyJumpTable() {
        return emptyJumpTable;
    }

    static private JumpTableConfig emptyJumpTable = new JumpTableConfig(Integer.MAX_VALUE, Integer.MAX_VALUE);

    public boolean equals(JumpTableConfig another) {
        return (rindex_step == another.rindex_step && jump_table_step == another.jump_table_step);
    }
}
