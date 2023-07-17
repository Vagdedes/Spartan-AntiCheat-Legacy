package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.handlers.stability.DetectionLocation;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPreventionUtils;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.utils.gameplay.GroundUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.*;

public class IrregularMovementsStep {

    private static final Map<UUID, LinkedList<SpartanLocation>>
            groundPositionStorage = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers())),
            airPositionStorage = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));
    private static final int maxPositions = 10, singlePositionAllowance = 2, detectionCount = 2;
    private static final double groundBaseLimit = 1.0, airBaseLimit = 1.5,
            downLimit = Math.ceil(MoveUtils.nearMaxFallingMotion) * 1.5;

    static void remove(SpartanPlayer p) {
        UUID uuid = p.getUniqueId();
        groundPositionStorage.remove(uuid);
        airPositionStorage.remove(uuid);
    }

    static void refresh(SpartanPlayer p, SpartanLocation loc) {
        UUID uuid = p.getUniqueId();
        startPositionStorage(groundPositionStorage, uuid, loc);
        startPositionStorage(airPositionStorage, uuid, loc);
    }

    static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation previousFrom,
                    double vertical,
                    boolean velocityScenario, boolean damageScenario, boolean groundInteractionScenario, boolean knownBoxes, boolean bedrockPlayer, boolean falling, boolean oldFalling,
                    String prevention) {
        if (!p.getAllowFlight()) {
            double configurationLimit = IrregularMovements.check.getCheck().getDecimalOption("step_limit_in_blocks", GroundUtils.maxPlayerStep);

            if (configurationLimit > 0.0) {
                AsyncDetection.run(p, IrregularMovements.check, () -> {
                    UUID uuid = p.getUniqueId();
                    double currentY = to.getY();
                    LinkedList<SpartanLocation>
                            storedGroundPositions = groundPositionStorage.get(uuid),
                            storedAirPositions = groundPositionStorage.get(uuid);
                    boolean suspectedOrHacker = p.getProfile().isSuspectedOrHacker(IrregularMovements.check);

                    if (knownBoxes && Math.abs(vertical) < 0.001) {
                        if (storedAirPositions != null) {
                            storedAirPositions.clear();
                        }
                        if (storedGroundPositions == null) {
                            startPositionStorage(groundPositionStorage, uuid, to);
                        } else {
                            double finalLimit = adjustLimit(
                                    Math.max(configurationLimit, groundBaseLimit),
                                    p, bedrockPlayer, velocityScenario, groundInteractionScenario, damageScenario
                            );
                            int ticksUsed = storedGroundPositions.size();
                            boolean hasTicks = ticksUsed > 0,
                                    twoTicksOrLess = (!hasTicks || ticksUsed <= singlePositionAllowance);
                            List<String> upDetections = new ArrayList<>(detectionCount),
                                    downDetections = new ArrayList<>(detectionCount);

                            // Detection #1
                            if (!twoTicksOrLess || ticksUsed > 1) {
                                double totalAdvantage = getPositionStorage(storedGroundPositions);

                                if (totalAdvantage != 0.0) {
                                    totalAdvantage /= (ticksUsed - 1);

                                    if (twoTicksOrLess && !suspectedOrHacker ? totalAdvantage > finalLimit : totalAdvantage >= finalLimit) {
                                        upDetections.add("multiple: " + totalAdvantage);
                                    } else {
                                        finalLimit = Math.max(getDownLimit(p, falling, oldFalling), finalLimit);

                                        if (totalAdvantage < -finalLimit) {
                                            downDetections.add("multiple: " + totalAdvantage);
                                        }
                                    }
                                }
                            }

                            // Detection #2
                            if (twoTicksOrLess) {
                                double totalAdvantage = currentY - previousFrom.getY();

                                if (totalAdvantage > finalLimit) {
                                    upDetections.add("single: " + totalAdvantage);
                                } else {
                                    finalLimit = Math.max(getDownLimit(p, falling, oldFalling), finalLimit);

                                    if (totalAdvantage < -finalLimit) {
                                        downDetections.add("single: " + totalAdvantage);
                                    }
                                }
                            }

                            // Outcome
                            if (upDetections.size() > 0) {
                                cancel(p, getFurthestLocation(p, to, previousFrom, ticksUsed > 0 ? storedGroundPositions.getFirst() : null),
                                        prevention, "ground-up", finalLimit, ticksUsed, upDetections);
                            } else if (downDetections.size() > 0) {
                                cancel(p, getFurthestLocation(p, to, previousFrom, ticksUsed > 0 ? storedGroundPositions.getFirst() : null),
                                        prevention, "ground-down", finalLimit, ticksUsed, downDetections);
                            }

                            if (hasTicks) {
                                storedGroundPositions.clear();
                            }
                            storedGroundPositions.add(to);
                        }
                    } else {
                        if (storedGroundPositions != null) {
                            int ticksUsed = storedGroundPositions.size();

                            if (ticksUsed == 0) {
                                storedGroundPositions.add(to);
                            } else if (storedGroundPositions.getLast().getY() != currentY) {
                                if (ticksUsed >= maxPositions) {
                                    storedGroundPositions.removeFirst();
                                }
                                storedGroundPositions.add(to);
                            }
                        }

                        // Separator
                        if (storedAirPositions != null) {
                            double finalLimit = adjustLimit(
                                    Math.max(configurationLimit, airBaseLimit),
                                    p, bedrockPlayer, velocityScenario, groundInteractionScenario, damageScenario
                            );
                            int ticksUsed = storedAirPositions.size();
                            boolean twoTicksOrLess;
                            List<String> upDetections = new ArrayList<>(detectionCount),
                                    downDetections = new ArrayList<>(detectionCount);

                            if (ticksUsed == 0) {
                                storedAirPositions.add(to);
                                twoTicksOrLess = true;
                            } else {
                                twoTicksOrLess = ticksUsed <= singlePositionAllowance;

                                if (storedAirPositions.getLast().getY() != currentY) {
                                    if (ticksUsed >= maxPositions) {
                                        storedAirPositions.removeFirst();
                                    }
                                    storedAirPositions.add(to);
                                }
                            }

                            // Detection #1
                            if (!twoTicksOrLess || ticksUsed > 1) {
                                double totalAdvantage = getPositionStorage(storedAirPositions);

                                if (totalAdvantage != 0.0) {
                                    totalAdvantage /= (ticksUsed - 1);

                                    if (twoTicksOrLess && !suspectedOrHacker ? totalAdvantage > finalLimit : totalAdvantage >= finalLimit) {
                                        upDetections.add("multiple: " + totalAdvantage);
                                    } else {
                                        finalLimit = Math.max(getDownLimit(p, falling, oldFalling), finalLimit);

                                        if (totalAdvantage < -finalLimit) {
                                            downDetections.add("multiple: " + totalAdvantage);
                                        }
                                    }
                                }
                            }

                            // Detection #2
                            if (twoTicksOrLess) {
                                double totalAdvantage = currentY - previousFrom.getY();

                                if (totalAdvantage != 0.0) {
                                    totalAdvantage /= ticksUsed;

                                    if (totalAdvantage > finalLimit) {
                                        upDetections.add("single: " + totalAdvantage);
                                    } else if (totalAdvantage < 0.0) {
                                        finalLimit = Math.max(getDownLimit(p, falling, oldFalling), finalLimit);

                                        if (totalAdvantage < -finalLimit) {
                                            downDetections.add("single: " + totalAdvantage);
                                        }
                                    }
                                }
                            }

                            // Outcome
                            if (upDetections.size() > 0) {
                                cancel(p, getFurthestLocation(p, to, previousFrom, ticksUsed > 0 ? storedAirPositions.getFirst() : null),
                                        prevention, "air-up", finalLimit, ticksUsed, upDetections);
                            } else if (downDetections.size() > 0) {
                                cancel(p, getFurthestLocation(p, to, previousFrom, ticksUsed > 0 ? storedAirPositions.getFirst() : null),
                                        prevention, "air-down", finalLimit, ticksUsed, downDetections);
                            }
                        } else {
                            startPositionStorage(airPositionStorage, uuid, to);
                        }
                    }
                });
            }
        }
    }

    // Separator

    private static SpartanLocation getFurthestLocation(SpartanPlayer p, SpartanLocation to, SpartanLocation loc1, SpartanLocation loc2) {
        if (loc2 == null) {
            loc2 = DetectionLocation.get(p, false);
        }
        return loc2 == null ? loc1 : to.distance(loc1) >= to.distance(loc2) ? loc1 : loc2;
    }

    private static void startPositionStorage(Map<UUID, LinkedList<SpartanLocation>> map, UUID uuid, SpartanLocation location) {
        LinkedList<SpartanLocation> storedPositions = new LinkedList<>();
        storedPositions.add(location);
        map.put(uuid, storedPositions);
    }

    private static double getPositionStorage(LinkedList<SpartanLocation> linkedList) {
        Iterator<SpartanLocation> iterator = linkedList.descendingIterator();
        double newerPosition = Double.MIN_VALUE, totalAdvantage = 0.0;

        while (iterator.hasNext()) {
            double olderPosition = iterator.next().getY();

            if (newerPosition != Double.MIN_VALUE) {
                totalAdvantage += (newerPosition - olderPosition);
            }
            newerPosition = olderPosition;
        }
        return totalAdvantage;
    }

    // Separator

    private static double getDownLimit(SpartanPlayer player, boolean falling, boolean oldFalling) {
        return player.getAllowFlight() || player.getLastFall() <= 2_000L ? downLimit :
                (falling || oldFalling ? airBaseLimit : groundBaseLimit);
    }

    private static double adjustLimit(double limit,
                                      SpartanPlayer player,
                                      boolean bedrockPlayer, boolean velocityScenario, boolean groundInteractionScenario, boolean damageScenario) {
        double jumpAdditional = player.getJumpEffectLimit(0.0),
                groundOrBedrockAdditional = bedrockPlayer || groundInteractionScenario ? 0.5 : 0.0,
                damageAdditional = damageScenario ? 1.0 : 0.0,
                velocityAdditional = velocityScenario && !damageScenario ? MoveUtils.chunk : 0.0;
        return limit + Math.max(
                Math.max(jumpAdditional, groundOrBedrockAdditional),
                Math.max(damageAdditional, velocityAdditional)
        );
    }

    // Separator

    private static boolean cancel(SpartanPlayer p, SpartanLocation location,
                                  String prevention, String detection,
                                  double limit,
                                  int ticksUsed,
                                  List<String> detections) {
        HackPreventionUtils.grantLocationAccess(p);

        if (IrregularMovements.cancel(
                p,
                location,
                prevention,
                "step-" + detection + "-" + AlgebraUtils.cut(limit, 5),
                StringUtils.toString(detections.toArray(new String[0]), ", ") + ", ticks-used: " + ticksUsed,
                1,
                IrregularMovements.defaultBufferTime
        )) {
            remove(p);
            return true;
        }
        return false;
    }
}
