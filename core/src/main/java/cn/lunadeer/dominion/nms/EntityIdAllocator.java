package cn.lunadeer.dominion.nms;

import cn.lunadeer.dominion.utils.XLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class EntityIdAllocator {

    private static final AtomicInteger FALLBACK_COUNTER = new AtomicInteger(2_000_000_000);
    private static final Map<Class<?>, AtomicInteger> ENTITY_COUNTERS = new ConcurrentHashMap<>();

    private EntityIdAllocator() {
    }

    public static int nextEntityId(Class<?> entityClass) {
        return ENTITY_COUNTERS.computeIfAbsent(entityClass, EntityIdAllocator::resolveCounter).incrementAndGet();
    }

    private static AtomicInteger resolveCounter(Class<?> entityClass) {
        AtomicInteger counter = findStaticAtomicInteger(entityClass);
        if (counter != null) {
            return counter;
        }
        XLogger.warn("Failed to locate NMS entity counter in {0}; using Dominion fake entity id range.", entityClass.getName());
        return FALLBACK_COUNTER;
    }

    private static AtomicInteger findStaticAtomicInteger(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !AtomicInteger.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof AtomicInteger counter) {
                    return counter;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }
}