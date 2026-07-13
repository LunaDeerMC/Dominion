package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.doos.PlayerDOO;
import cn.lunadeer.dominion.misc.DominionException;
import cn.lunadeer.dominion.providers.PlayerProvider;
import cn.lunadeer.dominion.utils.Notification;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static cn.lunadeer.dominion.misc.Asserts.assertDominionOwner;

public final class PlayerProviderHandler extends PlayerProvider {
    public PlayerProviderHandler() {
        instance = this;
    }

    @Override
    public @NotNull List<PlayerDTO> getKnownPlayers() {
        return CacheManager.instance.getPlayerCache().getPlayers().stream()
                .sorted(Comparator.comparing(PlayerDTO::getLastKnownName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public @NotNull List<GroupDTO> getAvailableGroupTitles(@NotNull UUID player) {
        return java.util.stream.Stream.concat(
                        CacheManager.instance.getPlayerCache().getPlayerGroupTitleList(player).stream(),
                        CacheManager.instance.getPlayerOwnDominionDTOs(player).stream()
                                .flatMap(dominion -> dominion.getGroups().stream()))
                .collect(java.util.stream.Collectors.toMap(GroupDTO::getId, Function.identity(), (left, right) -> left))
                .values().stream()
                .sorted(Comparator.comparing(GroupDTO::getNamePlain, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public CompletableFuture<PlayerDTO> setGroupTitle(@NotNull Player operator, @Nullable GroupDTO group) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerDTO player = Objects.requireNonNull(CacheManager.instance.getPlayer(operator.getUniqueId()));
                if (group == null) {
                    ((PlayerDOO) player).setUsingGroupTitleID(-1);
                    return player;
                }
                DominionDTO dominion = Objects.requireNonNull(CacheManager.instance.getDominion(group.getDomID()));
                try {
                    assertDominionOwner(operator, dominion);
                } catch (Exception ignored) {
                    MemberDTO member = CacheManager.instance.getMember(dominion, operator);
                    if (member == null || !Objects.equals(member.getGroupId(), group.getId())) {
                        throw new DominionException(Language.groupTitleCommandText.groupNotBelonging, group.getNamePlain());
                    }
                }
                ((PlayerDOO) player).setUsingGroupTitleID(group.getId());
                Notification.info(operator, Language.groupTitleCommandText.usingTitleSuccess, group.getNamePlain());
                return player;
            } catch (Exception e) {
                Notification.error(operator, Language.groupTitleCommandText.usingTitleFail, e.getMessage());
                return null;
            }
        });
    }
}
