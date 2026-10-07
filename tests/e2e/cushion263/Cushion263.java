import cn.lunadeer.dominion.api.dtos.*;
import cn.lunadeer.dominion.api.dtos.flag.*;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.doos.PlayerDOO;
import cn.lunadeer.dominion.providers.DominionProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.damage.CraftDamageSource;
import org.bukkit.craftbukkit.entity.CraftCushion;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

/** Disposable Paper 26.3 server only. Creates claims and changes blocks. */
public class Cushion263 extends JavaPlugin {
    private int checks, failures;
    private Player player;
    private DominionDTO dom;
    private World world;
    private Location at;
    private final List<Entity> entities = new ArrayList<>();
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) return true;
        try {
            if (args.length>0 && args[0].equals("verify")) {
                DominionDTO saved=CacheManager.instance.getDominion(getConfig().getString("claim"));
                check(saved!=null,"claim survives restart");
                check(saved.getGuestFlagValue(Flags.CUSHION_PLACE),"place value persists");
                check(saved.getGuestFlagValue(Flags.CUSHION_BREAK),"break value persists");
                check(saved.getEnvFlagValue(Flags.CUSHION_MOB_DAMAGE),"mob value persists");
                check(saved.getEnvFlagValue(Flags.CUSHION_ENVIRONMENT_BREAK),"environment value persists");
                groups();summary();return true;
            }
            player=Bukkit.getPlayerExact("Probe263");world=player.getWorld();at=new Location(world,522.5,70,520.5);
            groups();
            UUID owner=UUID.fromString("00000000-0000-0000-0000-000000263001");
            PlayerDOO.create(owner,"CushionOwner");
            DominionProvider.getInstance().createDominion(Bukkit.getConsoleSender(),"cushion_"+System.currentTimeMillis(),owner,
                world,new CuboidDTO(510,60,510,540,90,540),null,true).thenAccept(created->
                Bukkit.getScheduler().runTaskLater(this,()->{
                    try { begin(created); } catch(Throwable t) { fail(t); }
                },40));
        } catch(Throwable t) {fail(t);}
        return true;
    }
    private void groups() {
        for(String id:List.of("building","decoration","cushion")) {
            var g=FlagGroups.getPriFlagGroup(id);check(g!=null,"privilege group "+id);
            check(g.containsFlag(Flags.CUSHION_PLACE)&&g.containsFlag(Flags.CUSHION_BREAK),id+" memberships");
        }
        for(String id:List.of("entity-protection","cushion")) {
            var g=FlagGroups.getEnvFlagGroup(id);check(g!=null,"environment group "+id);
            check(g.containsFlag(Flags.CUSHION_MOB_DAMAGE)&&g.containsFlag(Flags.CUSHION_ENVIRONMENT_BREAK),id+" memberships");
        }
        var y=YamlConfiguration.loadConfiguration(new java.io.File("plugins/Dominion/flags.yml"));
        check(y.getInt("schema-version")==6,"schema upgraded to 6");
        check(y.getStringList("groups.privilege.custom-preserved.flags").equals(List.of("place")),"custom group preserved");
        check("EMERALD".equals(y.getString("groups.privilege.building.material")),"custom material preserved");
    }
    private void begin(DominionDTO created) throws Exception {
        dom=CacheManager.instance.getDominion(created.getId());
        for (Flag f:List.of(Flags.CUSHION_PLACE,Flags.CUSHION_BREAK,Flags.CUSHION_MOB_DAMAGE,Flags.CUSHION_ENVIRONMENT_BREAK)) {
            check(!f.getDefaultValue(),"default false "+f.getFlagName());
        }
        check(!dom.getGuestFlagValue(Flags.CUSHION_PLACE)&&!dom.getGuestFlagValue(Flags.CUSHION_BREAK),"new claim denies player flags");
        check(!dom.getEnvFlagValue(Flags.CUSHION_MOB_DAMAGE)&&!dom.getEnvFlagValue(Flags.CUSHION_ENVIRONMENT_BREAK),"new claim denies environment flags");
        dom.setGuestFlagValue(Flags.ARROW_HIT,true);
        player.setOp(false);player.setGameMode(GameMode.CREATIVE);player.setAllowFlight(true);player.setFlying(true);
        for(int x=518;x<526;x++)for(int z=518;z<524;z++)world.getBlockAt(x,69,z).setType(Material.STONE);
        player.teleport(new Location(world,520.5,70,520.5));
        player.getInventory().setItemInMainHand(new ItemStack(Material.WHITE_CUSHION));
        later(()->place(false),20);
    }
    private void place(boolean allow) throws Exception {
        dom.setGuestFlagValue(Flags.CUSHION_PLACE,allow);
        player.sendMessage(Component.text("CUSHION_PLACE:522:69:520"));
        later(()->{
            List<Cushion> cushions=world.getNearbyEntities(at,1,1,1).stream().filter(e->e instanceof Cushion).map(e->(Cushion)e).toList();
            check(cushions.size()==(allow?1:0),"network place allow="+allow);
            if(!allow)place(true);
            else { entities.addAll(cushions); attack(cushions.get(0),false); }
        },40);
    }
    private void attack(Cushion cushion,boolean allow) throws Exception {
        dom.setGuestFlagValue(Flags.CUSHION_BREAK,allow);
        player.getInventory().setItemInMainHand(new ItemStack(Material.STICK));
        player.sendMessage(Component.text("CUSHION_ATTACK:"+cushion.getEntityId()));
        later(()->{
            check(cushion.isDead()==allow,"network direct break allow="+allow);
            if(!allow)attack(cushion,true); else engine();
        },40);
    }
    private Cushion cushion() {
        Cushion c=world.spawn(at,Cushion.class);entities.add(c);return c;
    }
    private void engine() throws Exception {
        Skeleton mob=world.spawn(at.clone().add(0,0,2),Skeleton.class);mob.setAI(false);entities.add(mob);
        Arrow playerArrow=world.spawn(at,Arrow.class);playerArrow.setShooter(player);entities.add(playerArrow);
        Arrow mobArrow=world.spawn(at,Arrow.class);mobArrow.setShooter(mob);entities.add(mobArrow);
        Arrow ownerless=world.spawn(at,Arrow.class);entities.add(ownerless);
        world.getBlockAt(525,70,520).setType(Material.DISPENSER);
        Arrow dispenserArrow=world.spawn(at,Arrow.class);
        dispenserArrow.setShooter(((org.bukkit.block.Dispenser)world.getBlockAt(525,70,520).getState()).getBlockProjectileSource());
        entities.add(dispenserArrow);
        for(boolean allow:new boolean[]{false,true}) {
            // Opposite settings ensure attribution is not accidentally governed by another cushion flag.
            dom.setGuestFlagValue(Flags.CUSHION_BREAK,allow);
            dom.setEnvFlagValue(Flags.CUSHION_MOB_DAMAGE,!allow);
            dom.setEnvFlagValue(Flags.CUSHION_ENVIRONMENT_BREAK,!allow);
            hit("player projectile",DamageType.ARROW,playerArrow,player,allow);
            dom.setGuestFlagValue(Flags.CUSHION_BREAK,!allow);
            dom.setEnvFlagValue(Flags.CUSHION_MOB_DAMAGE,allow);
            hit("mob melee",DamageType.MOB_ATTACK,mob,mob,allow);
            hit("mob projectile",DamageType.ARROW,mobArrow,mob,allow);
            dom.setEnvFlagValue(Flags.CUSHION_MOB_DAMAGE,!allow);
            dom.setEnvFlagValue(Flags.CUSHION_ENVIRONMENT_BREAK,allow);
            hit("ownerless projectile",DamageType.ARROW,ownerless,null,allow);
            hit("dispenser projectile",DamageType.ARROW,dispenserArrow,null,allow);
            hit("fire",DamageType.IN_FIRE,null,null,allow);
            Cushion unsupported=cushion();
            world.getBlockAt(522,69,520).setType(Material.AIR,false);
            for(int tick=0;tick<101&&!unsupported.isDead();tick++)((CraftCushion)unsupported).getHandle().tick();
            check(unsupported.isDead()==allow,"support loss allow="+allow);unsupported.remove();
            world.getBlockAt(522,69,520).setType(Material.STONE,false);
            dom.setEnvFlagValue(Flags.CUSHION_ENVIRONMENT_BREAK,!allow);
            dom.setEnvFlagValue(Flags.TNT_DAMAGE_ENTITY,allow);
            TNTPrimed tnt=world.spawn(at,TNTPrimed.class);entities.add(tnt);tnt.setFuseTicks(10000);
            hit("TNT existing flag",DamageType.EXPLOSION,tnt,tnt,allow);
        }
        for(Entity e:entities)e.remove();
        piston(false);
    }
    private void piston(boolean allow) throws Exception {
        dom.setEnvFlagValue(Flags.CUSHION_ENVIRONMENT_BREAK,allow);
        world.getBlockAt(521,70,519).setType(Material.AIR);
        world.getBlockAt(521,70,520).setType(Material.AIR);
        world.getBlockAt(522,70,520).setType(Material.AIR);
        later(()->{
            world.getBlockAt(521,70,520).setBlockData(Bukkit.createBlockData("minecraft:piston[facing=east]"));
            Cushion c=cushion();
            world.getBlockAt(521,70,519).setType(Material.REDSTONE_BLOCK);
            later(()->{
                check(c.isDead()==allow,"real piston removal allow="+allow);c.remove();
                if(!allow)piston(true);else finish();
            },20);
        },10);
    }
    private void finish() throws Exception {
        for(Entity e:entities)e.remove();
        dom.setGuestFlagValue(Flags.CUSHION_PLACE,true);dom.setGuestFlagValue(Flags.CUSHION_BREAK,true);
        dom.setEnvFlagValue(Flags.CUSHION_MOB_DAMAGE,true);dom.setEnvFlagValue(Flags.CUSHION_ENVIRONMENT_BREAK,true);
        getConfig().set("claim",dom.getName());saveConfig();summary();
    }
    private void hit(String name,DamageType type,Entity direct,Entity causing,boolean allow) {
        Cushion c=cushion();var builder=DamageSource.builder(type);
        if(direct!=null)builder.withDirectEntity(direct);
        if(causing!=null)builder.withCausingEntity(causing);
        ((CraftCushion)c).getHandle().hurtServer(((CraftWorld)world).getHandle(),((CraftDamageSource)builder.build()).getHandle(),1);
        check(c.isDead()==allow,name+" allow="+allow);c.remove();
    }
    private interface Step {void run() throws Exception;}
    private void later(Step step,long ticks) {Bukkit.getScheduler().runTaskLater(this,()->{try {step.run();}catch(Throwable t){fail(t);}},ticks);}
    private void check(boolean ok,String text){checks++;if(!ok)failures++;getLogger().info((ok?"PASS ":"FAIL ")+text);}
    private void fail(Throwable t){failures++;getLogger().log(java.util.logging.Level.SEVERE,"FAIL exception",t);summary();}
    private void summary(){getLogger().info("SUMMARY checks="+checks+" failures="+failures);}
}
