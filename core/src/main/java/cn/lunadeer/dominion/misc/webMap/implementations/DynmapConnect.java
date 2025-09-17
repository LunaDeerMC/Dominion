package cn.lunadeer.dominion.misc.webMap.implementations;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.misc.webMap.WebMapRender;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.World;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static cn.lunadeer.dominion.utils.Misc.formatString;

public class DynmapConnect extends WebMapRender {

    // Constants for marker sets and MCA chunk size
    private static final String DOMINION_MARKER_SET_ID = "dominion";
    private static final String DOMINION_MARKER_SET_LABEL = "Dominion";
    private static final String MCA_MARKER_SET_ID = "mca";
    private static final String MCA_MARKER_SET_LABEL = "MCA";
    private static final int MCA_CHUNK_SIZE = 512;
    private static final int MCA_COLOR = 0x00CC00;
    private static final double DEFAULT_FILL_OPACITY = 0.2;
    private static final double DEFAULT_LINE_OPACITY = 0.8;
    private static final int DEFAULT_LINE_WEIGHT = 1;

    public static class DynmapConnectText extends ConfigurationPart {
        public String registerSuccess = "Register to dynmap success!";
        public String registerFail = "Register to dynmap failed!";
        public String infoLabel = "<div>{0}</div><div>Owner: {1}</div>";
    }

    public static class DynmapConnectListener extends DynmapCommonAPIListener {
        private MarkerSet dominionMarkerSet;
        private MarkerSet mcaMarkerSet;

        public DynmapConnectListener() {
            DynmapCommonAPIListener.register(this);
        }

        @Override
        public void apiEnabled(DynmapCommonAPI dynmapCommonAPI) {
            try {
                MarkerAPI markerAPI = dynmapCommonAPI.getMarkerAPI();
                initializeMarkerSets(markerAPI);
                XLogger.info(Language.dynmapConnectText.registerSuccess);
            } catch (Exception e) {
                XLogger.error("Failed to initialize Dynmap API: " + e.getMessage(), e);
            }
        }

        private void initializeMarkerSets(@NotNull MarkerAPI markerAPI) {
            this.dominionMarkerSet = getOrCreateMarkerSet(markerAPI, DOMINION_MARKER_SET_ID, DOMINION_MARKER_SET_LABEL);
            this.mcaMarkerSet = getOrCreateMarkerSet(markerAPI, MCA_MARKER_SET_ID, MCA_MARKER_SET_LABEL);
        }

        @Nullable
        private MarkerSet getOrCreateMarkerSet(@NotNull MarkerAPI markerAPI, @NotNull String id, @NotNull String label) {
            MarkerSet markerSet = markerAPI.getMarkerSet(id);
            if (markerSet == null) {
                markerSet = markerAPI.createMarkerSet(id, label, null, false);
            }
            return markerSet;
        }

        private void removeDominionMarker(@NotNull String dominionName) {
            if (this.dominionMarkerSet == null) {
                XLogger.warn(Language.dynmapConnectText.registerFail);
                return;
            }
            Marker marker = this.dominionMarkerSet.findMarker(dominionName);
            if (marker != null) {
                marker.deleteMarker();
                XLogger.debug("Removed dominion marker: " + dominionName);
            }
        }

        private void setDominionMarker(@NotNull DominionDTO dominion) {
            PlayerDTO owner = CacheManager.instance.getPlayer(dominion.getOwner());
            if (owner == null || dominion.getWorld() == null) {
                XLogger.debug("Skipping dominion marker for " + dominion.getName() +
                        " - missing owner or world information");
                return;
            }

            try {
                String nameLabel = formatString(Language.dynmapConnectText.infoLabel,
                        dominion.getName(), owner.getLastKnownName());
                double[] xCoordinates = {dominion.getCuboid().x1(), dominion.getCuboid().x2()};
                double[] zCoordinates = {dominion.getCuboid().z1(), dominion.getCuboid().z2()};

                AreaMarker marker = this.dominionMarkerSet.createAreaMarker(
                        dominion.getName(),
                        nameLabel,
                        true,
                        dominion.getWorld().getName(),
                        xCoordinates,
                        zCoordinates,
                        false
                );

                // Apply styling
                int color = dominion.getColorHex();
                marker.setFillStyle(DEFAULT_FILL_OPACITY, color);
                marker.setLineStyle(DEFAULT_LINE_WEIGHT, DEFAULT_LINE_OPACITY, color);

                XLogger.debug("Added dominion marker: " + dominion.getName());
            } catch (Exception e) {
                XLogger.error("Failed to create dominion marker for " + dominion.getName() + ": " + e.getMessage());
            }
        }

