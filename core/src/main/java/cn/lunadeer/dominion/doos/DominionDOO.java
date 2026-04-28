package cn.lunadeer.dominion.doos;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.*;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.storage.repository.DominionRepository;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DominionDOO implements DominionDTO {

    private DominionCuboid cuboid;
    private Integer id;
    private UUID owner;
    private String name;
    private Integer parentDomId = -1;
    private String joinMessage = "";
    private String leaveMessage = "";
    private final Map<EnvFlag, Boolean> envFlags = new HashMap<>();
    private final Map<PriFlag, Boolean> preFlags = new HashMap<>();
    private String tp_location = "default";
    private String color = "#00BFFF";
    private UUID world_uid;
    private Integer serverId;
    private Boolean owner_glow = false;

    // Cache for the parsed UUID to avoid repeated string parsing
    private UUID cachedWorldUid = null;

    private static DominionDOO parse(DominionRepository.DominionRow row) {
        if (row == null) return null;
        DominionCuboid cuboid = new DominionCuboid(
                row.x1(), row.y1(), row.z1(), row.x2(), row.y2(), row.z2());
        return new DominionDOO(
                row.id(),
                row.owner(),
                row.name(),
                row.worldUid(),
                cuboid,
                row.parentDomId(),
                row.joinMessage(),
                row.leaveMessage(),
                row.envFlags(),
                row.guestFlags(),
                row.tpLocation(),
                row.color(),
                row.serverId(),
                row.ownerGlow()
        );
    }

    public static List<DominionDOO> selectAll(Integer serverId) throws SQLException {
        return DominionRepository.selectAll(serverId).stream().map(DominionDOO::parse).toList();
    }

    public static DominionDOO rootDominion() {
        return new DominionDOO(-1,
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                "根领地", UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new DominionCuboid(-2147483648, -2147483648, -2147483648, 2147483647, 2147483647, 2147483647),
                -1,
                "null", "null",
                new HashMap<>(), new HashMap<>(),
                "default", "#00BFFF", -1, false);
    }

    public static @Nullable DominionDOO select(Integer id) throws SQLException {
        if (id == -1) {
            return rootDominion();
        }
        return parse(DominionRepository.select(id));
    }

    public static @Nullable DominionDOO select(String name) throws SQLException {
        return parse(DominionRepository.select(name));
    }

    public static @NotNull DominionDOO insert(DominionDOO dominion) throws SQLException {
        DominionDOO inserted = parse(DominionRepository.insert(dominion.toRow()));
        if (inserted == null) {
            throw new SQLException("Failed to insert dominion.");
        }
        CacheManager.instance.getCache().getDominionCache().load(inserted.getId());
        return inserted;
    }

    public static void deleteById(Integer dominion) throws SQLException {
        DominionRepository.deleteById(dominion);
        CacheManager.instance.getCache().getDominionCache().delete(dominion);
    }

    // full constructor
    private DominionDOO(Integer id, UUID owner, String name, UUID world_uid,
                        DominionCuboid cuboid,
                        Integer parentDomId,
                        String joinMessage, String leaveMessage,
                        Map<EnvFlag, Boolean> envFlags,
                        Map<PriFlag, Boolean> preFlags,
                        String tp_location,
                        String color,
                        Integer serverId,
                        Boolean ownerGlow) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.world_uid = world_uid;
        this.cuboid = cuboid;
        this.parentDomId = parentDomId;
        this.joinMessage = joinMessage;
        this.leaveMessage = leaveMessage;
        this.envFlags.putAll(envFlags);
        this.preFlags.putAll(preFlags);
        this.tp_location = tp_location;
        this.color = color;
        this.serverId = serverId;
        this.owner_glow = ownerGlow;
    }

    // constructor for new dominion
    public DominionDOO(@NotNull UUID owner,
                       @NotNull String name,
                        @NotNull UUID world_uid,
                        @NotNull CuboidDTO cuboid,
                        @NotNull Integer parentDomId) {
        this.owner = owner;
        this.name = name;
        this.world_uid = world_uid;
        this.cuboid = new DominionCuboid(cuboid);
        this.parentDomId = parentDomId;
        this.joinMessage = Configuration.pluginMessage.defaultEnterMessage;
        this.leaveMessage = Configuration.pluginMessage.defaultLeaveMessage;
        this.serverId = Configuration.multiServer.serverId;
        this.owner_glow = false;
        for (EnvFlag flag : Flags.getAllEnvFlagsEnable()) {
            this.envFlags.put(flag, flag.getDefaultValue());
        }
        for (PriFlag flag : Flags.getAllPriFlagsEnable()) {
            this.preFlags.put(flag, flag.getDefaultValue());
        }
    }

    private DominionRepository.DominionRow toRow() {
        return new DominionRepository.DominionRow(
                id, owner, name, world_uid,
                cuboid.x1(), cuboid.y1(), cuboid.z1(), cuboid.x2(), cuboid.y2(), cuboid.z2(),
                parentDomId, joinMessage, leaveMessage, envFlags, preFlags, tp_location, color, serverId, owner_glow
        );
    }

    private static class DominionCuboid extends CuboidDTO {
        public DominionCuboid(int x1, int y1, int z1, int x2, int y2, int z2) {
            super(x1, y1, z1, x2, y2, z2);
        }

        public DominionCuboid(CuboidDTO superObj) {
            super(superObj.x1(), superObj.y1(), superObj.z1(), superObj.x2(), superObj.y2(), superObj.z2());
        }

        public DominionCuboid(ResultSet rs) throws SQLException {
            super(rs.getInt("x1"), rs.getInt("y1"), rs.getInt("z1"),
                    rs.getInt("x2"), rs.getInt("y2"), rs.getInt("z2"));
        }

    }

    // getters and setters
    @Override
    public @NotNull Integer getId() {
        return id;
    }

    /**
     * 设置领地ID，该方法不会更新数据库，仅用于构造对象
     *
     * @param id 领地ID
     * @return 领地
     */
    public @NotNull DominionDOO setId(Integer id) {
        this.id = id;
        return this;
    }

    @Override
    public @NotNull UUID getOwner() {
        return owner;
    }

    @Override
    public @NotNull PlayerDTO getOwnerDTO() {
        return Objects.requireNonNull(CacheManager.instance.getPlayer(getOwner()));
    }

    @Override
    public @NotNull DominionDOO setOwner(UUID owner) throws SQLException {
        this.owner = owner;
        DominionRepository.updateOwner(id, owner);
        CacheManager.instance.getCache().getDominionCache().load();
        return this;
    }

    @Override
    public @NotNull DominionDOO setOwner(Player owner) throws SQLException {
        return setOwner(owner.getUniqueId());
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull DominionDOO setName(String name) throws SQLException {
        String oldName = this.name;
        this.name = name;
        DominionRepository.updateName(id, name);
        CacheManager.instance.getCache().getDominionCache().dominionNameUpdate(oldName, name, getId());
        return this;
    }

    @Override
    public @Nullable World getWorld() {
        return Dominion.instance.getServer().getWorld(getWorldUid());
    }

    @Override
    public @NotNull UUID getWorldUid() {
        // Return cached UUID if available
        if (cachedWorldUid == null) {
            cachedWorldUid = world_uid;
        }
        return cachedWorldUid;
    }

    @Override
    public @NotNull CuboidDTO getCuboid() {
        return cuboid;
    }

    @Override
    public @NotNull DominionDOO setCuboid(@NotNull CuboidDTO cuboid) throws SQLException {
        this.cuboid = new DominionCuboid(cuboid);
        DominionRepository.updateCuboid(id, this.cuboid.x1(), this.cuboid.y1(), this.cuboid.z1(),
                this.cuboid.x2(), this.cuboid.y2(), this.cuboid.z2());
        CacheManager.instance.getCache().getDominionCache().load(getId());
        return this;
    }

    @Override
    public @NotNull Integer getParentDomId() {
        return parentDomId;
    }

    @Override
    public @NotNull String getJoinMessage() {
        return joinMessage;
    }

    @Override
    public @NotNull DominionDOO setJoinMessage(String joinMessage) throws SQLException {
        this.joinMessage = joinMessage;
        DominionRepository.updateJoinMessage(id, joinMessage);
        return this;
    }

    @Override
    public @NotNull String getLeaveMessage() {
        return leaveMessage;
    }

    @Override
    public @NotNull DominionDOO setLeaveMessage(String leaveMessage) throws SQLException {
        this.leaveMessage = leaveMessage;
        DominionRepository.updateLeaveMessage(id, leaveMessage);
        return this;
    }

    @Override
    public @NotNull Map<EnvFlag, Boolean> getEnvironmentFlagValue() {
        return envFlags;
    }

    /**
     * 获取领地某个环境配置的值
     *
     * @param flag 权限
     * @return 权限值
     */
    @Override
    public boolean getEnvFlagValue(@NotNull EnvFlag flag) {
        return envFlags.getOrDefault(flag, false);
    }

    @Override
    public @NotNull Map<PriFlag, Boolean> getGuestPrivilegeFlagValue() {
        return preFlags;
    }

    /**
     * 获取领地某个访客权限的值
     *
     * @param flag 权限
     * @return 权限值
     */
    @Override
    public boolean getGuestFlagValue(@NotNull PriFlag flag) {
        if (preFlags.equals(Flags.ADMIN)) { // guest's admin flag is always false
            return false;
        }
        return preFlags.getOrDefault(flag, false);
    }

    @Override
    public @NotNull DominionDOO setEnvFlagValue(@NotNull EnvFlag flag, @NotNull Boolean value) throws SQLException {
        envFlags.put(flag, value);
        DominionRepository.updateEnvFlag(id, flag, value);
        return this;
    }

    @Override
    public @NotNull DominionDOO setGuestFlagValue(@NotNull PriFlag flag, @NotNull Boolean value) throws SQLException {
        preFlags.put(flag, value);
        DominionRepository.updateGuestFlag(id, flag, value);
        return this;
    }


    @Override
    public @NotNull Location getTpLocation() {
        if (Objects.equals(tp_location, "default")) {
            return new Location(getWorld(),
                    (double) (cuboid.x1() + cuboid.x2()) / 2,
                    (double) (cuboid.y1() + cuboid.y2()) / 2,
                    (double) (cuboid.z1() + cuboid.z2()) / 2);
        } else {
            // 0:0:0
            String[] loc = tp_location.split(":");
            World w = getWorld();
            if (loc.length == 3 && w != null) {
                return new Location(w, Integer.parseInt(loc[0]), Integer.parseInt(loc[1]), Integer.parseInt(loc[2]));
            } else if (loc.length == 5 && w != null) {
                return new Location(w, Integer.parseInt(loc[0]), Integer.parseInt(loc[1]), Integer.parseInt(loc[2]),
                        Float.parseFloat(loc[3]), Float.parseFloat(loc[4]));
            } else {
                return new Location(getWorld(),
                        (double) (cuboid.x1() + cuboid.x2()) / 2,
                        (double) (cuboid.y1() + cuboid.y2()) / 2,
                        (double) (cuboid.z1() + cuboid.z2()) / 2);
            }
        }
    }

    @Override
    public @NotNull DominionDOO setTpLocation(Location loc) throws SQLException {
        this.tp_location = loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ() + ":" + loc.getYaw() + ":" + loc.getPitch();
        DominionRepository.updateTpLocation(id, this.tp_location);
        return this;
    }

    public @NotNull DominionDOO setColor(@NotNull Color color) throws SQLException {
        this.color = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        DominionRepository.updateColor(id, this.color);
        return this;
    }

    @Override
    public List<GroupDTO> getGroups() {
        return Objects.requireNonNull(CacheManager.instance.getCache(getServerId())).getGroupCache().getDominionGroups(this);
    }

    @Override
    public List<MemberDTO> getMembers() {
        return Objects.requireNonNull(CacheManager.instance.getCache(getServerId())).getMemberCache().getDominionMembers(this);
    }

    @Override
    public Integer getServerId() {
        return serverId;
    }

    @Override
    public int getColorR() {
        return Integer.valueOf(getColor().substring(1, 3), 16);
    }

    @Override
    public int getColorG() {
        return Integer.valueOf(getColor().substring(3, 5), 16);
    }

    @Override
    public int getColorB() {
        return Integer.valueOf(getColor().substring(5, 7), 16);
    }

    @Override
    public @NotNull String getColor() {
        return color;
    }

    @Override
    public int getColorHex() {
        return (getColorR() << 16) + (getColorG() << 8) + getColorB();
    }

    /**
     * Delete dominion by player UUID.
     * <p>
     * THIS SHOULD ONLY BE USED TO CLEAR LEGACY DATA.
     *
     * @param playerUUID the UUID of the player to delete
     * @throws SQLException if a database access error occurs
     */
    public static void deleteByPlayerUuid(UUID playerUUID) throws SQLException {
        DominionRepository.deleteByPlayerUuid(playerUUID);
    }

    @Override
    public Boolean getOwnerGlow() {
        return owner_glow;
    }

    @Override
    public @NotNull DominionDOO setOwnerGlow(Boolean ownerGlow) throws SQLException {
        this.owner_glow = ownerGlow;
        DominionRepository.updateOwnerGlow(id, ownerGlow);
        return this;
    }
}
