package com.vagdedes.spartan.abstraction.check.implementation.combat.killAura;

// Study of the utility and yaw/pitch movement when in an active fight

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.LinearRegression;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class Aim extends DetectionExecutor {

    private final KillAura killAura;

    Aim(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        killAura = (KillAura) executor;
    }

    private static final double
            maxObservableDistance = CombatUtils.maxHitDistance * 1.5,
            maxPatternObservations = 30;
    private static final int maxEntities = 5;
    private static final long maxPatternObservationsTime = (AlgebraUtils.integerRound(maxPatternObservations * 2) * 50L) + 50L;
    private static final String
            key = Aim.class.getName(),
            compareKey = key + "=compare";

    void runMove(SpartanLocation from, Pattern pattern) {
        if (player.movement.isMoving(true)
                && player.getLastDamageDealt().ticksPassed() <= KillAura.pastDamageMilliseconds) {
            List<Entity> entities = player.getNearbyEntities(maxObservableDistance, maxObservableDistance, maxObservableDistance);

            if (entities.size() <= maxEntities) {
                Decimals decimals = player.getDecimals(hackType);
                Timer timer = player.getTimer(hackType);
                double reality = Math.toRadians(CombatProcessing.getDecimal(player, CombatProcessing.yawDifference, 0.0f));

                for (Entity nearbyEntity : entities) {
                    if (nearbyEntity instanceof LivingEntity) {
                        LivingEntity entity = (LivingEntity) nearbyEntity;

                        if (killAura.canRunEntity(entity)) {
                            double[] utility = CombatUtils.get_X_Y_Distance(player, entity);

                            if (utility != null
                                    && utility[2] >= KillAura.minimumDistance
                                    && !CombatUtils.hasBlockBehind(player, entity)) {
                                if (entity instanceof Player) {
                                    SpartanPlayer target = SpartanBukkit.getPlayer((Player) entity);

                                    if (target != null && !target.movement.isMoving(true)) {
                                        continue;
                                    }
                                }
                                checkPattern(from, entity, decimals, timer, reality, pattern);
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkPattern(SpartanLocation from,
                              LivingEntity entity,
                              Decimals decimals, Timer timer,
                              double reality,
                              Pattern pattern) {
        if (reality > 0.0) {
            int id = entity.getEntityId();
            String key = Aim.key + "=" + id,
                    compareKey = Aim.compareKey + "=" + id;
            double prediction = CombatUtils.getRotationDistance(from, pattern.getEntityLocation(entity), false);
            decimals.add(key, reality);
            decimals.add(compareKey, prediction);

            if (decimals.getCount(key) == maxPatternObservations) {
                LinearRegression regression = new LinearRegression(
                        decimals.getOldestToNewestList(key),
                        decimals.getOldestToNewestList(compareKey)
                );
                decimals.remove(key);
                decimals.remove(compareKey);
                double r2 = regression.r2,
                        error = regression.error,
                        total = r2 + error;

                if (total > 0.8) {
                    long time = timer.get(Aim.key);

                    if (time <= maxPatternObservationsTime) {
                        cancel("type: aim(pattern)"
                                + ", total: " + total
                                + ", r2: " + r2
                                + ", error: " + error
                                + ", observations: " + maxPatternObservations
                                + ", time: " + time);
                    }
                    timer.set(Aim.key);
                }
            }
        }
    }

}
