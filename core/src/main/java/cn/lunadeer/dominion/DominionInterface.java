package cn.lunadeer.dominion;

import cn.lunadeer.dominion.api.DominionAPI;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.misc.Others;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class DominionInterface extends DominionAPI {

    public DominionInterface() {
        instance = this;
    }

    @Override
    public @Nullable PlayerDTO getPlayer(String name) {
        return CacheManager.instance.getPlayer(name);
    }

    @Override
    public @Nullable PlayerDTO getPlayer(@NotNull UUID player) {
        return CacheManager.instance.getPlayer(player);
    }

    @Override
    public @NotNull String getPlayerName(@NotNull UUID uuid) {
        return CacheManager.instance.getPlayerName(uuid);
    }

    @Override
    public List<DominionDTO> getAllDominions() {
        return CacheManager.instance.getAllDominions();
    }

    @Override
    public List<DominionDTO> getAllDominionsOfPlayer(@NotNull UUID player) {
        return CacheManager.instance.getPlayerOwnDominionDTOs(player);
    }

    @Override
    public List<DominionDTO> getChildrenDominionOf(DominionDTO parent) {
        return CacheManager.instance.getChildrenDominionOf(parent);
    }

    @Override
    public @Nullable DominionDTO getDominion(Integer id) {
        return CacheManager.instance.getDominion(id);
    }

    @Override
    public @Nullable DominionDTO getDominion(String name) {
        return CacheManager.instance.getDominion(name);
    }

    @Override
    public @Nullable DominionDTO getDominion(Location location) {
        return CacheManager.instance.getDominion(location);
    }

    @Override
    public List<DominionDTO> getPlayerOwnDominionDTOs(UUID player) {
        return CacheManager.instance.getPlayerOwnDominionDTOs(player);
    }

    @Override
    public List<DominionDTO> getPlayerAdminDominionDTOs(UUID player) {
        return CacheManager.instance.getPlayerAdminDominionDTOs(player);
    }

    @Override
    public @Nullable MemberDTO getMember(@Nullable DominionDTO dominion, @NotNull Player player) {
        return CacheManager.instance.getMember(dominion, player);
    }

    @Override
    public @Nullable MemberDTO getMember(@Nullable DominionDTO dominion, @NotNull UUID player) {
        return CacheManager.instance.getMember(dominion, player);
    }

    @Override
    public @Nullable GroupDTO getGroup(MemberDTO member) {
        return CacheManager.instance.getGroup(member);
    }

    @Override
    public @Nullable GroupDTO getGroup(Integer id) {
        return CacheManager.instance.getGroup(id);
    }

    @Override
    public DominionDTO getPlayerCurrentDominion(@NotNull Player player) {
        return CacheManager.instance.getPlayerCurrentDominion(player);
    }

    @Override
    public void resetPlayerCurrentDominionId(@NotNull Player player) {
        CacheManager.instance.resetPlayerCurrentDominionId(player);
    }

    @Override
    public Integer dominionCount() {
        return CacheManager.instance.dominionCount();
    }

    @Override
    public Integer groupCount() {
        return CacheManager.instance.groupCount();
    }

    @Override
    public Integer memberCount() {
        return CacheManager.instance.memberCount();
    }


    @Override
    public boolean checkPrivilegeFlag(@NotNull Location location, @NotNull PriFlag flag, @NotNull Player player) {
        return Others.checkPrivilegeFlag(location, flag, player, null);
    }

    @Override
    public boolean checkPrivilegeFlag(@Nullable DominionDTO dom, @NotNull PriFlag flag, @NotNull Player player) {
        return Others.checkPrivilegeFlag(dom, flag, player, null);
    }

    @Override
    public boolean checkPrivilegeFlagSilence(@NotNull Location location, @NotNull PriFlag flag, @NotNull Player player) {
        return Others.checkPrivilegeFlagSilence(location, flag, player, null);
    }

    @Override
    public boolean checkPrivilegeFlagSilence(@Nullable DominionDTO dom, @NotNull PriFlag flag, @NotNull Player player) {
        return Others.checkPrivilegeFlagSilence(dom, flag, player, null);
    }

    @Override
    public boolean checkEnvironmentFlag(@NotNull Location location, @NotNull EnvFlag flag) {
        return Others.checkEnvironmentFlag(location, flag, null);
    }

    @Override
    public boolean checkEnvironmentFlag(@Nullable DominionDTO dom, @NotNull EnvFlag flag) {
        return Others.checkEnvironmentFlag(dom, flag, null);
    }

    @Override
    public void reloadCache() {
        CacheManager.instance.getCache().getDominionCache().load();
        CacheManager.instance.getCache().getMemberCache().load();
        CacheManager.instance.getCache().getGroupCache().load();
    }

    @Override
    public void reloadConfig() throws Exception {
        Configuration.loadConfigurationAndDatabase(Bukkit.getConsoleSender());
    }

}
