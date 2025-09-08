package cn.lunadeer.dominion.utils.webMap;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
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

        Scheduler.runTaskRepeatAsync(() -> {
            for (WebMapRender mapRender : webMapInstances) {
                mapRender.renderDominions(CacheManager.instance.getCache().getDominionCache().getAllDominions());
            }
        }, Configuration.webMapRenderer.refreshIntervalSeconds * 20L, Configuration.webMapRenderer.refreshIntervalSeconds * 20L);
    }

    public static void renderAllMCA(@NotNull Map<String, List<String>> mcaFiles) {
        Scheduler.runTaskAsync(() -> {
            for (WebMapRender mapRender : webMapInstances) {
                mapRender.renderMCA(mcaFiles);
            }
        });
    }

    protected static List<WebMapRender> webMapInstances = new ArrayList<>();

    protected abstract void renderDominions(@NotNull List<DominionDTO> dominions);

    protected abstract void renderMCA(@NotNull Map<String, List<String>> mcaFiles);
}
