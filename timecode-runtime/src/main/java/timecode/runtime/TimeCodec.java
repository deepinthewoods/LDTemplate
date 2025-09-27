package timecode.runtime;

import java.nio.ByteBuffer;

public interface TimeCodec<T> {
    short typeId();
    Class<T> type();
    void write(T obj, ByteBuffer out);
    void read(T obj, ByteBuffer in);
}

