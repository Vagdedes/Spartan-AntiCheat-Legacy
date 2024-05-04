package com.vagdedes.spartan.utils.gameplay;

import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.functionality.management.Cache;
import com.vagdedes.spartan.functionality.server.Chunks;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;

import java.util.*;

public class PatternUtils {

    private static final Map<Integer, Map<Integer, Boolean>> memory
            = Cache.store(Collections.synchronizedMap(new LinkedHashMap<>()));

    private static final int
            waterLogHash = "water-log".hashCode(),
            liquidHash = "liquid".hashCode(),
            waterHash = "water".hashCode(),
            lavaHash = "lava".hashCode(),
            nonWaterLoggedLiquidHash = "non-water-logged-liquid".hashCode();

    // Separator

    public static void synchronizeClearance(SpartanBlock block) {
        synchronized (memory) {
            memory.remove(block.identifier);
        }
    }

    // Separator

    private static <E> int fastAbstractCollection(Collection<E> collection) {
        int size = collection.size();
        return size > 0
                ? (size * SpartanBukkit.hashCodeMultiplier) + collection.iterator().next().hashCode()
                : 1;
    }

    private static int customHash(Enums.HackType hackType, String key) {
        return (key.hashCode() * SpartanBukkit.hashCodeMultiplier) + (hackType == null ? 1 : hackType.hashCode());
    }

    public static boolean cache(SpartanLocation location, Enums.HackType hackType, String key, boolean result) {
        synchronized (memory) {
            memory.computeIfAbsent(location.getIdentifier(), childMap -> new LinkedHashMap<>())
                    .put(customHash(hackType, key), result);
        }
        return result;
    }

    public static Boolean getCached(SpartanLocation location, Enums.HackType hackType, String key) {
        synchronized (memory) {
            Map<Integer, Boolean> childMap = memory.get(location.getIdentifier());
            return childMap != null ? childMap.get(customHash(hackType, key)) : null;
        }
    }

    // Separator

    @SafeVarargs
    public static boolean collides(SpartanLocation location,
                                   boolean top,
                                   boolean aboveTop,
                                   double horizontalBox,
                                   Set<Material>... sets) {
        if (top) {
            double verticalBox;

            if (aboveTop) {
                verticalBox = location.player != null
                        ? location.player.getEyeHeight() + (location.getY() - location.getBlockY())
                        : 1.0;
            } else {
                verticalBox = 0.0;
            }
            return isBlockPattern(new double[][]{
                    {horizontalBox, 0.0, horizontalBox},
                    {horizontalBox, 1.0, horizontalBox, aboveTop ? 1.0 : 0.0},
                    {horizontalBox, verticalBox, horizontalBox, verticalBox > 2.0 ? 1.0 : 0.0}
            }, location, horizontalBox > 0.0, sets);
        } else {
            return isBlockPattern(new double[]{
                    horizontalBox, 0.0, horizontalBox
            }, location, horizontalBox > 0.0, sets);
        }
    }

    // Separator

