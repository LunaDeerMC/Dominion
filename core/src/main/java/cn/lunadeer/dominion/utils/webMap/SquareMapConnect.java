package cn.lunadeer.dominion.utils.webMap;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import xyz.jpenilla.squaremap.api.*;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SquareMapConnect extends WebMapRender {

    private static final Logger LOGGER = Logger.getLogger(SquareMapConnect.class.getName());
    private final Squaremap api;

    private static final Key MCA_KEY = Key.of("mca_layer_key");
    private static final Key DOMINION_KEY = Key.of("dominion_layer_key");

    // Pre-create MarkerOptions to avoid repeated object creation
    private static final MarkerOptions MCA_MARKER_OPTIONS = MarkerOptions.builder()
            .fillColor(new Color(0, 204, 0)).fillOpacity(0.2)
            .strokeColor(new Color(0, 204, 0)).strokeOpacity(0.8)
            .build();

    // Constants for MCA calculations
    private static final int MCA_CHUNK_SIZE = 512;

    public SquareMapConnect() {
        WebMapRender.webMapInstances.add(this);
        api = SquaremapProvider.get();
        initializeLayers();
    }

    private void initializeLayers() {
        Bukkit.getWorlds().forEach(world -> {
            try {
                api.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world)).ifPresent(mapWorld -> {
                    // Initialize Dominion layer
                    if (mapWorld.layerRegistry().get(DOMINION_KEY) == null) {
                        SimpleLayerProvider dominionProvider = SimpleLayerProvider.builder("Dominion")
                                .showControls(true)
                                .build();
                        mapWorld.layerRegistry().register(DOMINION_KEY, dominionProvider);
                    }

                    // Initialize MCA layer
                    if (mapWorld.layerRegistry().get(MCA_KEY) == null) {
                        SimpleLayerProvider mcaProvider = SimpleLayerProvider.builder("MCA")
                                .showControls(true)
                                .build();
                        mapWorld.layerRegistry().register(MCA_KEY, mcaProvider);
                    }
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to initialize layers for world: " + world.getName(), e);
            }
        });
    }

    @Override
    protected void renderDominions(@NotNull List<DominionDTO> dominions) {
        if (dominions.isEmpty()) {
            return; // Early return if no dominions to render
        }

        Scheduler.runTaskAsync(() -> {
            try {
                Map<World, List<DominionDTO>> dominionMap = dominions.stream()
                        .filter(dominion -> dominion.getWorld() != null)
                        .collect(Collectors.groupingBy(DominionDTO::getWorld));

                dominionMap.forEach(this::renderDominionsForWorld);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error rendering dominions", e);
            }
        });
    }

    private void renderDominionsForWorld(World world, List<DominionDTO> dominionList) {
        api.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world)).ifPresent(mapWorld -> {
            SimpleLayerProvider dominionProvider = (SimpleLayerProvider) mapWorld.layerRegistry().get(DOMINION_KEY);
            if (dominionProvider == null) {
                LOGGER.warning("Dominion provider not found for world: " + world.getName());
                return;
            }

            // Batch process dominions
            dominionList.forEach(dominion -> {
                try {
                    renderSingleDominion(dominionProvider, dominion);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to render dominion: " + dominion.getId(), e);
                }
            });

            // Only re-register once after all markers are updated
            mapWorld.layerRegistry().unregister(DOMINION_KEY);
            mapWorld.layerRegistry().register(DOMINION_KEY, dominionProvider);
        });
    }

    private void renderSingleDominion(SimpleLayerProvider provider, DominionDTO dominion) {
        Point p1 = Point.of(dominion.getCuboid().x1(), dominion.getCuboid().z1());
        Point p2 = Point.of(dominion.getCuboid().x2(), dominion.getCuboid().z2());

        Color dominionColor = new Color(dominion.getColorR(), dominion.getColorG(), dominion.getColorB());
        MarkerOptions options = MarkerOptions.builder()
                .fillColor(dominionColor).fillOpacity(0.2)
                .strokeColor(dominionColor).strokeOpacity(0.8)
                .build();

        Marker marker = Marker.rectangle(p1, p2).markerOptions(options);
        Key key = Key.of("dominion_" + dominion.getId());

        provider.removeMarker(key);
        provider.addMarker(key, marker);
    }

    @Override
    protected void renderMCA(@NotNull Map<String, List<String>> mcaFiles) {
        if (mcaFiles.isEmpty()) {
            return; // Early return if no MCA files to render
        }

        Scheduler.runTaskAsync(() -> {
            try {
                mcaFiles.forEach(this::renderMCAForWorld);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error rendering MCA files", e);
            }
        });
    }

    private void renderMCAForWorld(String worldName, List<String> files) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            LOGGER.warning("World not found: " + worldName);
            return;
        }

        api.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world)).ifPresent(mapWorld -> {
            SimpleLayerProvider mcaProvider = (SimpleLayerProvider) mapWorld.layerRegistry().get(MCA_KEY);
            if (mcaProvider == null) {
                LOGGER.warning("MCA provider not found for world: " + worldName);
                return;
            }

            // Batch process MCA files
            files.forEach(file -> {
                try {
                    renderSingleMCAFile(mcaProvider, worldName, file);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to render MCA file: " + file, e);
                }
            });

            // Only re-register once after all markers are updated
            mapWorld.layerRegistry().unregister(MCA_KEY);
            mapWorld.layerRegistry().register(MCA_KEY, mcaProvider);
        });
    }

    private void renderSingleMCAFile(SimpleLayerProvider provider, String worldName, String file) {
        String[] coords = file.split("\\.");
        if (coords.length < 3) {
            LOGGER.warning("Invalid MCA file format: " + file);
            return;
        }

        try {
            int chunkX = Integer.parseInt(coords[1]);
            int chunkZ = Integer.parseInt(coords[2]);

            int worldX1 = chunkX * MCA_CHUNK_SIZE;
            int worldZ1 = chunkZ * MCA_CHUNK_SIZE;
            int worldX2 = (chunkX + 1) * MCA_CHUNK_SIZE;
            int worldZ2 = (chunkZ + 1) * MCA_CHUNK_SIZE;

            Point p1 = Point.of(worldX1, worldZ1);
            Point p2 = Point.of(worldX2, worldZ2);

            Marker marker = Marker.rectangle(p1, p2).markerOptions(MCA_MARKER_OPTIONS);
            Key key = Key.of("mca_" + worldName + "_" + chunkX + "_" + chunkZ);

            provider.removeMarker(key);
            provider.addMarker(key, marker);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid coordinates in MCA file: " + file, e);
        }
    }
}
