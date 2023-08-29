package me.vagdedes.spartan.checks.combat.killAura;

// Study of the utility and yaw/pitch movement when in an active fight

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.handlers.tracking.CombatProcessing;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.java.math.LinearRegression;
import me.vagdedes.spartan.utils.java.math.TrigonometryUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class Aim {

    private static final double
            maxObservableDistance = CombatUtils.maxHitDistance * 1.5,
            maxPatternObservations = 30;
    private static final long
            timeKept = 2_500L;
    private static final int
            maxEntities = 5,
            failurePenalty = 1,
            tableGeneralizationLevel = 10;
    private static final long maxPatternObservationsTime = (AlgebraUtils.integerRound(maxPatternObservations * 2) * 50L) + 50L;
    private static final String
            key = Aim.class.getName(),
            compareKey = key + "=compare";
    static final Map<UUID, Map<Integer, DirectionData>> dataMap = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));

    // Separator

    static class DirectionData {

        private final LinkedHashMap<Long, TrigonometryUtils.Side> sides;
        private final LinkedHashMap<Long, Integer> type;

        private DirectionData(long time,
                              double width, double height,
                              boolean countHit,
                              float yaw, float pitch) {
            this.sides = new LinkedHashMap<>();
            this.sides.put(time, new TrigonometryUtils.Side(width, height));
            this.type = new LinkedHashMap<>();
            this.type.put(time, countHit ? 0 : Objects.hash(yaw, pitch));
        }

        private void refresh(long time,
                             double width, double height,
                             long timeKept,
                             boolean countHit,
                             float yaw, float pitch) {
            if (sides.size() > 0) {
                Iterator<Long> iterator = sides.keySet().iterator();

                while (iterator.hasNext()) {
                    Long next = iterator.next();

                    if ((time - next) >= timeKept) {
                        iterator.remove();
                    } else {
                        break;
                    }
                }
            }
            if (type.size() > 0) {
                Iterator<Long> iterator = type.keySet().iterator();

                while (iterator.hasNext()) {
                    Long next = iterator.next();

                    if ((time - next) >= timeKept) {
                        iterator.remove();
                    } else {
                        break;
                    }
                }
            }
            this.sides.put(time, new TrigonometryUtils.Side(width, height));
            this.type.put(time, countHit ? 0 : Objects.hash(yaw, pitch));
        }

        public Collection<TrigonometryUtils.Side> getSides() {
            return sides.values();
        }

        public int getHits() {
            if (type.size() > 0) {
                int count = 0;

                for (int hit : type.values()) {
                    if (hit == 0) {
                        count++;
                    }
                }
                return count;
            }
            return 0;
        }

        public int getUniqueMovements() {
            int size = type.size();

            if (size > 0) {
                Set<Integer> set = new HashSet<>(size - 1);

                for (int hit : type.values()) {
                    if (hit != 0) {
                        set.add(hit);
                    }
                }
                return set.size();
            }
            return 0;
        }

        public int getUnusedTicks() {
            int size = type.size();

            if (size > 2) { // Using either one is the same, but the type holds less memory
                long first = 0L, last = 0L;
                Iterator<Long> iterator = type.keySet().iterator();
                Set<Long> between = new HashSet<>(size - 2);

                while (iterator.hasNext()) {
                    if (first == 0L) {
                        first = AlgebraUtils.roundToNearest(iterator.next(), TPS.tickTimeInteger);
                    } else {
                        long time = AlgebraUtils.roundToNearest(iterator.next(), TPS.tickTimeInteger);
                        between.add(time);
                        last = time;
                    }
                }
                between.remove(last);

                if (between.size() > 0) {
                    int count = 0;

                    for (long loop = first; loop < last; loop += 50L) {
                        if (!between.contains(loop)) {
                            count++;
                        }
                    }
                    return count;
                }
            }
            return 0;
        }

        public boolean remove(TrigonometryUtils.Side side) {
            if (sides.size() > 0) {
                Iterator<Map.Entry<Long, TrigonometryUtils.Side>> iterator = sides.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<Long, TrigonometryUtils.Side> next = iterator.next();

                    if (next.equals(side)) {
                        type.remove(next.getKey());
                        iterator.remove();
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static DirectionData getPastDirectionData(
            UUID uuid, LivingEntity entity,
            double width, double height,
            float yaw, float pitch,
            long timeKept,
            boolean countHit) {
        long time = System.currentTimeMillis();
        DirectionData object;
        Map<Integer, DirectionData> map = dataMap.get(uuid);

        if (map != null) {
            int id = entity.getEntityId();
            object = map.get(id);

            if (object != null) {
                object.refresh(time, width, height, timeKept, countHit, yaw, pitch);
            } else {
                map.put(id, object = new DirectionData(time, width, height, countHit, yaw, pitch));
            }
        } else {
            map = new LinkedHashMap<>();
            map.put(entity.getEntityId(), object = new DirectionData(time, width, height, countHit, yaw, pitch));
            dataMap.put(uuid, map);
        }
        return object;
    }

    private static boolean remove(
            UUID uuid, LivingEntity entity, TrigonometryUtils.Side side) {
        Map<Integer, DirectionData> map = dataMap.get(uuid);

        if (map != null) {
            if (side == null) {
                map.remove(entity.getEntityId());

                if (map.size() == 0) {
                    dataMap.remove(uuid);
                }
                return true;
            } else {
                DirectionData object = map.get(entity.getEntityId());
                return object != null && object.remove(side);
            }
        }
        return false;
    }

    // Separator

    static void remove(SpartanPlayer player) {
        dataMap.remove(player.getUniqueId());
    }

    static void run(SpartanPlayer player, LivingEntity entity, SpartanLocation location, double width, double height) {
        getPastDirectionData(
                player.getUniqueId(), entity,
                width, height,
                location.getYaw(), location.getPitch(),
                PlayerData.combatTimeRequirement,
                true
        );
    }

    static void runMove(SpartanPlayer player, SpartanLocation to, SpartanLocation from, Buffer buffer) {
        if (player.isMoving(true) && Damage.getLastDealt(player) <= KillAura.pastDamageMilliseconds) {
            List<Entity> entities = player.getNearbyEntities(maxObservableDistance, maxObservableDistance, maxObservableDistance);

            if (entities.size() <= maxEntities) {
                UUID uuid = player.getUniqueId();
                Decimals decimals = player.getDecimals(KillAura.check);
                Timer timer = player.getTimer(KillAura.check);
                double reality = Math.toRadians(CombatProcessing.getDecimal(player, CombatProcessing.yawDifference, 0.0f));
                int bufferTime, bufferThreshold;

                if (Damage.getLastReceived(player) >= timeKept // The player hasn't been hit for a while
                        || player.getProfile().isSuspectedOrHacker(KillAura.check)) { // There's significant data against the player already
                    bufferTime = 30;
                    bufferThreshold = 10;
                } else {
                    bufferTime = 45;
                    bufferThreshold = 15;
                }

                for (Entity nearbyEntity : entities) {
                    if (nearbyEntity instanceof LivingEntity) {
                        LivingEntity entity = (LivingEntity) nearbyEntity;

                        if (KillAura.canRunEntity(player, entity)) {
                            double[] utility = CombatUtils.get_X_Y_Distance(player, entity);

                            if (utility != null
                                    && utility[2] >= KillAura.minimumDistance
                                    && !CombatUtils.hasBlockBehind(player, entity)) {
                                if (entity instanceof Player) {
                                    SpartanPlayer target = SpartanBukkit.getPlayer((Player) entity);

                                    if (target != null && !target.isMoving(true)) {
                                        continue;
                                    }
                                }
                                checkMap(uuid, player, to, entity, utility, buffer, bufferTime, bufferThreshold);
                                checkPattern(player, from, entity, decimals, timer, reality);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void checkMap(UUID uuid,
                                 SpartanPlayer player, SpartanLocation location,
                                 LivingEntity entity, double[] utility,
                                 Buffer buffer,
                                 int bufferTime, int bufferThreshold) {
        double width = utility[0];
        DirectionData data = getPastDirectionData(
                uuid, entity,
                width, utility[1],
                location.getYaw(), location.getPitch(),
                timeKept,
                false
        );

        if (data != null) {
            String entityKey = key + "=" + entity.getEntityId();
            double entitySideWidth = (Math.min(CombatUtils.getWidthAndHeight(entity)[0], KillAura.normalWidthToPlayerView) / 2.0) + 0.01;

            if (Math.abs(width) <= entitySideWidth) {
                Collection<TrigonometryUtils.Side> sides = data.getSides();
                int collectedSides = sides.size();

                if (collectedSides >= 10) { // 10 is the starting point where results get significantly more accurate than before
                    int hits = data.getHits();

                    if (hits >= 2) { // 2 to make sure the player is trying to hit the entity and not just follow a certain direction
                        double unusedTicksRatio = data.getUnusedTicks() / ((double) collectedSides),
                                unusedTicksRequiredRatio = Math.min(0.2, Math.max(0.3, hits * 0.04)); // 20-30%

                        if (unusedTicksRatio <= unusedTicksRequiredRatio) {
                            double uniqueMovementsRatio = data.getUniqueMovements() / ((double) collectedSides);

                            if (uniqueMovementsRatio >= 0.8) { // 80%
                                double averageAccuracy = 0.0;
                                int setSize = collectedSides - 1,
                                        validSides = collectedSides;
                                Set<Integer> processed = new HashSet<>(setSize);

                                for (TrigonometryUtils.Side side : sides) {
                                    int x = AlgebraUtils.generalize(side.x, tableGeneralizationLevel),
                                            y = AlgebraUtils.generalize(side.y, tableGeneralizationLevel),
                                            box = Objects.hash(x, y);

                                    if (processed.add(box)) {
                                        double loopAccuracy = 0.0;
                                        int loopDivisor = setSize;
                                        Set<Integer> calculated = new HashSet<>(setSize);

                                        for (TrigonometryUtils.Side otherSide : sides) {
                                            int otherX = AlgebraUtils.generalize(otherSide.x, tableGeneralizationLevel),
                                                    otherY = AlgebraUtils.generalize(otherSide.y, tableGeneralizationLevel),
                                                    otherBox = Objects.hash(otherX, otherY);

                                            if (box != otherBox) {
                                                if (calculated.add(otherBox)) {
                                                    loopAccuracy += Math.sqrt(
                                                            AlgebraUtils.getPreDistance(x, otherX)
                                                                    + AlgebraUtils.getPreDistance(y, otherY)
                                                    );
                                                } else {
                                                    loopDivisor--;
                                                }
                                            }
                                        }
                                        loopAccuracy /= loopDivisor;
                                        averageAccuracy += loopAccuracy;
                                    } else {
                                        validSides--;
                                    }
                                }
                                averageAccuracy /= validSides;
                                averageAccuracy /= tableGeneralizationLevel;

                                if (averageAccuracy < 1.0) {
                                    if (buffer.start(entityKey, bufferTime) == bufferThreshold) {
                                        buffer.remove(entityKey);
                                        remove(uuid, entity, null);
                                        KillAura.violateLive(player,
                                                "type: aim(map)"
                                                        + ", sides: " + collectedSides
                                                        + ", average-accuracy: " + averageAccuracy
                                                        + ", hits: " + hits
                                                        + ", unused-ticks-required: " + unusedTicksRequiredRatio
                                                        + ", unused-ticks: " + unusedTicksRatio
                                                        + ", unique-movements: " + uniqueMovementsRatio
                                                        + ", entity: " + CombatUtils.entityToString(entity)
                                                        + ", width: " + width
                                                        + ", entity-side-width: " + entitySideWidth
                                                        + ", buffer-time: " + bufferTime
                                                        + ", buffer-threshold: " + bufferThreshold);
                                    }
                                } else {
                                    buffer.decrease(entityKey, failurePenalty);
                                }
                            } else {
                                buffer.decrease(entityKey, failurePenalty);
                            }
                        } else {
                            buffer.decrease(entityKey, failurePenalty);
                        }
                    } else {
                        buffer.decrease(entityKey, failurePenalty);
                    }
                }
            } else {
                String entityOffRangeKey = "=off-range";

                if (buffer.start(entityOffRangeKey, 20) == 3) {
                    buffer.remove(entityOffRangeKey);
                    buffer.remove(entityKey);
                    remove(uuid, entity, null);
                }
            }
        }
    }

    private static void checkPattern(SpartanPlayer player, SpartanLocation from,
                                     LivingEntity entity,
                                     Decimals decimals, Timer timer,
                                     double reality) {
        if (reality > 0.0) {
            int id = entity.getEntityId();
            String key = Aim.key + "=" + id,
                    compareKey = Aim.compareKey + "=" + id;
            double prediction = CombatUtils.getRotationDistance(from, Pattern.getEntityLocation(player.getUniqueId(), entity), false);
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
                        KillAura.violate(player, "type: aim(pattern)"
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
