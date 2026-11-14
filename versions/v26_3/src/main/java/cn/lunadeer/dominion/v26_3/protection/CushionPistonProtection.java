package cn.lunadeer.dominion.v26_3.protection;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Cushion;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.util.BoundingBox;
import java.util.List;
import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

/** Paper 26.3's BlockAttachedEntity.move removes cushions without an EntityBreakEvent. */
public final class CushionPistonProtection {
    private CushionPistonProtection() {}

    public static void check(BlockPistonEvent event, List<Block> blocks, boolean extending) {
        Block piston = event.getBlock();
        BlockFace facing = ((Directional) piston.getBlockData()).getFacing();
        BlockFace movement = extending ? facing : facing.getOppositeFace();
        // Include the moving piston head even when no blocks are pushed or pulled.
        Block head = piston.getRelative(facing);
        BoundingBox headPath = BoundingBox.of(head).union(BoundingBox.of(piston));
        if (denied(event, headPath)) return;
        for (Block block : blocks) {
            BoundingBox original = movementBounds(block);
            if (original == null) continue;
            BoundingBox swept = original.clone().union(original.clone().shift(movement.getDirection()));
            // Honey blocks also carry entities standing on top during horizontal motion.
            if (block.getType() == Material.HONEY_BLOCK && movement.getModY() == 0) swept.expand(0, 0, 0, 0, 0.5, 0);
            if (denied(event, swept.expand(0.01))) return;
        }
    }

    private static BoundingBox movementBounds(Block block) {
        BoundingBox outline = block.getBoundingBox();
        if (outline.getVolume() > 0) return outline;

        // Empty outlines are reported at the world origin. Piston events also include
        // destroyed fluids, so do not turn an empty shape into a swept unit block.
        // Preserve protection for a block with collision geometry but no outline.
        BoundingBox collision = null;
        for (BoundingBox part : block.getCollisionShape().getBoundingBoxes()) {
            if (part.getVolume() <= 0) continue;
            if (collision == null) collision = part.clone();
            else collision.union(part);
        }
        // Collision boxes are block-local, unlike a nonempty outline bounding box.
        return collision == null ? null : collision.shift(block.getX(), block.getY(), block.getZ());
    }

    private static boolean denied(BlockPistonEvent event, BoundingBox area) {
        for (var entity : event.getBlock().getWorld().getNearbyEntities(area, e -> e instanceof Cushion)) {
            if (!checkEnvironmentFlag(entity.getLocation(), Flags.CUSHION_ENVIRONMENT_BREAK, event)) return true;
        }
        return false;
    }
}
