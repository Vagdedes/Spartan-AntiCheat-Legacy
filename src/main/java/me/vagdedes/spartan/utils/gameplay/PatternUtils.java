package me.vagdedes.spartan.utils.gameplay;

import me.vagdedes.spartan.handlers.stability.Chunks;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.Material;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PatternUtils {

    private static final Map<Integer, Map<Integer, Boolean>> memory = Cache.store(new ConcurrentHashMap<>());

    public static void synchronizeClearance(SpartanBlock block) {
        memory.remove(block.getIdentifier());
    }

    @SafeVarargs
    public static boolean isBlockPattern(double[][] positions,
                                         SpartanLocation location,
                                         boolean surroundings, boolean not,
                                         Set<Material>... sets) {
        int locationIdentifier = location.getIdentifier(),
                searchHash = (Boolean.hashCode(surroundings) * SpartanBukkit.hashCodeMultiplier) + Boolean.hashCode(not);
        int[] positionIdentifiers = new int[positions.length];
        Map<Integer, Boolean> childMap = memory.get(locationIdentifier);

        for (Set<Material> set : sets) {
            searchHash = (searchHash * SpartanBukkit.hashCodeMultiplier) + set.hashCode();
        }
        if (childMap != null) {
            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];
                int positionIdentifier = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;
                positionIdentifiers[position] = positionIdentifier;
                Boolean result = childMap.get(positionIdentifier);

                if (result != null) {
                    return result;
                }
                if (surroundings) {
                    for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                        Material material = surroundingLocation.getBlock().getType();

                        for (Set<Material> set : sets) {
                            if (not != set.contains(material)) {
                                childMap.put(positionIdentifiers[position], true);
                                return true;
                            }
                        }
                    }
                } else {
                    Material material = location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().getType();

                    for (Set<Material> set : sets) {
                        if (not != set.contains(material)) {
                            childMap.put(positionIdentifiers[position], true);
                            return true;
                        }
                    }
                }
            }
        } else {
            childMap = new ConcurrentHashMap<>();
            memory.put(locationIdentifier, childMap);

            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];
                positionIdentifiers[position] = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;

                if (surroundings) {
                    for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                        Material material = surroundingLocation.getBlock().getType();

                        for (Set<Material> set : sets) {
                            if (not != set.contains(material)) {
                                childMap.put(positionIdentifiers[position], true);
                                return true;
                            }
                        }
                    }
                } else {
                    Material material = location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().getType();

                    for (Set<Material> set : sets) {
                        if (not != set.contains(material)) {
                            childMap.put(positionIdentifiers[position], true);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @SafeVarargs
    public static boolean isBlockPattern(double[] coordinates,
                                         SpartanLocation location,
                                         boolean surroundings, boolean not,
                                         Set<Material>... sets) {
        int locationIdentifier = location.getIdentifier(),
                searchHash = (Boolean.hashCode(surroundings) * SpartanBukkit.hashCodeMultiplier) + Boolean.hashCode(not);
        Map<Integer, Boolean> childMap = memory.get(locationIdentifier);

        // Calculate hashes and check them if cache is available
        for (Set<Material> set : sets) {
            searchHash = (searchHash * SpartanBukkit.hashCodeMultiplier) + set.hashCode();
        }
        double x = coordinates[0], y = coordinates[1], z = coordinates[2];
        int positionIdentifier = (Chunks.positionIdentifier(x, y, z) * SpartanBukkit.hashCodeMultiplier) + searchHash;

        if (childMap != null) {
            Boolean result = childMap.get(positionIdentifier);

            if (result != null) {
                return result;
            }
        } else {
            childMap = new ConcurrentHashMap<>();
            memory.put(locationIdentifier, childMap);
        }

        // Calculate and add them in cache
        if (surroundings) {
            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                Material material = surroundingLocation.getBlock().getType();

                for (Set<Material> set : sets) {
                    if (not != set.contains(material)) {
                        childMap.put(positionIdentifier, true);
                        return true;
                    }
                }
            }
        } else {
            Material material = location.clone().add(x, y, z).getBlock().getType();

            for (Set<Material> set : sets) {
                if (not != set.contains(material)) {
                    childMap.put(positionIdentifier, true);
                    return true;
                }
            }
        }
        return false;
    }
}
