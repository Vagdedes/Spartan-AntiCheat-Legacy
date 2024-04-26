package com.vagdedes.spartan.abstraction.check.implementation.combat.killAura;

// Study of the yaw movement in patterns

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.functionality.management.Cache;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.math.LinearRegression;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Pattern extends DetectionExecutor {

    private final KillAura killAura;
    private SpartanLocation[] selfLocations;

    Pattern(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        killAura = (KillAura) executor;
        selfLocations = new SpartanLocation[]{executor.player.movement.getCustomFromLocation()};
    }

    private final Map<Integer, Location> entityLocations = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));
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

    void remove() {
        selfLocations = null;
    }

    void run(LivingEntity entity, SpartanLocation location) {
        if (!CombatUtils.hasBlockBehind(player, entity)
                && (entity instanceof Player || entity instanceof Monster)) {
            int scenario = 1;
            double previousYawDifference = 0.0;
            Decimals decimals = player.getDecimals(hackType);
            Vector direction = CombatUtils.getDirection(location.getYaw(), 0.0f);

            for (SpartanLocation oldLocation : selfLocations) {
                double yawDifference = direction.distance(CombatUtils.getDirection(oldLocation.getYaw(), 0.0f));

                if (Math.abs(yawDifference - previousYawDifference) > redundantDifference) {
                    previousYawDifference = yawDifference;

                    if (yawDifference >= requiredDifference
                            && detect(
                            decimals,
                            scenario,
                            yawDifference,
                            CombatUtils.getRotationDistance(oldLocation, getEntityLocation(entity), false)
                    )) {
                        break;
                    }
                }
                scenario++;
            }
        }
    }

    void runMove(SpartanLocation location) {
        if (selfLocations.length > 1) {
            selfLocations = new SpartanLocation[]{location, selfLocations[0], selfLocations[1]};
        } else {
            selfLocations = new SpartanLocation[]{location, selfLocations[0]};
        }

        // Separator

        List<Entity> entities = player.getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);

        if (!entities.isEmpty()) {
            for (Entity nearbyEntity : entities) {
                if (nearbyEntity instanceof LivingEntity) {
                    LivingEntity entity = (LivingEntity) nearbyEntity;

                    if (killAura.canRunEntity(entity)) {
                        entityLocations.put(entity.getEntityId(), entity.getLocation());
                    }
                }
            }
            int size = entityLocations.size();

            if (size > maxStoredEntities) {
                Iterator<Integer> iterator = entityLocations.keySet().iterator();

                while (iterator.hasNext() && size != maxStoredEntities) {
                    size--;
                    iterator.next();
                    iterator.remove();
                }
            }
        } else {
            entityLocations.clear();
        }
    }

    private boolean detect(Decimals decimals,
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
                killAura.violateLive("type: pattern(long)"
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
                Buffer buffer = player.getBuffer(hackType);
                int ticks = (minObservations * 2) * CombatUtils.getFightTicks(player);

                if (buffer.start(key, ticks, Check.detectionMeasurementTicks) == (2 / (double) ticks)) { // Once suspected, player has 2 more tries time-wise to complete the 2nd point
                    buffer.remove(key);
                    cancel("type: pattern(short)"
                            + ", scenario: " + scenario
                            + ", total: " + total
                            + ", r2: " + r2
                            + ", error: " + error
                            + ", observations: " + observations
                            + ", required-correlation: " + correlation);
                }
            } else {
                player.getBuffer(hackType).remove(key); // The correlation is too low to trust the algorithm with a player who doesn't reach it again
            }
        }
        return false;
    }

    Location getEntityLocation(LivingEntity entity) {
        return entityLocations.getOrDefault(entity.getEntityId(), entity.getLocation());
    }
}
