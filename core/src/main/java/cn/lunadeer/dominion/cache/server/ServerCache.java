package cn.lunadeer.dominion.cache.server;

import cn.lunadeer.dominion.utils.McaRecord;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ServerCache {

    private final int serverId;

    private final DominionCache dominionCache;
    private final MemberCache memberCache;
    private final GroupCache groupCache;
    private final List<McaRecord> mcaWhitelistCache;

    public ServerCache(int serverId) {
        this.serverId = serverId;
        this.dominionCache = new DominionCache(serverId);
        this.memberCache = new MemberCache(serverId);
        this.groupCache = new GroupCache(serverId);
        this.mcaWhitelistCache = new ArrayList<>();
    }

    public Integer getServerId() {
        return serverId;
    }

    public @NotNull DominionCache getDominionCache() {
        return dominionCache;
    }

    public @NotNull MemberCache getMemberCache() {
        return memberCache;
    }

    public @NotNull GroupCache getGroupCache() {
        return groupCache;
    }

    public List<McaRecord> getMcaWhitelistCache() {
        return mcaWhitelistCache;
    }
}
