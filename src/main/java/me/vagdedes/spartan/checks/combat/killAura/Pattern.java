package me.vagdedes.spartan.checks.combat.killAura;

// Study of the yaw movement in patterns

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.java.math.LinearRegression;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class Pattern {

    private static final Map<UUID, SpartanLocation[]> selfLocations = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));
    private static final Map<UUID, Map<Integer, Location>> entityLocations = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));
    private static final int
            minObservations = 7,
            maxObservations = 20,
            maxStoredEntities = 20;
    private static final double
            requiredDifference = 0.1,
            redundantDifference = Math.toRadians(1.0),
            correlation = 0.95;
    private static final String
            key = Pattern.class.getName(),
            compareKey = key + "=compare";

    static void remove(SpartanPlayer player) {
        UUID uuid = player.getUniqueId();
        selfLocations.remove(uuid);
    }

    static void run(SpartanPlayer player, LivingEntity entity, SpartanLocation location) {
        if (!CombatUtils.hasBlockBehind(player, entity)
                && (entity instanceof Player || entity instanceof Monster)) {
            UUID uuid = player.getUniqueId();
            SpartanLocation[] locations = selfLocations.get(uuid);

            if (locations != null) {
                int scenario = 1;
                double previousYawDifference = 0.0;
                Decimals decimals = player.getDecimals(KillAura.check);
                Vector direction = CombatUtils.getDirection(location.getYaw(), 0.0f);

                for (SpartanLocation oldLocation : locations) {
                    double yawDifference = direction.distance(CombatUtils.getDirection(oldLocation.getYaw(), 0.0f));

                    if (Math.abs(yawDifference - previousYawDifference) > redundantDifference) {
                        previousYawDifference = yawDifference;

                        if (yawDifference >= requiredDifference
                                && detect(
                                player,
                                decimals,
                                scenario,
                                yawDifference,
                                CombatUtils.getRotationDistance(oldLocation, getEntityLocation(uuid, entity), false)
                        )) {
                            break;
                        }
                    }
                    scenario++;
                }
            }
        }
    }

    static void runMove(SpartanPlayer player, SpartanLocation location) {
        UUID uuid = player.getUniqueId();
        SpartanLocation[] locations = selfLocations.get(uuid);

        if (locations != null) {
            if (locations.length > 1) {
                selfLocations.put(uuid, new SpartanLocation[]{location, locations[0], locations[1]});
            } else {
                selfLocations.put(uuid, new SpartanLocation[]{location, locations[0]});
            }
        } else {
            selfLocations.put(uuid, new SpartanLocation[]{location});
        }

        // Separator

        List<Entity> entities = player.getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, maxStoredEntities);

        if (entities.size() > 0) {
            Map<Integer, Location> linkedMap = entityLocations.get(uuid);

            if (linkedMap == null) {
                linkedMap = new LinkedHashMap<>(maxStoredEntities);
                entityLocations.put(uuid, linkedMap);
            }
            for (Entity nearbyEntity : entities) {
                if (nearbyEntity instanceof LivingEntity) {
                    LivingEntity entity = (LivingEntity) nearbyEntity;

                    if (KillAura.canRunEntity(player, entity)) {
                        linkedMap.put(entity.getEntityId(), entity.getLocation());
                    }
                }
            }
            int size = linkedMap.size();

            if (size > maxStoredEntities) {
                Iterator<Integer> iterator = linkedMap.keySet().iterator();

                while (iterator.hasNext() && size != maxStoredEntities) {
                    size--;
                    iterator.next();
                    iterator.remove();
                }
            }
        } else {
            entityLocations.remove(uuid);
        }
    }

    private static boolean detect(SpartanPlayer player, Decimals decimals,
                                  int scenario,
                                  double reality, double prediction) {
        String key = Pattern.key + scenario,
                compareKey = Pattern.compareKey + scenario;
        decimals.add(key, reality);
        decimals.add(compareKey, prediction);

        if (decimals.getCount(key) == maxObservations) {
            LinearRegression regression = new LinearRegression(
                    decimals.getOldestToNewestList(key),
                    decimals.getOldestToNewestList(compareKey)
            );
            decimals.remove(key);
            decimals.remove(compareKey);
            double r2 = regression.r2,
                    error = regression.error,
                    total = r2 + error;
            int observations = regression.observations;

            if (total >= correlation) {
                KillAura.violateAll(player, "type: pattern(long)"
                        + ", scenario: " + scenario
                        + ", total: " + total
                        + ", r2: " + r2
                        + ", error: " + error
                        + ", observations: " + observations
                        + ", required-correlation: " + correlation);
                return true;
            }
        }

        // Separator

        key += "short";
        compareKey += "short";
        decimals.add(key, reality);
        decimals.add(compareKey, prediction);

        if (decimals.getCount(key) == minObservations) {
            LinearRegression regression = new LinearRegression(
                    decimals.getOldestToNewestList(key),
                    decimals.getOldestToNewestList(compareKey)
            );
            decimals.remove(key);
            decimals.remove(compareKey);
            double r2 = regression.r2,
                    error = regression.error,
                    total = r2 + error;
            int observations = regression.observations;

            if (total >= correlation) {
                Buffer buffer = player.getBuffer(KillAura.check);

                if (buffer.start(key, (minObservations * 2) * CombatUtils.getFightTicks(player)) == 2) { // Once suspected, player has 2 more tries time-wise to complete the 2nd point
                    buffer.remove(key);
                    KillAura.violate(player, "type: pattern(short)"
                            + ", scenario: " + scenario
                            + ", total: " + total
                            + ", r2: " + r2
                            + ", error: " + error
                            + ", observations: " + observations
                            + ", required-correlation: " + correlation);
                }
            } else {
                player.getBuffer(KillAura.check).remove(key); // The correlation is too low to trust the algorithm with a player who doesn't reach it again
            }
        }
        return false;
    }

     static Location getEntityLocation(UUID uuid, LivingEntity entity) {
         Map<Integer, Location> linkedMap = entityLocations.get(uuid);
        return linkedMap != null ? linkedMap.getOrDefault(entity.getEntityId(), entity.getLocation()) : entity.getLocation();
    }
}
