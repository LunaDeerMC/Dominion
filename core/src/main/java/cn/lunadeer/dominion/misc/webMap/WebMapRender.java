package cn.lunadeer.dominion.misc.webMap;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.misc.webMap.implementations.BlueMapConnect;
import cn.lunadeer.dominion.misc.webMap.implementations.DynmapConnect;
import cn.lunadeer.dominion.misc.webMap.implementations.Pl3xMapConnect;
import cn.lunadeer.dominion.misc.webMap.implementations.SquareMapConnect;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class WebMapRender {

    public static void WebMapRenderInit() {
        if (Configuration.webMapRenderer.dynmap) {
            try {
                new DynmapConnect();
            } catch (Throwable e) {
                XLogger.error(e);
            }
        }
        if (Configuration.webMapRenderer.blueMap) {
            try {
                new BlueMapConnect();
            } catch (Throwable e) {
                XLogger.error(e);
            }
        }
        if (Configuration.webMapRenderer.squareMap) {
            try {
                new SquareMapConnect();
            } catch (Throwable e) {
                XLogger.error(e);
            }
        }
        if (Configuration.webMapRenderer.pl3xMap) {
            try {
                new Pl3xMapConnect();
            } catch (Throwable e) {
                XLogger.error(e);
            }
        }

        Bukkit.getServer().getPluginManager().registerEvents(new RenderEventHandler(), Dominion.instance);

        Scheduler.runTaskLaterAsync(() -> {
            for (WebMapRender mapRender : webMapInstances) {
                mapRender.renderAll(CacheManager.instance.getCache().getDominionCache().getAllDominions());
            }
        }, Configuration.webMapRenderer.renderDelaySec * 20L);
    }

    public static void renderAllMCA(@NotNull Map<String, List<String>> mcaFiles) {
        Scheduler.runTaskAsync(() -> {
            for (WebMapRender mapRender : webMapInstances) {
                mapRender.renderMCA(mcaFiles);
            }
        });
    }

    protected static List<WebMapRender> webMapInstances = new ArrayList<>();

    protected abstract void renderAll(@NotNull List<DominionDTO> dominions);

    /**
     * Updates a DominionDTO in the internal set for rendering.
     * Implementations should define how the dominion is updated.
     *
     * @param dominion the dominion to update
     */
    protected abstract void updateDominionInSet(@NotNull DominionDTO dominion);

    /**
     * Updates a DominionDTO in all registered web map renderers.
     *
     * @param dominion the dominion to update
     */
    public static void updateDominion(@NotNull DominionDTO dominion) {
        for (WebMapRender mapRender : webMapInstances) {
            mapRender.updateDominionInSet(dominion);
        }
    }

    /**
     * Removes a DominionDTO from the internal set for rendering.
     * Implementations should define how the dominion is removed.
     *
     * @param worldName    the name of the world where the dominion is located
     * @param dominionName the name of the dominion to remove
     */
    protected abstract void removeDominionFromSet(@NotNull String worldName, @NotNull String dominionName);

    /**
     * Removes a DominionDTO from all registered web map renderers.
     *
     * @param worldName    the name of the world where the dominion is located
     * @param dominionName the name of the dominion to remove
     */
    public static void removeDominion(@NotNull String worldName, @NotNull String dominionName) {
        for (WebMapRender mapRender : webMapInstances) {
            mapRender.removeDominionFromSet(worldName, dominionName);
        }
    }

    protected abstract void renderMCA(@NotNull Map<String, List<String>> mcaFiles);
}
