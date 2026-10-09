package cn.lunadeer.dominion.flags;

import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A live API view of active flag values, including read-only legacy aliases.
 * The backing map must contain active flags only. Alias values are never stored:
 * an alias exists exactly while its replacement exists in the backing map.
 * Active keys can be modified through this map and active entry values can be
 * changed with {@link Map.Entry#setValue(Object)}. Collection views do not support
 * structural modifications: removing one active entry can also remove aliases,
 * which cannot satisfy the collection contract of removing a single element.
 */
final class LegacyFlagMap<F extends Flag> extends AbstractMap<F, Boolean> {
    private final Map<F, Boolean> backing;

    private LegacyFlagMap(Map<F, Boolean> backing) {
        this.backing = Objects.requireNonNull(backing, "backing");
        for (F flag : backing.keySet()) {
            if (flag != null && Flags.isLegacyFlag(flag)) {
                throw new IllegalArgumentException("Backing map contains legacy flag: " + flag.getFlagName());
            }
        }
    }

    static <F extends Flag> Map<F, Boolean> view(Map<F, Boolean> backing) {
        return backing instanceof LegacyFlagMap<?> ? backing : new LegacyFlagMap<>(backing);
    }

    private static Object readKey(Object key) {
        return key instanceof Flag flag ? Flags.resolveReadFlag(flag) : key;
    }

    private static void requireWritable(Object key) {
        if (key instanceof Flag flag && Flags.isLegacyFlag(flag)) {
            throw new UnsupportedOperationException("Legacy flag is read-only: " + flag.getFlagName());
        }
    }

    @Override
    public Boolean get(Object key) {
        return backing.get(readKey(key));
    }

    @Override
    public Boolean getOrDefault(Object key, Boolean defaultValue) {
        return backing.getOrDefault(readKey(key), defaultValue);
    }

    @Override
    public boolean containsKey(Object key) {
        return backing.containsKey(readKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return backing.containsValue(value);
    }

    @Override
    public int size() {
        int size = backing.size();
        for (Flag replacement : Flags.getLegacyAliases().values()) {
            if (backing.containsKey(replacement)) {
                size++;
            }
        }
        return size;
    }

    @Override
    public Boolean put(F key, Boolean value) {
        requireWritable(key);
        return backing.put(key, value);
    }

    @Override
    public void putAll(Map<? extends F, ? extends Boolean> values) {
        values.keySet().forEach(LegacyFlagMap::requireWritable);
        backing.putAll(values);
    }

    @Override
    public Boolean remove(Object key) {
        requireWritable(key);
        return backing.remove(key);
    }

    @Override
    public boolean remove(Object key, Object value) {
        requireWritable(key);
        return backing.remove(key, value);
    }

    @Override
    public void clear() {
        backing.clear();
    }

    @Override
    public Boolean putIfAbsent(F key, Boolean value) {
        requireWritable(key);
        return backing.putIfAbsent(key, value);
    }

    @Override
    public Boolean replace(F key, Boolean value) {
        requireWritable(key);
        return backing.replace(key, value);
    }

    @Override
    public boolean replace(F key, Boolean oldValue, Boolean newValue) {
        requireWritable(key);
        return backing.replace(key, oldValue, newValue);
    }

    @Override
    public void replaceAll(BiFunction<? super F, ? super Boolean, ? extends Boolean> function) {
        Objects.requireNonNull(function, "function");
        // A callback may produce conflicting values for a flag and its alias.
        // Reject before modifying any entries, just as writing an alias does.
        if (size() != backing.size()) {
            throw new UnsupportedOperationException("Legacy flag aliases are read-only");
        }
        backing.replaceAll(function);
    }

    @Override
    public Boolean computeIfAbsent(F key, Function<? super F, ? extends Boolean> function) {
        requireWritable(key);
        return backing.computeIfAbsent(key, function);
    }

    @Override
    public Boolean computeIfPresent(F key,
                                    BiFunction<? super F, ? super Boolean, ? extends Boolean> function) {
        requireWritable(key);
        return backing.computeIfPresent(key, function);
    }

    @Override
    public Boolean compute(F key, BiFunction<? super F, ? super Boolean, ? extends Boolean> function) {
        requireWritable(key);
        return backing.compute(key, function);
    }

    @Override
    public Boolean merge(F key, Boolean value,
                         BiFunction<? super Boolean, ? super Boolean, ? extends Boolean> function) {
        requireWritable(key);
        return backing.merge(key, value, function);
    }

    @Override
    public Set<F> keySet() {
        return Collections.unmodifiableSet(super.keySet());
    }

    @Override
    public Collection<Boolean> values() {
        return Collections.unmodifiableCollection(super.values());
    }

    @Override
    public Set<Entry<F, Boolean>> entrySet() {
        return Collections.unmodifiableSet(new AbstractSet<>() {
            @Override
            public Iterator<Entry<F, Boolean>> iterator() {
                return entryIterator();
            }

            @Override
            public int size() {
                return LegacyFlagMap.this.size();
            }

            @Override
            public boolean contains(Object value) {
                return value instanceof Entry<?, ?> entry && containsKey(entry.getKey())
                        && Objects.equals(get(entry.getKey()), entry.getValue());
            }

        });
    }

    @SuppressWarnings("unchecked")
    private Iterator<Entry<F, Boolean>> entryIterator() {
        // Snapshot keys so a caller can update active map entries while reading;
        // entry values remain live and keys removed since creation are skipped.
        List<F> keys = new ArrayList<>(backing.keySet());
        Flags.getLegacyAliases().forEach((alias, replacement) -> {
            if (backing.containsKey(replacement)) {
                // Alias and replacement always have the same flag type.
                keys.add((F) alias);
            }
        });
        return new Iterator<>() {
            private int nextIndex;

            @Override
            public boolean hasNext() {
                // Removing an active flag also removes its aliases from this view.
                while (nextIndex < keys.size() && !containsKey(keys.get(nextIndex))) {
                    nextIndex++;
                }
                return nextIndex < keys.size();
            }

            @Override
            public Entry<F, Boolean> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return new LiveEntry(keys.get(nextIndex++));
            }
        };
    }

    private final class LiveEntry implements Entry<F, Boolean> {
        private final F key;

        private LiveEntry(F key) {
            this.key = key;
        }

        @Override
        public F getKey() {
            return key;
        }

        @Override
        public Boolean getValue() {
            return get(key);
        }

        @Override
        public Boolean setValue(Boolean value) {
            requireWritable(key);
            if (!backing.containsKey(key)) {
                throw new IllegalStateException("Flag entry is no longer present");
            }
            return backing.put(key, value);
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof Entry<?, ?> entry && Objects.equals(key, entry.getKey())
                    && Objects.equals(getValue(), entry.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(getValue());
        }

        @Override
        public String toString() {
            return key + "=" + getValue();
        }
    }
}
