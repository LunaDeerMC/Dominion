package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopyProviderHandlerTest {
    @Test
    void mapsOnlyMembersActuallyBelongingToSourceGroup() {
        MemberDTO sourceInGroup = member(1);
        MemberDTO sourceOutsideGroup = member(2);
        MemberDTO targetInGroup = member(101);
        MemberDTO targetOutsideGroup = member(102);

        List<MemberDTO> mapped = CopyProviderHandler.mapGroupMembers(
                List.of(sourceInGroup), Map.of(1, targetInGroup, 2, targetOutsideGroup));

        assertEquals(List.of(targetInGroup), mapped);
        assertEquals(2, sourceOutsideGroup.getId());
    }

    private static MemberDTO member(int id) {
        UUID uuid = UUID.nameUUIDFromBytes(String.valueOf(id).getBytes());
        return new MemberDTO() {
            public Integer getId() { return id; }
            public UUID getPlayerUUID() { return uuid; }
            public Integer getDomID() { return 1; }
            public Integer getGroupId() { return -1; }
            public Boolean getFlagValue(PriFlag flag) { return false; }
            public Map<PriFlag, Boolean> getFlagsValue() { return Map.of(); }
            public MemberDTO setFlagValue(PriFlag flag, Boolean value) { return this; }
            public PlayerDTO getPlayer() { throw new UnsupportedOperationException(); }
        };
    }
}
