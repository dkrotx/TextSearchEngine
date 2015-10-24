package org.bytesoft.tsengine.encoders;

import java.util.ArrayList;

public abstract  class FibonacciSequence {
    static int[] FIB_SEQUENCE;

    static {
        ArrayList<Integer> sequence = new ArrayList<>();

        sequence.add(1);
        sequence.add(1);

        for(;;)
        {
            long n = (long)sequence.get(sequence.size() - 2) + sequence.get(sequence.size() - 1);
            if (n > (long)Integer.MAX_VALUE)
                break;

            sequence.add((int)n);
        }

        FIB_SEQUENCE = new int[sequence.size() - 1];
        for (int i = 1; i < sequence.size(); i++)
            FIB_SEQUENCE[i - 1] = sequence.get(i);
    }
}
