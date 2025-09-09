package cn.lunadeer.dominion.misc.webMap;

import cn.lunadeer.dominion.events.dominion.DominionCreateEvent;
import cn.lunadeer.dominion.events.dominion.DominionDeleteEvent;
import cn.lunadeer.dominion.events.dominion.modify.DominionReSizeEvent;
import cn.lunadeer.dominion.events.dominion.modify.DominionRenameEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class RenderEventHandler implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDominionCreate(DominionCreateEvent event) {
        if (event.isCancelled()) return;
        event.afterCreated(dominionDTO -> {
            if (dominionDTO == null) return;
            WebMapRender.updateDominion(dominionDTO);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDominionResize(DominionReSizeEvent event) {
        if (event.isCancelled()) return;
        event.afterModified(newDom -> {
            if (newDom == null) return;
            WebMapRender.updateDominion(newDom);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDominionRename(DominionRenameEvent event) {
        if (event.isCancelled()) return;
        if (event.getDominion().getWorld() == null) return;
        String worldName = event.getDominion().getWorld().getName();
        event.afterModified(newDom -> {
            WebMapRender.removeDominion(worldName, event.getOldName());
            if (newDom == null) return;
            WebMapRender.updateDominion(newDom);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDominionDelete(DominionDeleteEvent event) {
        if (event.isCancelled()) return;
        if (event.getDominion().getWorld() == null) return;
        WebMapRender.removeDominion(event.getDominion().getWorld().getName(), event.getDominion().getName());
    }
}
