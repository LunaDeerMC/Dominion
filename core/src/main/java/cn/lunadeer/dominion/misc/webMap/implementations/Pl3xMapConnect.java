package cn.lunadeer.dominion.misc.webMap.implementations;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.misc.webMap.WebMapRender;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.event.EventHandler;
import net.pl3x.map.core.event.EventListener;
import net.pl3x.map.core.event.server.Pl3xMapEnabledEvent;
import net.pl3x.map.core.event.world.WorldLoadedEvent;
import net.pl3x.map.core.event.world.WorldUnloadedEvent;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.markers.marker.Rectangle;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.markers.option.Popup;
import net.pl3x.map.core.markers.option.Tooltip;
import net.pl3x.map.core.world.World;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Pl3xMapConnect extends WebMapRender implements EventListener {

    private static final Logger LOGGER = Logger.getLogger(Pl3xMapConnect.class.getName());
    private static final String DOMINION_LAYER = "dominion_markers";
    private static final String MCA_LAYER = "mca_markers";
    private static final int MCA_CHUNK_SIZE = 512;

    private final ConcurrentLinkedQueue<DominionDTO> dominions = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MCARegion> mcaRegions = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, Options> dominionOptionsCache = new ConcurrentHashMap<>();
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    public Pl3xMapConnect() {
        WebMapRender.webMapInstances.add(this);
        initializePl3xMapConnection();
    }

    private void initializePl3xMapConnection() {
        try {
            Pl3xMap.api().getEventRegistry().register(this);
            if (Pl3xMap.api().isEnabled()) {
                onPl3xMapEnabled(new Pl3xMapEnabledEvent());
            }
            isInitialized.set(true);
            LOGGER.info("Pl3xMap connection initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Pl3xMap connection", e);
            isInitialized.set(false);
        }
    }

    @Override
    protected void renderWorldInSet(org.bukkit.@NotNull World world) {
        if (!isInitialized.get()) {
            LOGGER.warning("Pl3xMap not initialized, skipping dominion rendering");
            return;
        }

        Scheduler.runTaskAsync(() -> {
            try {
                // Filter and add valid dominions
                List<DominionDTO> validDominions = CacheManager.instance.getCache().getDominionCache().getDominionsByWorld(world);

                this.dominions.addAll(validDominions);

                // Pre-cache options for better performance
                validDominions.forEach(dominion -> {
                    Options options = getDominionOptions(dominion);
                    dominionOptionsCache.put(dominion.getName(), options);
                });

                updateLayers();
                LOGGER.info("Updated " + validDominions.size() + " dominion markers on Pl3xMap");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error rendering dominions on Pl3xMap", e);
            }
        });
    }

    @Override
    protected void updateDominionInSet(@NotNull DominionDTO dominion) {
        if (!isInitialized.get()) {
            LOGGER.warning("Pl3xMap not initialized, skipping dominion update");
            return;
        }

        this.dominions.removeIf(d -> d.getName().equals(dominion.getName()) && Objects.equals(d.getWorld(), dominion.getWorld()));
        if (dominion.getWorld() != null) {
            this.dominions.add(dominion);
            dominionOptionsCache.put(dominion.getName(), getDominionOptions(dominion));
        } else {
            dominionOptionsCache.remove(dominion.getName());
        }
        updateLayers();
    }

    @Override
    protected void removeDominionFromSet(@NotNull String worldName, @NotNull String dominionName) {
        if (!isInitialized.get()) {
            LOGGER.warning("Pl3xMap not initialized, skipping dominion removal");
            return;
        }

        this.dominions.removeIf(d -> d.getName().equals(dominionName) && d.getWorld() != null && d.getWorld().getName().equals(worldName));
        dominionOptionsCache.remove(dominionName);
        updateLayers();
    }

    @Override
    protected void renderMCA(@NotNull Map<String, List<String>> mcaFiles) {
        if (!isInitialized.get()) {
            LOGGER.warning("Pl3xMap not initialized, skipping MCA rendering");
            return;
        }

        Scheduler.runTaskAsync(() -> {
            try {
                this.mcaRegions.clear();

                mcaFiles.entrySet().parallelStream()
                        .filter(entry -> Bukkit.getWorld(entry.getKey()) != null)
                        .forEach(entry -> {
                            String worldName = entry.getKey();
                            entry.getValue().parallelStream()
                                    .map(fileName -> parseMCAFile(worldName, fileName))
                                    .filter(Objects::nonNull)
                                    .forEach(mcaRegions::add);
                        });

                updateLayers();
                LOGGER.info("Updated " + mcaRegions.size() + " MCA markers on Pl3xMap");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error rendering MCA files on Pl3xMap", e);
            }
        });
    }

    private MCARegion parseMCAFile(@NotNull String worldName, @NotNull String fileName) {
        try {
            String[] cords = fileName.split("\\.");
            if (cords.length < 3) {
                return null; // Silent fail for invalid format
            }

            int chunkX = Integer.parseInt(cords[1]);
            int chunkZ = Integer.parseInt(cords[2]);

            int x1 = chunkX * MCA_CHUNK_SIZE;
            int z1 = chunkZ * MCA_CHUNK_SIZE;
            int x2 = x1 + MCA_CHUNK_SIZE;
            int z2 = z1 + MCA_CHUNK_SIZE;

            return new MCARegion(worldName, fileName, x1, z1, x2, z2);
        } catch (NumberFormatException e) {
            // Log only in debug mode to avoid spam
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Failed to parse MCA coordinates from file: " + fileName, e);
            }
            return null;
        }
    }

    private void updateLayers() {
        if (!isInitialized.get()) return;

        try {
            Pl3xMap.api().getWorldRegistry().forEach(this::registerLayers);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update layers", e);
        }
    }

    private void registerLayers(@NotNull World world) {
        try {
            // Register dominion layer
            DominionLayer dominionLayer = new DominionLayer(this, world);
            world.getLayerRegistry().unregister(DOMINION_LAYER);
            world.getLayerRegistry().register(dominionLayer);

            // Register MCA layer
            MCALayer mcaLayer = new MCALayer(this, world);
            world.getLayerRegistry().unregister(MCA_LAYER);
            world.getLayerRegistry().register(mcaLayer);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to register layers for world: " + world.getName(), e);
        }
    }

    @EventHandler
    public void onPl3xMapEnabled(@NotNull Pl3xMapEnabledEvent event) {
        LOGGER.info("Pl3xMap enabled, registering layers for all worlds");
        updateLayers();
    }

    @EventHandler
    public void onWorldLoaded(@NotNull WorldLoadedEvent event) {
        registerLayers(event.getWorld());
    }

    @EventHandler
    public void onWorldUnloaded(@NotNull WorldUnloadedEvent event) {
        try {
            event.getWorld().getLayerRegistry().unregister(DOMINION_LAYER);
            event.getWorld().getLayerRegistry().unregister(MCA_LAYER);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to unregister layers for world: " + event.getWorld().getName(), e);
        }
    }

    @NotNull
    private Options getDominionOptions(@NotNull DominionDTO dominion) {
        PlayerDTO owner = CacheManager.instance.getPlayer(dominion.getOwner());
        String ownerName = owner != null ? owner.getLastKnownName() : "Unknown";

        String tooltip = dominion.getName();
        String popup = String.format("<div><strong>%s</strong></div><div>Owner: %s</div><div>Size: %dx%dx%d</div>",
                dominion.getName(),
                ownerName,
                Math.abs(dominion.getCuboid().x2() - dominion.getCuboid().x1()),
                Math.abs(dominion.getCuboid().y2() - dominion.getCuboid().y1()),
                Math.abs(dominion.getCuboid().z2() - dominion.getCuboid().z1()));

        return Options.builder()
                .tooltip(new Tooltip(tooltip))
                .popup(new Popup(popup))
                .fillColor(new Color(dominion.getColorR(), dominion.getColorG(), dominion.getColorB(), (int) (255 * 0.2)).getRGB())
                .strokeColor(new Color(dominion.getColorR(), dominion.getColorG(), dominion.getColorB(), (int) (255 * 0.8)).getRGB())
                .strokeWeight(2)
                .build();
    }

    @NotNull
    private Options getMCAOptions(@NotNull MCARegion region) {
        return Options.builder()
                .tooltip(new Tooltip(region.fileName))
                .popup(new Popup("<div><strong>MCA Region</strong></div><div>" + region.fileName + "</div>"))
                .fillColor(new Color(0, 204, 0, (int) (255 * 0.2)).getRGB())
                .strokeColor(new Color(0, 204, 0, (int) (255 * 0.8)).getRGB())
                .strokeWeight(1)
                .build();
    }

    public static class DominionLayer extends SimpleLayer {

        private final Pl3xMapConnect connect;
        private final World mapWorld;

        public DominionLayer(@NotNull Pl3xMapConnect connect, @NotNull World mapWorld) {
            super(DOMINION_LAYER, () -> "Dominion");
            this.connect = connect;
            this.mapWorld = mapWorld;
        }

        @Override
        @NotNull
        public Collection<Marker<?>> getMarkers() {
            return connect.dominions.stream()
                    .filter(dominion -> dominion.getWorld().getName().equals(mapWorld.getName()))
                    .map(dominion -> Rectangle.of(
                            dominion.getName(),
                            Point.of(dominion.getCuboid().x1(), dominion.getCuboid().z1()),
                            Point.of(dominion.getCuboid().x2(), dominion.getCuboid().z2())
                    ).setOptions(connect.getDominionOptions(dominion)))
                    .collect(Collectors.toList());
        }
    }

    public static class MCALayer extends SimpleLayer {

        private final Pl3xMapConnect connect;
        private final World mapWorld;

        public MCALayer(@NotNull Pl3xMapConnect connect, @NotNull World mapWorld) {
            super(MCA_LAYER, () -> "MCA");
            this.connect = connect;
            this.mapWorld = mapWorld;
        }

        @Override
        @NotNull
        public Collection<Marker<?>> getMarkers() {
            return connect.mcaRegions.stream()
                    .filter(region -> region.worldName.equals(mapWorld.getName()))
                    .map(region -> Rectangle.of(
                            "mca_" + region.fileName,
                            Point.of(region.x1, region.z1),
                            Point.of(region.x2, region.z2)
                    ).setOptions(connect.getMCAOptions(region)))
                    .collect(Collectors.toList());
        }
    }

    private record MCARegion(String worldName, String fileName, int x1, int z1, int x2, int z2) {
    }
}