    @SafeVarargs
    public static boolean isBlockPattern(double[][] positions,
                                         SpartanLocation location,
                                         boolean surroundings,
                                         Set<Material>... sets) {
        int locationIdentifier = location.getIdentifier(),
                searchHash = Boolean.hashCode(surroundings);
        int[] positionIdentifiers = new int[positions.length];
        Map<Integer, Boolean> childMap;

        synchronized (memory) {
            childMap = memory.get(locationIdentifier);
        }
        for (Set<Material> set : sets) {
            searchHash = (searchHash * SpartanBukkit.hashCodeMultiplier) + fastAbstractCollection(set);
        }
        if (childMap != null) {
            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                if (coordinates.length == 3 || coordinates[3] == 1.0) {
                    int positionIdentifier = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;
                    positionIdentifiers[position] = positionIdentifier;
                    Boolean result;

                    synchronized (memory) {
                        result = childMap.get(positionIdentifier);
                    }

                    if (result != null && result) {
                        return true;
                    } else {
                        if (surroundings) {
                            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                                Material material = surroundingLocation.getBlock().material;

                                for (Set<Material> set : sets) {
                                    if (set.contains(material)) {
                                        synchronized (memory) {
                                            childMap.put(positionIdentifiers[position], true);
                                        }
                                        return true;
                                    }
                                }
                            }
                        } else {
                            Material material = location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().material;

                            for (Set<Material> set : sets) {
                                if (set.contains(material)) {
                                    synchronized (memory) {
                                        childMap.put(positionIdentifiers[position], true);
                                    }
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            boolean result = false;
            childMap = new LinkedHashMap<>();

            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                if (coordinates.length == 3 || coordinates[3] == 1.0) {
                    positionIdentifiers[position] = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;

                    if (surroundings) {
                        for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                            Material material = surroundingLocation.getBlock().material;

                            for (Set<Material> set : sets) {
                                if (set.contains(material)) {
                                    childMap.put(positionIdentifiers[position], true);
                                    result = true;
                                    break;
                                }
                            }
                        }

                        if (result) {
                            break;
                        }
                    } else {
                        Material material = location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().material;

                        for (Set<Material> set : sets) {
                            if (set.contains(material)) {
                                childMap.put(positionIdentifiers[position], true);
                                result = true;
                                break;
                            }
                        }
                    }
                }
            }
            synchronized (memory) {
                memory.put(locationIdentifier, childMap);
            }
            return result;
        }
        return false;
    }

    @SafeVarargs
    public static boolean isBlockPattern(double[] coordinates,
                                         SpartanLocation location,
                                         boolean surroundings,
                                         Set<Material>... sets) {
        if (coordinates.length == 3 || coordinates[3] == 1.0) {
            int locationIdentifier = location.getIdentifier(),
                    searchHash = Boolean.hashCode(surroundings);
            Map<Integer, Boolean> childMap;

            // Calculate hashes and check them if cache is available
            for (Set<Material> set : sets) {
                searchHash = (searchHash * SpartanBukkit.hashCodeMultiplier) + fastAbstractCollection(set);
            }
            double x = coordinates[0], y = coordinates[1], z = coordinates[2];
            int positionIdentifier = (Chunks.positionIdentifier(x, y, z) * SpartanBukkit.hashCodeMultiplier) + searchHash;

            synchronized (memory) {
                childMap = memory.get(locationIdentifier);

                if (childMap != null) {
                    Boolean result = childMap.get(positionIdentifier);

                    if (result != null) {
                        return result;
                    }
                } else {
                    childMap = new LinkedHashMap<>();
                    memory.put(locationIdentifier, childMap);
                }
            }

            // Calculate and add them in cache
            if (surroundings) {
                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                    Material material = surroundingLocation.getBlock().material;

                    for (Set<Material> set : sets) {
                        if (set.contains(material)) {
                            synchronized (memory) {
                                childMap.put(positionIdentifier, true);
                            }
                            return true;
                        }
                    }
                }
            } else {
                Material material = location.clone().add(x, y, z).getBlock().material;

                for (Set<Material> set : sets) {
                    if (set.contains(material)) {
                        synchronized (memory) {
                            childMap.put(positionIdentifier, true);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Separator

    public static boolean isWaterLogPattern(double[][] positions,
                                            SpartanLocation location,
                                            boolean surroundings) {
        int locationIdentifier = location.getIdentifier(),
                searchHash = (Boolean.hashCode(surroundings) * SpartanBukkit.hashCodeMultiplier) + waterLogHash;
        int[] positionIdentifiers = new int[positions.length];
        Map<Integer, Boolean> childMap;

        synchronized (memory) {
            childMap = memory.get(locationIdentifier);
        }
        if (childMap != null) {
            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                if (coordinates.length == 3 || coordinates[3] == 1.0) {
                    int positionIdentifier = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;
                    positionIdentifiers[position] = positionIdentifier;
                    Boolean result;

                    synchronized (memory) {
                        result = childMap.get(positionIdentifier);
                    }
                    if (result != null && result) {
                        return true;
                    } else {
                        if (surroundings) {
                            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                                if (surroundingLocation.getBlock().waterLogged) {
                                    synchronized (memory) {
                                        childMap.put(positionIdentifiers[position], true);
                                    }
                                    return true;
                                }
                            }
                        } else if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().waterLogged) {
                            synchronized (memory) {
                                childMap.put(positionIdentifiers[position], true);
                            }
                            return true;
                        }
                    }
                }
            }
        } else {
            boolean result = false;
            childMap = new LinkedHashMap<>();

            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                if (coordinates.length == 3 || coordinates[3] == 1.0) {
                    positionIdentifiers[position] = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;

                    if (surroundings) {
                        for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                            if (surroundingLocation.getBlock().waterLogged) {
                                childMap.put(positionIdentifiers[position], true);
                                result = true;
                                break;
                            }
                        }

                        if (result) {
                            break;
                        }
                    } else {
                        if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().waterLogged) {
                            childMap.put(positionIdentifiers[position], true);
                            result = true;
                            break;
                        }
                    }
                }
            }
            synchronized (memory) {
                memory.put(locationIdentifier, childMap);
            }
            return result;
        }
        return false;
    }

    public static boolean isWaterLogPattern(double[] coordinates,
                                            SpartanLocation location,
                                            boolean surroundings) {
        if (coordinates.length == 3 || coordinates[3] == 1.0) {
            int locationIdentifier = location.getIdentifier(),
                    searchHash = Boolean.hashCode(surroundings);
            searchHash = (searchHash * SpartanBukkit.hashCodeMultiplier) + waterLogHash;
            Map<Integer, Boolean> childMap;

            // Calculate hashes and check them if cache is available
            double x = coordinates[0], y = coordinates[1], z = coordinates[2];
            int positionIdentifier = (Chunks.positionIdentifier(x, y, z) * SpartanBukkit.hashCodeMultiplier) + searchHash;

            synchronized (memory) {
                childMap = memory.get(locationIdentifier);

                if (childMap != null) {
                    Boolean result = childMap.get(positionIdentifier);

                    if (result != null) {
                        return result;
                    }
                } else {
                    childMap = new LinkedHashMap<>();
                    memory.put(locationIdentifier, childMap);
                }
            }

            // Calculate and add them in cache
            if (surroundings) {
                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                    if (surroundingLocation.getBlock().waterLogged) {
                        synchronized (memory) {
                            childMap.put(positionIdentifier, true);
                        }
                        return true;
                    }
                }
            } else if (location.clone().add(x, y, z).getBlock().waterLogged) {
                synchronized (memory) {
                    childMap.put(positionIdentifier, true);
                }
                return true;
            }
        }
        return false;
    }

