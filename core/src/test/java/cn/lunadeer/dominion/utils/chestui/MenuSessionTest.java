package cn.lunadeer.dominion.utils.chestui;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MenuSessionTest {
    @Test
    void navigationPreservesParametersPageFilterAndBackStack() {
        MenuSession session = new MenuSession(UUID.randomUUID(), MenuRoute.of("MAIN"));
        MenuRoute list = MenuRoute.of("DOMINION_LIST").page(3).filter("north");
        session.push(list);
        session.push(MenuRoute.of("DASHBOARD").with("dom", 42));
        assertEquals(42, session.current().integer("dom"));
        assertTrue(session.back());
        assertEquals(1, session.current().page()); // applying a filter resets pagination
        assertEquals("north", session.current().filter());
        session.home();
        assertEquals("MAIN", session.current().id());
        assertFalse(session.back());
    }

    @Test
    void asyncGenerationRejectsStaleCompletions() {
        MenuSession session = new MenuSession(UUID.randomUUID(), MenuRoute.of("MAIN"));
        long first = session.beginAsync();
        long second = session.beginAsync();
        assertFalse(session.isCurrentAsync(first));
        assertTrue(session.isCurrentAsync(second));
        assertTrue(session.busy());
    }

    @Test
    void paginationClampsEmptyAndOutOfRangePages() {
        assertEquals(new Pagination(1, 1, 0, 0), Pagination.of(8, 0, 28));
        assertEquals(new Pagination(3, 3, 56, 57), Pagination.of(99, 57, 28));
    }

    @Test
    void chatInputUsesSixtySecondTimeout() {
        assertEquals(1200L, ChatInputService.TIMEOUT_TICKS);
    }
}
