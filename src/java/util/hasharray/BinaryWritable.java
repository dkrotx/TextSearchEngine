package util.hasharray;

import java.io.DataOutputStream;
import java.io.IOException;

public interface BinaryWritable {
    int  SizeBytes();
    void write(DataOutputStream out) throws IOException;
}