    // Separator

    public static boolean isWaterPattern(double[][] positions,
                                         SpartanLocation location,
                                         boolean surroundings) {
        int locationIdentifier = location.getIdentifier(),
                searchHash = (Boolean.hashCode(surroundings) * SpartanBukkit.hashCodeMultiplier) + waterHash;
        int[] positionIdentifiers = new int[positions.length];
        Map<Integer, Boolean> childMap;

        synchronized (memory) {
            childMap = memory.get(locationIdentifier);
        }
        if (childMap != null) {
            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                if (coordinates.length == 3 || coordinates[3] == 1.0) {
                    int positionIdentifier = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;
                    positionIdentifiers[position] = positionIdentifier;
                    Boolean result;

                    synchronized (memory) {
                        result = childMap.get(positionIdentifier);
                    }
                    if (result != null && result) {
                        return true;
                    } else {
                        if (surroundings) {
                            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                                if (surroundingLocation.getBlock().isLiquidOrWaterLogged(false)) {
                                    synchronized (memory) {
                                        childMap.put(positionIdentifiers[position], true);
                                    }
                                    return true;
                                }
                            }
                        } else if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().isLiquidOrWaterLogged(false)) {
                            synchronized (memory) {
                                childMap.put(positionIdentifiers[position], true);
                            }
                            return true;
                        }
                    }
                }
            }
        } else {
            boolean result = false;
            childMap = new LinkedHashMap<>();

            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                if (coordinates.length == 3 || coordinates[3] == 1.0) {
                    positionIdentifiers[position] = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;

                    if (surroundings) {
                        for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                            if (surroundingLocation.getBlock().isLiquidOrWaterLogged(false)) {
                                childMap.put(positionIdentifiers[position], true);
                                result = true;
                                break;
                            }
                        }

                        if (result) {
                            break;
                        }
                    } else {
                        if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().isLiquidOrWaterLogged(false)) {
                            childMap.put(positionIdentifiers[position], true);
                            result = true;
                            break;
                        }
                    }
                }
            }

            synchronized (memory) {
                memory.put(locationIdentifier, childMap);
            }
            return result;
        }
        return false;
    }

    public static boolean isWaterPattern(double[] coordinates,
                                         SpartanLocation location,
                                         boolean surroundings) {
        if (coordinates.length == 3 || coordinates[3] == 1.0) {
            int locationIdentifier = location.getIdentifier(),
                    searchHash = Boolean.hashCode(surroundings);
            searchHash = (searchHash * SpartanBukkit.hashCodeMultiplier) + waterHash;
            Map<Integer, Boolean> childMap;

            // Calculate hashes and check them if cache is available
            double x = coordinates[0], y = coordinates[1], z = coordinates[2];
            int positionIdentifier = (Chunks.positionIdentifier(x, y, z) * SpartanBukkit.hashCodeMultiplier) + searchHash;

            synchronized (memory) {
                childMap = memory.get(locationIdentifier);

                if (childMap != null) {
                    Boolean result = childMap.get(positionIdentifier);

                    if (result != null) {
                        return result;
                    }
                } else {
                    childMap = new LinkedHashMap<>();
                    memory.put(locationIdentifier, childMap);
                }
            }

            // Calculate and add them in cache
            if (surroundings) {
                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                    if (surroundingLocation.getBlock().isLiquidOrWaterLogged(false)) {
                        synchronized (memory) {
                            childMap.put(positionIdentifier, true);
                        }
                        return true;
                    }
                }
            } else if (location.clone().add(x, y, z).getBlock().isLiquidOrWaterLogged(false)) {
                synchronized (memory) {
                    childMap.put(positionIdentifier, true);
                }
                return true;
            }
        }
        return false;
    }

    // Separator

    public static boolean isLavaPattern(double[][] positions,
                                        SpartanLocation location,
                                        boolean surroundings) {
        int locationIdentifier = location.getIdentifier(),
                searchHash = (Boolean.hashCode(surroundings) * SpartanBukkit.hashCodeMultiplier) + lavaHash;
        int[] positionIdentifiers = new int[positions.length];
        Map<Integer, Boolean> childMap;

        synchronized (memory) {
            childMap = memory.get(locationIdentifier);
        }
        if (childMap != null) {
            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                if (coordinates.length == 3 || coordinates[3] == 1.0) {
                    int positionIdentifier = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;
                    positionIdentifiers[position] = positionIdentifier;
                    Boolean result;

                    synchronized (memory) {
                        result = childMap.get(positionIdentifier);
                    }
                    if (result != null && result) {
                        return true;
                    } else {
                        if (surroundings) {
                            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                                if (surroundingLocation.getBlock().isLiquid(MaterialUtils.get("lava"))) {
                                    synchronized (memory) {
                                        childMap.put(positionIdentifiers[position], true);
                                    }
                                    return true;
                                }
                            }
                        } else if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().isLiquid(MaterialUtils.get("lava"))) {
                            synchronized (memory) {
                                childMap.put(positionIdentifiers[position], true);
                            }
                            return true;
                        }
                    }
                }
            }
        } else {
            boolean result = false;
            childMap = new LinkedHashMap<>();

            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                if (coordinates.length == 3 || coordinates[3] == 1.0) {
                    positionIdentifiers[position] = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;

                    if (surroundings) {
                        for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                            if (surroundingLocation.getBlock().isLiquid(MaterialUtils.get("lava"))) {
                                childMap.put(positionIdentifiers[position], true);
                                result = true;
                                break;
                            }
                        }

                        if (result) {
                            break;
                        }
                    } else {
                        if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().isLiquid(MaterialUtils.get("lava"))) {
                            childMap.put(positionIdentifiers[position], true);
                            result = true;
                            break;
                        }
                    }
                }
            }
            synchronized (memory) {
                memory.put(locationIdentifier, childMap);
            }
            return result;
        }
        return false;
    }

    public static boolean isLavaPattern(double[] coordinates,
                                        SpartanLocation location,
                                        boolean surroundings) {
        if (coordinates.length == 3 || coordinates[3] == 1.0) {
            int locationIdentifier = location.getIdentifier(),
                    searchHash = Boolean.hashCode(surroundings);
            searchHash = (searchHash * SpartanBukkit.hashCodeMultiplier) + lavaHash;
            Map<Integer, Boolean> childMap;

            // Calculate hashes and check them if cache is available
            double x = coordinates[0], y = coordinates[1], z = coordinates[2];
            int positionIdentifier = (Chunks.positionIdentifier(x, y, z) * SpartanBukkit.hashCodeMultiplier) + searchHash;

            synchronized (memory) {
                childMap = memory.get(locationIdentifier);

                if (childMap != null) {
                    Boolean result = childMap.get(positionIdentifier);

                    if (result != null) {
                        return result;
                    }
                } else {
                    childMap = new LinkedHashMap<>();
                    memory.put(locationIdentifier, childMap);
                }
            }

            // Calculate and add them in cache
            if (surroundings) {
                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                    if (surroundingLocation.getBlock().isLiquid(MaterialUtils.get("lava"))) {
                        synchronized (memory) {
                            childMap.put(positionIdentifier, true);
                        }
                        return true;
                    }
                }
            } else if (location.clone().add(x, y, z).getBlock().isLiquid(MaterialUtils.get("lava"))) {
                synchronized (memory) {
                    childMap.put(positionIdentifier, true);
                }
                return true;
            }
        }
        return false;
    }

    // Separator

    public static boolean isLiquidPattern(double[][] positions,
                                          SpartanLocation location,
                                          boolean surroundings) {
        int locationIdentifier = location.getIdentifier(),
                searchHash = (Boolean.hashCode(surroundings) * SpartanBukkit.hashCodeMultiplier) + liquidHash;
        int[] positionIdentifiers = new int[positions.length];
        Map<Integer, Boolean> childMap;

        synchronized (memory) {
            childMap = memory.get(locationIdentifier);
        }
        if (childMap != null) {
            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                if (coordinates.length == 3 || coordinates[3] == 1.0) {
                    int positionIdentifier = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;
                    positionIdentifiers[position] = positionIdentifier;
                    Boolean result;

                    synchronized (memory) {
                        result = childMap.get(positionIdentifier);
                    }
                    if (result != null && result) {
                        return true;
                    } else {
                        if (surroundings) {
                            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                                if (surroundingLocation.getBlock().isLiquidOrWaterLogged(true)) {
                                    synchronized (memory) {
                                        childMap.put(positionIdentifiers[position], true);
                                    }
                                    return true;
                                }
                            }
                        } else if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().isLiquidOrWaterLogged(true)) {
                            synchronized (memory) {
                                childMap.put(positionIdentifiers[position], true);
                            }
                            return true;
                        }
                    }
                }
            }
        } else {
            boolean result = false;
            childMap = new LinkedHashMap<>();

            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                if (coordinates.length == 3 || coordinates[3] == 1.0) {
                    positionIdentifiers[position] = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;

                    if (surroundings) {
                        for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                            if (surroundingLocation.getBlock().isLiquidOrWaterLogged(true)) {
                                childMap.put(positionIdentifiers[position], true);
                                result = true;
                                break;
                            }
                        }

                        if (result) {
                            break;
                        }
                    } else {
                        if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().isLiquidOrWaterLogged(true)) {
                            childMap.put(positionIdentifiers[position], true);
                            result = true;
                            break;
                        }
                    }
                }
            }
            synchronized (memory) {
                memory.put(locationIdentifier, childMap);
            }
            return result;
        }
        return false;
    }

    public static boolean isLiquidPattern(double[] coordinates,
                                          SpartanLocation location,
                                          boolean surroundings) {
        if (coordinates.length == 3 || coordinates[3] == 1.0) {
            int locationIdentifier = location.getIdentifier(),
                    searchHash = Boolean.hashCode(surroundings);
            searchHash = (searchHash * SpartanBukkit.hashCodeMultiplier) + liquidHash;
            Map<Integer, Boolean> childMap;

            // Calculate hashes and check them if cache is available
            double x = coordinates[0], y = coordinates[1], z = coordinates[2];
            int positionIdentifier = (Chunks.positionIdentifier(x, y, z) * SpartanBukkit.hashCodeMultiplier) + searchHash;

            synchronized (memory) {
                childMap = memory.get(locationIdentifier);

                if (childMap != null) {
                    Boolean result = childMap.get(positionIdentifier);

                    if (result != null) {
                        return result;
                    }
                } else {
                    childMap = new LinkedHashMap<>();
                    memory.put(locationIdentifier, childMap);
                }
            }

            // Calculate and add them in cache
            if (surroundings) {
                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                    if (surroundingLocation.getBlock().isLiquidOrWaterLogged(true)) {
                        synchronized (memory) {
                            childMap.put(positionIdentifier, true);
                        }
                        return true;
                    }
                }
            } else if (location.clone().add(x, y, z).getBlock().isLiquidOrWaterLogged(true)) {
                synchronized (memory) {
                    childMap.put(positionIdentifier, true);
                }
                return true;
            }
        }
        return false;
    }

    // Separator

    public static boolean isNonWaterLoggedLiquidPattern(double[][] positions,
                                                        SpartanLocation location,
                                                        boolean surroundings) {
        int locationIdentifier = location.getIdentifier(),
                searchHash = (Boolean.hashCode(surroundings) * SpartanBukkit.hashCodeMultiplier) + nonWaterLoggedLiquidHash;
        int[] positionIdentifiers = new int[positions.length];
        Map<Integer, Boolean> childMap;

        synchronized (memory) {
            childMap = memory.get(locationIdentifier);
        }
        if (childMap != null) {
            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                if (coordinates.length == 3 || coordinates[3] == 1.0) {
                    int positionIdentifier = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;
                    positionIdentifiers[position] = positionIdentifier;
                    Boolean result;

                    synchronized (memory) {
                        result = childMap.get(positionIdentifier);
                    }
                    if (result != null && result) {
                        return true;
                    } else {
                        if (surroundings) {
                            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                                if (surroundingLocation.getBlock().isNonWaterLoggedLiquid(true)) {
                                    synchronized (memory) {
                                        childMap.put(positionIdentifiers[position], true);
                                    }
                                    return true;
                                }
                            }
                        } else if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().isNonWaterLoggedLiquid(true)) {
                            synchronized (memory) {
                                childMap.put(positionIdentifiers[position], true);
                            }
                            return true;
                        }
                    }
                }
            }
        } else {
            boolean result = false;
            childMap = new LinkedHashMap<>();

            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                if (coordinates.length == 3 || coordinates[3] == 1.0) {
                    positionIdentifiers[position] = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;

                    if (surroundings) {
                        for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                            if (surroundingLocation.getBlock().isNonWaterLoggedLiquid(true)) {
                                childMap.put(positionIdentifiers[position], true);
                                result = true;
                                break;
                            }
                        }

                        if (result) {
                            break;
                        }
                    } else {
                        if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().isNonWaterLoggedLiquid(true)) {
                            childMap.put(positionIdentifiers[position], true);
                            result = true;
                            break;
                        }
                    }
                }
            }
            synchronized (memory) {
                memory.put(locationIdentifier, childMap);
            }
            return result;
        }
        return false;
    }

    public static boolean isNonWaterLoggedLiquidPattern(double[] coordinates,
                                                        SpartanLocation location,
                                                        boolean surroundings) {
        if (coordinates.length == 3 || coordinates[3] == 1.0) {
            int locationIdentifier = location.getIdentifier(),
                    searchHash = Boolean.hashCode(surroundings);
            searchHash = (searchHash * SpartanBukkit.hashCodeMultiplier) + nonWaterLoggedLiquidHash;
            Map<Integer, Boolean> childMap;

            // Calculate hashes and check them if cache is available
            double x = coordinates[0], y = coordinates[1], z = coordinates[2];
            int positionIdentifier = (Chunks.positionIdentifier(x, y, z) * SpartanBukkit.hashCodeMultiplier) + searchHash;

            synchronized (memory) {
                childMap = memory.get(locationIdentifier);

                if (childMap != null) {
                    Boolean result = childMap.get(positionIdentifier);

                    if (result != null) {
                        return result;
                    }
                } else {
                    childMap = new LinkedHashMap<>();
                    memory.put(locationIdentifier, childMap);
                }
            }

            // Calculate and add them in cache
            if (surroundings) {
                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                    if (surroundingLocation.getBlock().isNonWaterLoggedLiquid(true)) {
                        synchronized (memory) {
                            childMap.put(positionIdentifier, true);
                        }
                        return true;
                    }
                }
            } else if (location.clone().add(x, y, z).getBlock().isNonWaterLoggedLiquid(true)) {
                synchronized (memory) {
                    childMap.put(positionIdentifier, true);
                }
                return true;
            }
        }
        return false;
    }
}
