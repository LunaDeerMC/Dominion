package cn.lunadeer.dominion.utils.webMap;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static cn.lunadeer.dominion.utils.Misc.formatString;

public class BlueMapConnect extends WebMapRender {

    // Constants for better maintainability
    private static final String DOMINION_LABEL = "Dominion";
    private static final String MCA_LABEL = "MCA";
    private static final double SHAPE_OFFSET = 0.001;
    private static final int MCA_CHUNK_SIZE = 512;
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;
    private static final Color MCA_LINE_COLOR = new Color(0, 204, 0, 0.8F);
    private static final Color MCA_FILL_COLOR = new Color(0, 204, 0, 0.2F);

    public static class BlueMapConnectText extends ConfigurationPart {
        public String registerFail = "Failed to register BlueMap API.";
        public String infoLabel = "<div>{0}</div><div>Owner: {1}</div>";
    }

    public BlueMapConnect() {
        WebMapRender.webMapInstances.add(this);
    }

    @Override
    protected void renderDominions(@NotNull List<DominionDTO> dominions) {
        executeBlueMapOperation(() -> {
            Map<String, List<DominionDTO>> worldDominions = groupDominionsByWorld(dominions);

            for (Map.Entry<String, List<DominionDTO>> entry : worldDominions.entrySet()) {
                renderDominionsForWorld(entry.getKey(), entry.getValue());
            }
        });
    }

    @Override
    protected void renderMCA(@NotNull Map<String, List<String>> mcaFiles) {
        executeBlueMapOperation(() -> {
            for (Map.Entry<String, List<String>> entry : mcaFiles.entrySet()) {
                renderMCAForWorld(entry.getKey(), entry.getValue());
            }
        });
    }

    /**
     * Common method to execute BlueMap operations with error handling
     */
    private void executeBlueMapOperation(Runnable operation) {
        Scheduler.runTaskAsync(() -> {
            try {
                BlueMapAPI.getInstance().ifPresent(api -> operation.run());
            } catch (NoClassDefFoundError e) {
                XLogger.warn(Language.blueMapConnectText.registerFail);
                XLogger.error(e);
            }
        });
    }

    /**
     * Groups dominions by world name for efficient processing
     */
    private Map<String, List<DominionDTO>> groupDominionsByWorld(@NotNull List<DominionDTO> dominions) {
        Map<String, List<DominionDTO>> worldDominions = new HashMap<>();

        for (DominionDTO dominion : dominions) {
            if (dominion.getWorld() == null) {
                continue;
            }

            String worldName = dominion.getWorld().getName();
            worldDominions.computeIfAbsent(worldName, k -> new ArrayList<>()).add(dominion);
        }

        return worldDominions;
    }

    /**
     * Renders dominions for a specific world
     */
    private void renderDominionsForWorld(String worldName, List<DominionDTO> dominions) {
        BlueMapAPI.getInstance().flatMap(api -> api.getWorld(worldName)).ifPresent(world -> {
            MarkerSet markerSet = MarkerSet.builder()
                    .label(DOMINION_LABEL)
                    .build();

            for (DominionDTO dominion : dominions) {
                createDominionMarker(dominion, markerSet);
            }

            addMarkerSetToMaps(world.getMaps(), worldName + "-" + markerSet.getLabel(), markerSet);
        });
    }

