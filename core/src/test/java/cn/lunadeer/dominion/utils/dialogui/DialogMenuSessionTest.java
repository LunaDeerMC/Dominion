package cn.lunadeer.dominion.utils.dialogui;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DialogMenuSessionTest {
    @Test
    void scopedFormDraftSurvivesNavigationAndCanBeDiscarded() {
        DialogMenuSession session = new DialogMenuSession(
                UUID.randomUUID(), DialogRoute.of("MAIN"));
        session.state("flag-draft:one:build", true);
        session.state("flag-draft:one:break", false);
        session.state("other", "kept");

        session.push(DialogRoute.of("FLAG_LIST").page(2));
        assertEquals(true, session.state("flag-draft:one:build", Boolean.class));
        assertEquals(false, session.state("flag-draft:one:break", Boolean.class));

        session.clearState("flag-draft:one:");
        assertNull(session.state("flag-draft:one:build", Boolean.class));
        assertNull(session.state("flag-draft:one:break", Boolean.class));
        assertEquals("kept", session.state("other", String.class));
    }
}
