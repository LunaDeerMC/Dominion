package cn.lunadeer.dominion.flags;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class LegacyFlagMapTest {

    @Test
    void exposesLiveAliasesThroughEveryReadView() {
        Map<PriFlag, Boolean> backing = new LinkedHashMap<>();
        backing.put(Flags.CHEST, true);
        Map<PriFlag, Boolean> view = LegacyFlagMap.view(backing);

        assertEquals(true, view.get(Flags.CONTAINER));
        assertEquals(true, view.getOrDefault(Flags.CONTAINER, false));
        assertTrue(view.containsKey(Flags.CONTAINER));
        assertEquals(Set.of(Flags.CHEST, Flags.CONTAINER), view.keySet());
        assertEquals(2, view.size());
        assertEquals(List.of(true, true), new ArrayList<>(view.values()));
        assertEquals(Map.of(Flags.CHEST, true, Flags.CONTAINER, true), new LinkedHashMap<>(view));

        Map.Entry<PriFlag, Boolean> retainedAlias = view.entrySet().stream()
                .filter(entry -> entry.getKey() == Flags.CONTAINER).findFirst().orElseThrow();
        backing.put(Flags.CHEST, false);

        assertEquals(false, view.get(Flags.CONTAINER));
        assertEquals(false, retainedAlias.getValue());
        assertTrue(view.entrySet().contains(Map.entry(Flags.CONTAINER, false)));
        assertFalse(view.entrySet().contains(Map.entry(Flags.CONTAINER, true)));
        assertEquals(List.of(false, false), new ArrayList<>(view.values()));
        assertEquals(Map.of(Flags.CHEST, false), backing);
    }

    @Test
    void aliasPresenceFollowsItsTargetIncludingNullValues() {
        Map<PriFlag, Boolean> backing = new LinkedHashMap<>();
        Map<PriFlag, Boolean> view = LegacyFlagMap.view(backing);
        Set<PriFlag> retainedKeys = view.keySet();

        assertNull(view.get(Flags.CONTAINER));
        assertEquals(true, view.getOrDefault(Flags.CONTAINER, true));
        assertFalse(view.containsKey(Flags.CONTAINER));
        assertTrue(view.entrySet().isEmpty());

        backing.put(Flags.CHEST, null);
        assertTrue(retainedKeys.contains(Flags.CONTAINER));
        assertNull(view.getOrDefault(Flags.CONTAINER, true));
        assertTrue(view.entrySet().contains(new AbstractMap.SimpleEntry<>(Flags.CONTAINER, null)));
        assertEquals(2, view.values().size());

        backing.remove(Flags.CHEST);
        assertTrue(retainedKeys.isEmpty());
        assertTrue(view.isEmpty());
    }

    @Test
    void allKeyedMutationsRejectAliasesEvenWhenTheyWouldBeNoOps() {
        Map<PriFlag, Boolean> backing = new LinkedHashMap<>();
        backing.put(Flags.CHEST, true);
        Map<PriFlag, Boolean> view = LegacyFlagMap.view(backing);
        AtomicBoolean callbackCalled = new AtomicBoolean();
        List<Executable> mutations = List.of(
                () -> view.put(Flags.CONTAINER, null),
                () -> view.putIfAbsent(Flags.CONTAINER, false),
                () -> view.remove(Flags.CONTAINER),
                () -> view.remove(Flags.CONTAINER, false),
                () -> view.replace(Flags.CONTAINER, false),
                () -> view.replace(Flags.CONTAINER, false, null),
                () -> view.computeIfAbsent(Flags.CONTAINER, flag -> {
                    callbackCalled.set(true);
                    return false;
                }),
                () -> view.computeIfPresent(Flags.CONTAINER, (flag, value) -> {
                    callbackCalled.set(true);
                    return null;
                }),
                () -> view.compute(Flags.CONTAINER, (flag, value) -> {
                    callbackCalled.set(true);
                    return false;
                }),
                () -> view.merge(Flags.CONTAINER, false, (oldValue, newValue) -> {
                    callbackCalled.set(true);
                    return false;
                }),
                () -> view.keySet().remove(Flags.CONTAINER),
                () -> view.entrySet().remove(Map.entry(Flags.CONTAINER, false))
        );

        for (Executable mutation : mutations) {
            assertThrows(UnsupportedOperationException.class, mutation);
        }
        assertFalse(callbackCalled.get());
        assertEquals(Map.of(Flags.CHEST, true), backing);

        backing.clear();
        for (Executable mutation : mutations) {
            assertThrows(UnsupportedOperationException.class, mutation);
        }
        assertFalse(callbackCalled.get());
        assertTrue(backing.isEmpty());
    }

    @Test
    void bulkAndEntryWritesCannotStoreIndependentAliasValues() {
        Map<PriFlag, Boolean> backing = new LinkedHashMap<>();
        backing.put(Flags.CHEST, true);
        Map<PriFlag, Boolean> view = LegacyFlagMap.view(backing);
        Map<PriFlag, Boolean> updates = new LinkedHashMap<>();
        updates.put(Flags.CHEST, false);
        updates.put(Flags.CONTAINER, false);

        assertThrows(UnsupportedOperationException.class, () -> view.putAll(updates));
        assertThrows(UnsupportedOperationException.class, () -> view.replaceAll((flag, value) -> false));
        Iterator<Map.Entry<PriFlag, Boolean>> entries = view.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<PriFlag, Boolean> entry = entries.next();
            if (entry.getKey() == Flags.CONTAINER) {
                assertThrows(UnsupportedOperationException.class, () -> entry.setValue(null));
                assertThrows(UnsupportedOperationException.class, entries::remove);
            }
        }

        assertEquals(Map.of(Flags.CHEST, true), backing);
        assertEquals(true, view.get(Flags.CONTAINER));
    }

    @Test
    void activeMutationsRemainSupportedAndKeepAliasViewsConsistent() {
        Map<PriFlag, Boolean> backing = new LinkedHashMap<>();
        Map<PriFlag, Boolean> view = LegacyFlagMap.view(backing);
        view.put(Flags.CHEST, true);
        view.compute(Flags.CHEST, (flag, value) -> !value);
        assertEquals(false, view.get(Flags.CONTAINER));

        Map.Entry<PriFlag, Boolean> activeEntry = view.entrySet().stream()
                .filter(entry -> entry.getKey() == Flags.CHEST).findFirst().orElseThrow();
        assertEquals(false, activeEntry.setValue(true));
        assertEquals(true, view.get(Flags.CONTAINER));
        assertEquals(true, view.remove(Flags.CHEST));
        assertTrue(view.isEmpty());

        view.putAll(Map.of(Flags.BARREL, true));
        view.replaceAll((flag, value) -> false);
        assertEquals(Map.of(Flags.BARREL, false), backing);
        view.put(Flags.CHEST, true);
        view.clear();
        assertTrue(backing.isEmpty());
        assertFalse(view.containsKey(Flags.CONTAINER));
    }

    @Test
    void collectionViewsRejectStructuralMutationsBeforeChangingTheMap() {
        Map<PriFlag, Boolean> backing = new LinkedHashMap<>();
        backing.put(Flags.CHEST, true);
        Map<PriFlag, Boolean> view = LegacyFlagMap.view(backing);
        AtomicBoolean callbackCalled = new AtomicBoolean();
        List<Executable> mutations = List.of(
                () -> view.values().remove(true),
                () -> view.keySet().remove(Flags.CHEST),
                () -> view.keySet().retainAll(Set.of(Flags.CONTAINER)),
                () -> view.keySet().removeAll(Set.of(Flags.CONTAINER)),
                () -> view.keySet().removeAll(Set.of(Flags.CHEST, Flags.CONTAINER)),
                () -> view.entrySet().retainAll(Set.of(Map.entry(Flags.CONTAINER, true))),
                () -> view.entrySet().remove(Map.entry(Flags.CHEST, true)),
                () -> view.entrySet().removeAll(Set.of(
                        Map.entry(Flags.CHEST, true), Map.entry(Flags.CONTAINER, true))),
                () -> view.values().retainAll(Set.of(false)),
                () -> view.values().removeAll(Set.of(true)),
                () -> view.entrySet().removeIf(entry -> {
                    callbackCalled.set(true);
                    return true;
                }),
                () -> view.keySet().removeIf(flag -> {
                    callbackCalled.set(true);
                    return true;
                }),
                () -> view.values().removeIf(value -> {
                    callbackCalled.set(true);
                    return true;
                }),
                () -> view.keySet().clear(),
                () -> view.entrySet().clear(),
                () -> view.values().clear()
        );

        for (Executable mutation : mutations) {
            assertThrows(UnsupportedOperationException.class, mutation);
            assertEquals(Map.of(Flags.CHEST, true), backing);
        }
        assertFalse(callbackCalled.get());

        for (Iterator<?> iterator : List.of(view.keySet().iterator(), view.values().iterator(),
                view.entrySet().iterator())) {
            iterator.next();
            assertThrows(UnsupportedOperationException.class, iterator::remove);
            assertEquals(Map.of(Flags.CHEST, true), backing);
        }
    }

    @Test
    void removingAnActiveMapKeyAlsoRemovesItsAliasFromExistingViews() {
        Map<PriFlag, Boolean> backing = new LinkedHashMap<>();
        backing.put(Flags.CHEST, true);
        Map<PriFlag, Boolean> view = LegacyFlagMap.view(backing);
        Set<Map.Entry<PriFlag, Boolean>> entries = view.entrySet();
        Iterator<Map.Entry<PriFlag, Boolean>> iterator = entries.iterator();

        Map.Entry<PriFlag, Boolean> retainedActive = iterator.next();
        assertEquals(Flags.CHEST, retainedActive.getKey());
        view.remove(Flags.CHEST);

        assertFalse(iterator.hasNext());
        assertTrue(entries.isEmpty());
        assertThrows(IllegalStateException.class, () -> retainedActive.setValue(false));
        assertTrue(backing.isEmpty());
    }

    @Test
    void environmentAliasesWorkWithoutAddingPrivilegeKeys() {
        Map<EnvFlag, Boolean> backing = new LinkedHashMap<>();
        backing.put(Flags.BURN_BLOCK, true);
        Map<EnvFlag, Boolean> view = LegacyFlagMap.view(backing);

        assertEquals(true, view.get(Flags.BURN));
        assertEquals(Set.of(Flags.BURN_BLOCK, Flags.BURN), view.keySet());
        assertFalse(view.containsKey(Flags.CONTAINER));
        view.put(Flags.BURN_BLOCK, false);
        assertEquals(false, view.get(Flags.BURN));
    }
}