    /**
     * Creates a marker for a single dominion
     */
    private void createDominionMarker(DominionDTO dominion, MarkerSet markerSet) {
        PlayerDTO player = CacheManager.instance.getPlayer(dominion.getOwner());
        if (player == null) {
            return;
        }

        Shape shape = createRectangularShape(
                dominion.getCuboid().x1(), dominion.getCuboid().z1(),
                dominion.getCuboid().x2(), dominion.getCuboid().z2()
        );

        Color lineColor = new Color(dominion.getColorR(), dominion.getColorG(), dominion.getColorB(), 0.8F);
        Color fillColor = new Color(dominion.getColorR(), dominion.getColorG(), dominion.getColorB(), 0.2F);

        ExtrudeMarker marker = ExtrudeMarker.builder()
                .label(dominion.getName())
                .detail(formatString(Language.blueMapConnectText.infoLabel, dominion.getName(), player.getLastKnownName()))
                .position(dominion.getCuboid().x1() + SHAPE_OFFSET, dominion.getCuboid().y1(), dominion.getCuboid().z1() + SHAPE_OFFSET)
                .shape(shape, dominion.getCuboid().y1() + (float) SHAPE_OFFSET, dominion.getCuboid().y2() - (float) SHAPE_OFFSET)
                .lineColor(lineColor)
                .fillColor(fillColor)
                .build();

        markerSet.getMarkers().put(dominion.getName(), marker);
    }

    /**
     * Renders MCA files for a specific world
     */
    private void renderMCAForWorld(String worldName, List<String> mcaFiles) {
        BlueMapAPI.getInstance().flatMap(api -> api.getWorld(worldName)).ifPresent(bmWorld -> {
            MarkerSet markerSet = MarkerSet.builder()
                    .label(MCA_LABEL)
                    .defaultHidden(true)
                    .build();

            for (String file : mcaFiles) {
                createMCAMarker(file, markerSet);
            }

            addMarkerSetToMaps(bmWorld.getMaps(), worldName + "-" + markerSet.getLabel(), markerSet);
        });
    }

    /**
     * Creates a marker for a single MCA file
     */
    private void createMCAMarker(String fileName, MarkerSet markerSet) {
        try {
            String[] parts = fileName.split("\\.");
            if (parts.length < 3) {
                XLogger.warn("Invalid MCA filename format: " + fileName);
                return;
            }

            int mcaX = Integer.parseInt(parts[1]);
            int mcaZ = Integer.parseInt(parts[2]);

            int worldX1 = mcaX * MCA_CHUNK_SIZE;
            int worldX2 = worldX1 + MCA_CHUNK_SIZE;
            int worldZ1 = mcaZ * MCA_CHUNK_SIZE;
            int worldZ2 = worldZ1 + MCA_CHUNK_SIZE;

            Shape shape = createRectangularShape(worldX1, worldZ1, worldX2, worldZ2);

            ExtrudeMarker marker = ExtrudeMarker.builder()
                    .label(fileName)
                    .position(worldX1 + SHAPE_OFFSET, MIN_Y, worldZ1 + SHAPE_OFFSET)
                    .shape(shape, MIN_Y, MAX_Y)
                    .lineColor(MCA_LINE_COLOR)
                    .fillColor(MCA_FILL_COLOR)
                    .build();

            markerSet.getMarkers().put(fileName, marker);
        } catch (NumberFormatException e) {
            XLogger.warn("Failed to parse MCA coordinates from filename: " + fileName);
            XLogger.error(e);
        }
    }

    /**
     * Creates a rectangular shape with the given boundaries
     */
    private Shape createRectangularShape(double x1, double z1, double x2, double z2) {
        Collection<Vector2d> vectors = Arrays.asList(
                new Vector2d(x1 + SHAPE_OFFSET, z1 + SHAPE_OFFSET),
                new Vector2d(x2 - SHAPE_OFFSET, z1 + SHAPE_OFFSET),
                new Vector2d(x2 - SHAPE_OFFSET, z2 - SHAPE_OFFSET),
                new Vector2d(x1 + SHAPE_OFFSET, z2 - SHAPE_OFFSET)
        );
        return new Shape(vectors);
    }

    /**
     * Adds a marker set to all maps in the collection
     */
    private void addMarkerSetToMaps(Collection<BlueMapMap> maps, String key, MarkerSet markerSet) {
        for (BlueMapMap map : maps) {
            map.getMarkerSets().put(key, markerSet);
        }
    }
}
