package timecode.runtime;

import java.util.HashMap;
import java.util.Map;

public final class TimeCodecRegistry {
    private final Map<Short, TimeCodec<?>> byType = new HashMap<>();
    private final Map<Class<?>, TimeCodec<?>> byClass = new HashMap<>();

    public void register(TimeCodec<?> c) {
        byType.put(c.typeId(), c);
        byClass.put(c.type(), c);
    }

    @SuppressWarnings("unchecked")
    public <T> TimeCodec<T> get(short typeId) { return (TimeCodec<T>) byType.get(typeId); }

    @SuppressWarnings("unchecked")
    public <T> TimeCodec<T> forClass(Class<T> clz) { return (TimeCodec<T>) byClass.get(clz); }
}

