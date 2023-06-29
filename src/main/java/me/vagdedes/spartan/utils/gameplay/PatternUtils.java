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

    public static boolean isBlockPattern(SpartanLocation location, double[][] positions, boolean surroundings, Set<Material>... sets) {
        int locationIdentifier = location.getIdentifier(),
                searchHash = Boolean.hashCode(surroundings);
        int[] positionIdentifiers = new int[positions.length];
        Map<Integer, Boolean> childMap = memory.get(locationIdentifier);

        // Calculate hashes and check them if cache is available
        for (Set<Material> set : sets) {
            searchHash = (searchHash * SpartanBukkit.hashCodeMultiplier) + set.hashCode();
        }
        if (childMap != null) {
            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];
                int positionIdentifier = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;
                positionIdentifiers[position] = positionIdentifier;

                if (childMap.getOrDefault(positionIdentifier, false)) {
                    return true;
                }
            }
        } else {
            childMap = new ConcurrentHashMap<>();
            memory.put(locationIdentifier, childMap);

            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];
                positionIdentifiers[position] = (Chunks.positionIdentifier(coordinates[0], coordinates[1], coordinates[2]) * SpartanBukkit.hashCodeMultiplier) + searchHash;
            }
        }

        // Calculate andd add them in cache
        if (surroundings) {
            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];

                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                    Material material = surroundingLocation.getBlock().getType();

                    for (Set<Material> set : sets) {
                        if (set.contains(material)) {
                            childMap.put(positionIdentifiers[position], true);
                            return true;
                        }
                    }
                }
            }
        } else {
            for (int position = 0; position < positions.length; position++) {
                double[] coordinates = positions[position];
                Material material = location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().getType();

                for (Set<Material> set : sets) {
                    if (set.contains(material)) {
                        childMap.put(positionIdentifiers[position], true);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
