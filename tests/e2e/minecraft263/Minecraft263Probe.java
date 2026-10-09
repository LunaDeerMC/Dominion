import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.*;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.doos.PlayerDOO;
import cn.lunadeer.dominion.misc.Others;
import cn.lunadeer.dominion.nms.*;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.utils.XVersionManager;
import cn.lunadeer.dominion.utils.dialogui.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

/** Disposable-server integration probe. Event dispatch tests are distinct from client actions. */
public final class Minecraft263Probe extends JavaPlugin {
    private int checks, failures, callbacks;
    @Override public void onEnable() { getCommand("probe263").setExecutor(this); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) return true;
        Player player = Bukkit.getPlayerExact("Probe263");
        if (player == null) { check(false, "protocol client connected"); return true; }
        try {
            check(Bukkit.getMinecraftVersion().equals("26.3"), "runtime 26.3");
            check(XVersionManager.VERSION.name().equals("v26_3"), "version dispatch");
            UUID owner = UUID.fromString("00000000-0000-0000-0000-000000002630");
            PlayerDOO.create(owner, "ProbeOwner");
            World world = player.getWorld();
            DominionProvider.getInstance().createDominion(Bukkit.getConsoleSender(), "probe263_"+System.currentTimeMillis(), owner,
                world, new CuboidDTO(100, 60, 100, 130, 100, 130), null, true)
                .thenAccept(dom -> Bukkit.getScheduler().runTaskLater(this, () -> {
                    try { run(player, dom); } catch (Throwable t) { fail(t); }
                }, 40L));
        } catch (Throwable t) { fail(t); }
        return true;
    }
    private void run(Player player, DominionDTO created) throws Exception {
        check(created != null, "create dominion");
        DominionDTO dom = CacheManager.instance.getDominion(created.getId());
        check(dom != null, "cache populated");
        Location loc = new Location(player.getWorld(), 110, 70, 110);
        player.teleport(loc); player.setGameMode(GameMode.CREATIVE); player.setOp(false); player.setAllowFlight(true); player.setFlying(true);
        // Exercise every enabled existing privilege flag's guest allow/deny decision.
        for (PriFlag flag : Flags.getActivePriFlagsEnable()) {
            if (flag == Flags.ADMIN) continue;
            var flags = dom.getGuestPrivilegeFlagValue(); Boolean before = flags.get(flag);
            flags.put(flag, false); check(!Others.checkPrivilegeFlagSilence(loc, flag, player, null), "deny "+flag.getFlagName());
            flags.put(flag, true); check(Others.checkPrivilegeFlagSilence(loc, flag, player, null), "allow "+flag.getFlagName());
            flags.put(flag, before);
        }
        for (EnvFlag flag : Flags.getActiveEnvFlagsEnable()) {
            var flags = dom.getEnvironmentFlagValue(); Boolean before = flags.get(flag);
            flags.put(flag, false); check(!Others.checkEnvironmentFlag(loc, flag, null), "deny "+flag.getFlagName());
            flags.put(flag, true); check(Others.checkEnvironmentFlag(loc, flag, null), "allow "+flag.getFlagName());
            flags.put(flag, before);
        }
        Map<PriFlag,Boolean> previous = new HashMap<>();
        dom.getGuestPrivilegeFlagValue().forEach((flag, value) -> {
            if (!Flags.isLegacyFlag(flag)) previous.put(flag, value);
        });
        previous.keySet().forEach(flag -> dom.getGuestPrivilegeFlagValue().put(flag, true));
        try {
            String[][] cases = {
                {"OAK_DOOR","DOOR"},{"OAK_TRAPDOOR","TRAPDOOR"},{"OAK_FENCE_GATE","FENCE_GATE"},
                {"STONE_BUTTON","BUTTON"},{"LEVER","LEVER"},{"STONE_PRESSURE_PLATE","PRESSURE"},
                {"RED_BED","BED"},{"CRAFTING_TABLE","CRAFT"},{"ENCHANTING_TABLE","ENCHANT"},
                {"ANVIL","ANVIL"},{"BEACON","BEACON"},{"BREWING_STAND","BREW"},
                {"CHEST","CHEST"},{"BARREL","BARREL"},{"FURNACE","FURNACE"},
                {"BLAST_FURNACE","BLAST_FURNACE"},{"SMOKER","SMOKER"},{"HOPPER","HOPPER"},
                {"DISPENSER","DISPENSER"},{"DROPPER","DROPPER"},{"SHULKER_BOX","SHULKER_BOX"},
                {"OAK_SHELF","SHELF"},{"SPRUCE_SHELF","SHELF"},{"BIRCH_SHELF","SHELF"},
                {"JUNGLE_SHELF","SHELF"},{"ACACIA_SHELF","SHELF"},{"DARK_OAK_SHELF","SHELF"},
                {"MANGROVE_SHELF","SHELF"},{"CHERRY_SHELF","SHELF"},{"PALE_OAK_SHELF","SHELF"},
                {"BAMBOO_SHELF","SHELF"},{"CRIMSON_SHELF","SHELF"},{"WARPED_SHELF","SHELF"},
                {"POPLAR_DOOR","DOOR"},{"POPLAR_TRAPDOOR","TRAPDOOR"},{"POPLAR_FENCE_GATE","FENCE_GATE"},
                {"POPLAR_BUTTON","BUTTON"},{"POPLAR_PRESSURE_PLATE","PRESSURE"},{"POPLAR_SHELF","SHELF"},
                {"STRAW_BED","BED"}
            };
            Block block = loc.getBlock();
            for (String[] c : cases) {
                PriFlag flag = (PriFlag)Flags.class.getField(c[1]).get(null);
                block.setType(Material.valueOf(c[0]),false);
                for (boolean allow : new boolean[]{false,true}) {
                    dom.getGuestPrivilegeFlagValue().put(flag,allow);
                    var event = new PlayerInteractEvent(player, c[1].equals("PRESSURE") ? Action.PHYSICAL : Action.RIGHT_CLICK_BLOCK,
                        new ItemStack(Material.STICK), block, BlockFace.UP, EquipmentSlot.HAND);
                    event.setUseInteractedBlock(Event.Result.DEFAULT); event.setUseItemInHand(Event.Result.DEFAULT);
                    Bukkit.getPluginManager().callEvent(event);
                    boolean denied=event.useInteractedBlock()==Event.Result.DENY;
                    check(denied != allow,c[0]+" "+c[1]+" allow="+allow);
                }
                dom.getGuestPrivilegeFlagValue().put(flag,true);
            }
            dom.getGuestPrivilegeFlagValue().put(Flags.SHELF,false);
            for (String material : List.of("SHELF_MUSHROOM", "BOOKSHELF", "CHISELED_BOOKSHELF", "STONE")) {
                block.setType(Material.valueOf(material), false);
                var event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                    new ItemStack(Material.STICK), block, BlockFace.UP, EquipmentSlot.HAND);
                event.setUseInteractedBlock(Event.Result.DEFAULT); event.setUseItemInHand(Event.Result.DEFAULT);
                Bukkit.getPluginManager().callEvent(event);
                check(event.useInteractedBlock()!=Event.Result.DENY,material+" unaffected by SHELF=false");
            }
            dom.getGuestPrivilegeFlagValue().put(Flags.SHELF,true);
            block.setType(Material.STONE,false);
            for (boolean allow : new boolean[]{false,true}) {
                dom.getGuestPrivilegeFlagValue().put(Flags.BREAK_BLOCK,allow);
                var event=new BlockBreakEvent(block,player); Bukkit.getPluginManager().callEvent(event);
                check(event.isCancelled()!=allow,"block break allow="+allow);
            }
        } finally { dom.getGuestPrivilegeFlagValue().clear();dom.getGuestPrivilegeFlagValue().putAll(previous); }
        NMSManager nms=NMSManager.instance();
        check(nms.getFakeEntityFactory().getClass().getName().contains("v26_3"),"display backend");
        var blockDisplay=nms.getFakeEntityFactory().createBlockDisplay(loc,Material.STONE.createBlockData());
        var itemDisplay=nms.getFakeEntityFactory().createItemDisplay(loc,new ItemStack(Material.DIAMOND));
        check(blockDisplay.getEntityId()!=itemDisplay.getEntityId(),"unique display IDs");
        for (var entity : List.of(blockDisplay,itemDisplay)) {
            entity.setGlowColor(Color.AQUA); entity.spawn(player); entity.sendMetadata(player);
            entity.teleport(loc.clone().add(1,1,1),player); entity.destroy(player);
        }
        var factory=nms.getDialogFactory().orElseThrow();
        check(factory.isSupported(),"dialog supported");
        check(nms.getDialogCallbackBridge().orElseThrow().install(player),"callback bridge installed");
        var action=DialogSpec.ActionButton.of(Component.text("Probe callback"),new DialogSpec.CallbackAction((p,r)->{
            callbacks++;getLogger().info("CALLBACK received="+callbacks);
        }));
        var dialog=DialogSpec.builder(Component.text("26.3 protocol probe"),new DialogSpec.Notice(action))
            .body(new DialogSpec.PlainMessageBody(Component.text("Network round trip"),200)).build();
        check(factory.validate(dialog).successful(),"dialog validation");
        check(factory.show(player,dialog,new DialogSessionContext(UUID.randomUUID(),1)).successful(),"dialog send");
        Bukkit.getScheduler().runTaskLater(this,()->{
            check(callbacks==1,"network callback and replay rejection");factory.close(player);
            networkBreak(player,dom,loc);
        },100L);
    }
    private void networkBreak(Player player, DominionDTO dom, Location loc) {
        Block target=loc.clone().add(2,0,0).getBlock(); target.setType(Material.STONE,false);
        Boolean before=dom.getGuestPrivilegeFlagValue().get(Flags.BREAK_BLOCK);
        dom.getGuestPrivilegeFlagValue().put(Flags.BREAK_BLOCK,false);
        player.sendMessage(Component.text("PROBE263_BREAK:112:70:110"));
        Bukkit.getScheduler().runTaskLater(this,()->{
            check(target.getType()==Material.STONE,"network block break denied");
            dom.getGuestPrivilegeFlagValue().put(Flags.BREAK_BLOCK,true);
            player.sendMessage(Component.text("PROBE263_BREAK:112:70:110"));
            Bukkit.getScheduler().runTaskLater(this,()->{
                check(target.getType()==Material.AIR,"network block break allowed");
                dom.getGuestPrivilegeFlagValue().put(Flags.BREAK_BLOCK,before);
                getLogger().info("SUMMARY checks="+checks+" failures="+failures);
            },40L);
        },40L);
    }
    private void check(boolean value,String name) { checks++;if(!value)failures++;getLogger().info((value?"PASS ":"FAIL ")+name); }
    private void fail(Throwable t) { failures++;getLogger().log(java.util.logging.Level.SEVERE,"FAIL exception",t);getLogger().info("SUMMARY checks="+checks+" failures="+failures); }
}
