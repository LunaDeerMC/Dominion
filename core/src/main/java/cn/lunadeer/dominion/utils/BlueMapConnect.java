package cn.lunadeer.dominion.utils;

import cn.lunadeer.dominion.Cache;
import cn.lunadeer.dominion.dtos.DominionDTO;
import cn.lunadeer.minecraftpluginutils.Scheduler;
import cn.lunadeer.minecraftpluginutils.XLogger;
import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.*;

public class BlueMapConnect {
    public static void render() {
        Scheduler.runTaskAsync(() -> {
            try {
                BlueMapAPI.getInstance().ifPresent(api -> {
                    Map<String, List<DominionDTO>> world_dominions = new HashMap<>();
                    for (DominionDTO dominion : Cache.instance.getDominions()) {
                        if (dominion.getWorld() == null) {
                            continue;
                        }
                        if (!world_dominions.containsKey(dominion.getWorld().getName())) {
                            world_dominions.put(dominion.getWorld().getName(), new ArrayList<>());
                        }
                        world_dominions.get(dominion.getWorld().getName()).add(dominion);
                    }
                    for (Map.Entry<String, List<DominionDTO>> d : world_dominions.entrySet()) {
                        api.getWorld(d.getKey()).ifPresent(world -> {
                            MarkerSet markerSet = MarkerSet.builder()
                                    .label("Dominion")
                                    .build();

                            for (DominionDTO dominion : d.getValue()) {
                                Collection<Vector2d> vectors = new ArrayList<>();
                                vectors.add(new Vector2d(dominion.getX1() + 0.001, dominion.getZ1() + 0.001));
                                vectors.add(new Vector2d(dominion.getX2() - 0.001, dominion.getZ1() + 0.001));
                                vectors.add(new Vector2d(dominion.getX2() - 0.001, dominion.getZ2() - 0.001));
                                vectors.add(new Vector2d(dominion.getX1() + 0.001, dominion.getZ2() - 0.001));
                                Shape shape = new Shape(vectors);
                                double x = vectors.iterator().next().getX();
                                double z = vectors.iterator().next().getY();
                                double y = dominion.getY1();

                                int r = dominion.getColorR();
                                int g = dominion.getColorG();
                                int b = dominion.getColorB();

                                Color line = new Color(r, g, b, 0.8F);
                                Color fill = new Color(r, g, b, 0.2F);
                                ExtrudeMarker marker = ExtrudeMarker.builder()
                                        .label(dominion.getName())
                                        .position(x, y, z)
                                        .shape(shape, dominion.getY1() + 0.001f, dominion.getY2() - 0.001f)
                                        .lineColor(line)
                                        .fillColor(fill)
                                        .build();
                                markerSet.getMarkers()
                                        .put(dominion.getName(), marker);
                            }

                            for (BlueMapMap map : world.getMaps()) {
                                map.getMarkerSets().put(d.getKey() + "-" + markerSet.getLabel(), markerSet);
                            }
                        });
                    }
                });
            } catch (NoClassDefFoundError e) {
                XLogger.warn("无法连接 BlueMap 插件，如果你不打算使用卫星地图渲染建议前往配置文件关闭此功能以避免下方的报错。");
                XLogger.err(e.getMessage());
            }
        });
    }

    public static void renderMCA(Map<String, List<String>> mca_files) {
        Scheduler.runTaskAsync(() -> {
            try {
                BlueMapAPI.getInstance().ifPresent(api -> {
                    for (String world : mca_files.keySet()) {
                        api.getWorld(world).ifPresent(bmWorld -> {
                            MarkerSet markerSet = MarkerSet.builder()
                                    .label("MCA")
                                    .defaultHidden(true)
                                    .build();
                            for (String file : mca_files.get(world)) {
                                // r.-1.-1.mca
                                int mca_x = Integer.parseInt(file.split("\\.")[1]);
                                int mca_z = Integer.parseInt(file.split("\\.")[2]);
                                int world_x1 = mca_x * 512;
                                int world_x2 = (mca_x + 1) * 512;
                                int world_z1 = mca_z * 512;
                                int world_z2 = (mca_z + 1) * 512;
                                Collection<Vector2d> vectors = new ArrayList<>();
                                vectors.add(new Vector2d(world_x1 + 0.001, world_z1 + 0.001));
                                vectors.add(new Vector2d(world_x2 - 0.001, world_z1 + 0.001));
                                vectors.add(new Vector2d(world_x2 - 0.001, world_z2 - 0.001));
                                vectors.add(new Vector2d(world_x1 + 0.001, world_z2 - 0.001));
                                Shape shape = new Shape(vectors);
                                double x = vectors.iterator().next().getX();
                                double z = vectors.iterator().next().getY();
                                double y = -64;

                                Color line = new Color(0, 204, 0, 0.8F);
                                Color fill = new Color(0, 204, 0, 0.2F);
                                ExtrudeMarker marker = ExtrudeMarker.builder()
                                        .label(file)
                                        .position(x, y, z)
                                        .shape(shape, -64, 320)
                                        .lineColor(line)
                                        .fillColor(fill)
                                        .build();
                                markerSet.getMarkers()
                                        .put(file, marker);
                            }
                            for (BlueMapMap map : bmWorld.getMaps()) {
                                map.getMarkerSets().put(world + "-" + markerSet.getLabel(), markerSet);
                            }
                        });
                    }
                });
            } catch (NoClassDefFoundError e) {
                XLogger.warn("无法连接 BlueMap 插件，如果你不打算使用卫星地图渲染建议前往配置文件关闭此功能以避免下方的报错。");
                XLogger.err(e.getMessage());
            }
        });
    }
}