        public void setMCAMarkers(@NotNull Map<String, List<String>> mcaFiles) {
            Scheduler.runTaskAsync(() -> {
                if (this.mcaMarkerSet == null) {
                    XLogger.warn(Language.dynmapConnectText.registerFail);
                    return;
                }

                // Clear existing markers
                clearAreaMarkers(this.mcaMarkerSet);

                // Create new markers
                int markerCount = 0;
                for (Map.Entry<String, List<String>> entry : mcaFiles.entrySet()) {
                    String worldName = entry.getKey();
                    for (String fileName : entry.getValue()) {
                        if (createMCAMarker(worldName, fileName)) {
                            markerCount++;
                        }
                    }
                }
                XLogger.debug("Updated " + markerCount + " MCA markers");
            });
        }

        private boolean createMCAMarker(@NotNull String worldName, @NotNull String fileName) {
            try {
                String[] coordinates = fileName.split("\\.");
                if (coordinates.length < 3) {
                    XLogger.warn("Invalid MCA file name format: " + fileName);
                    return false;
                }

                int chunkX = Integer.parseInt(coordinates[1]);
                int chunkZ = Integer.parseInt(coordinates[2]);

                int worldX1 = chunkX * MCA_CHUNK_SIZE;
                int worldX2 = (chunkX + 1) * MCA_CHUNK_SIZE;
                int worldZ1 = chunkZ * MCA_CHUNK_SIZE;
                int worldZ2 = (chunkZ + 1) * MCA_CHUNK_SIZE;

                String nameLabel = "<div>" + fileName + "</div>";
                double[] xCoordinates = {worldX1, worldX2};
                double[] zCoordinates = {worldZ1, worldZ2};

                AreaMarker marker = this.mcaMarkerSet.createAreaMarker(
                        fileName,
                        nameLabel,
                        true,
                        worldName,
                        xCoordinates,
                        zCoordinates,
                        false
                );

                marker.setFillStyle(DEFAULT_FILL_OPACITY, MCA_COLOR);
                marker.setLineStyle(DEFAULT_LINE_WEIGHT, DEFAULT_LINE_OPACITY, MCA_COLOR);
                return true;

            } catch (NumberFormatException e) {
                XLogger.warn("Failed to parse MCA coordinates from file: " + fileName);
                return false;
            } catch (Exception e) {
                XLogger.error("Failed to create MCA marker for " + fileName + ": " + e.getMessage());
                return false;
            }
        }

        private void clearAreaMarkers(@NotNull MarkerSet markerSet) {
            try {
                markerSet.getAreaMarkers().forEach(AreaMarker::deleteMarker);
            } catch (Exception e) {
                XLogger.error("Failed to clear area markers: " + e.getMessage());
            }
        }
    }

    private final DynmapConnectListener listener;

    public DynmapConnect() {
        WebMapRender.webMapInstances.add(this);
        this.listener = new DynmapConnectListener();
    }

    @Override
    protected void renderWorldInSet(@NotNull World world) {
        CacheManager.instance.getCache().getDominionCache().getDominionsByWorld(world)
                .forEach(listener::setDominionMarker);
    }

    @Override
    protected void updateDominionInSet(@NotNull DominionDTO dominion) {
        listener.setDominionMarker(dominion);
    }

    @Override
    protected void removeDominionFromSet(@NotNull String worldName, @NotNull String dominionName) {
        listener.removeDominionMarker(dominionName);
    }

    @Override
    protected void renderMCA(@NotNull Map<String, List<String>> mcaFiles) {
        listener.setMCAMarkers(mcaFiles);
    }
}
