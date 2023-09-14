package me.vagdedes.spartan.checks.movement;

import me.vagdedes.spartan.compatibility.manual.abilities.crackshot.CrackShot;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.stability.CancelViolation;
import me.vagdedes.spartan.handlers.stability.DetectionLocation;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.data.*;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class MorePackets {

    public static final HackType check = HackType.MorePackets;
    private static final Map<UUID, SpartanLocation> initialLocation = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers())); // Do not make the map weak, as it takes long for cycles to complete

    private static final int
            maxTicks = AlgebraUtils.integerRound(TPS.maximum),
            tickCycles = 3,
            teleportTicks = 10;

    private static final String
            dataKey = "data",
            horizontalKey = "motion-xz",
            verticalKey = "motion-y",
            lastTickKey = "last-tick",
            refreshLocationKey = "refresh-location",
            instantMotionKey = "instant-motion";

    public static void remove(SpartanPlayer p) {
        initialLocation.remove(p.getUniqueId());
    }

    public static void refresh(SpartanPlayer p, SpartanLocation loc) {
        initialLocation.put(p.getUniqueId(), loc);
    }

    public static void handleMining(SpartanPlayer p, SpartanBlock b) {
        if (BlockUtils.areOres(b.getType())) {
            check.getCheck().addDisabledUser(p.getUniqueId(), check.toString(), 5);
        }
    }

    public static void run(SpartanPlayer p, SpartanLocation to, double horizontal, double vertical) {
        if (canDo(p)) {
            Timer timer = p.getTimer(check);
            Buffer buffer = p.getBuffer(check);
            Cooldowns cooldowns = p.getCooldowns(check);
            Decimals decimals = p.getDecimals(check);

            // Last tick
            long lastTick = timer.get(dataKey);
            timer.set(dataKey);

            // Initial location
            UUID uuid = p.getUniqueId();
            SpartanLocation initialLoc = initialLocation.get(uuid);

            if (initialLoc == null) { // Do not start detection if not all cache is available
                initialLocation.put(uuid, to);
                buffer.remove(dataKey);
                timer.remove(dataKey);
                cooldowns.remove(new String[]{dataKey, refreshLocationKey});
                decimals.remove(new String[]{horizontalKey, verticalKey, lastTickKey});
                return;
            }
            boolean bedrockPlayer = p.isBedrockPlayer(),
                    hacker = p.getProfile().isSuspectedOrHacker(check);

            // Detections
            if (timer.has(lastTick)) { // Do not start detection if not all cache is available
                double horizontalMotionDifference = p.getCustomHorizontalDistance() - horizontal,
                        verticalMotionDifference = p.getCustomVerticalDistance() - vertical;
                int tickCounter = buffer.increase(dataKey, 1);

                // Instant
                if (check.getCheck().getBooleanOption("check_instant_movements", true)
                        && lastTick < 10L) { // First stage
                    double delay = LagLeniencies.getDelay(p);
                    boolean decreasedRequirement = false,
                            canHaveDecreasedRequirement =
                                    !bedrockPlayer
                                            && !p.isUsingItem()
                                            && Damage.hasCooldown(p)
                                            && !p.getHandlers().has(Handlers.HandlerType.Explosion)
                                            && !p.getHandlers().has(Handlers.HandlerType.ElytraUse)
                                            && !p.getHandlers().has(Handlers.HandlerType.Trident);
                    int ticksRequirement = Math.min(10 + AlgebraUtils.integerRound(delay), 15),
                            first = buffer.start(instantMotionKey + "=first", 1),
                            special;

                    if (lastTick == 0L) { // instant tick relative to its previous
                        special = buffer.start(instantMotionKey + "=first=special", 1);
                    } else {
                        special = buffer.get(instantMotionKey + "=first=special");
                    }

                    // Second stage
                    if (first >= ticksRequirement // Allow the player to pass when they reach the ticks requirement
                            || canHaveDecreasedRequirement && (decreasedRequirement = first >= 2 && special > (first / 2))) { // Alternatively, allow them to pass if more than half of the recorded ticks have been instant in relative to the previous tick
                        SpartanLocation customFrom = MoveUtils.getCachedLocation(p, true);

                        if (customFrom != null) {
                            double overallDistance = to.distance(customFrom),
                                    overallLimit = 1.5 + (decreasedRequirement ? 0.0 : Math.min(delay * 0.5, MoveUtils.chunk / 3.0));
                            boolean passedLimit;

                            if (overallDistance >= overallLimit) {
                                passedLimit = true;
                                cooldowns.add(instantMotionKey + "=passed-limit", 2);
                            } else {
                                passedLimit = !cooldowns.canDo(instantMotionKey + "=passed-limit");
                            }
                            if (passedLimit
                                    && (decreasedRequirement || buffer.start(instantMotionKey + "=second", 1) >= 2)) { // Third stage
                                buffer.remove(new String[]{
                                        instantMotionKey + "=first",
                                        instantMotionKey + "=first=special",
                                        instantMotionKey + "=second"
                                });

                                boolean instant = !bedrockPlayer && (hacker || overallDistance >= (overallLimit * tickCycles));
                                SpartanLocation handlerFrom = DetectionLocation.get(p, true);
                                SpartanLocation teleportLocation = overallDistance > to.distance(handlerFrom) ? customFrom : handlerFrom;

                                if (instant) {
                                    DetectionLocation.set(p, teleportLocation);
                                }
                                new HackPrevention(p, check, "type: instant, overall: " + overallDistance + ", last-tick: " + lastTick + ", ticks-requirement: " + ticksRequirement + ", decreased-requirement: " + decreasedRequirement,
                                        instant ? teleportLocation : null, instant ? teleportTicks : 0);
                            }
                        }
                    }
                }

                // Horizontal, Vertical
                if (cooldowns.canDo(dataKey)) { // Add to the values as the moment of check they are not positively modified
                    int cycleTicks = tickCycles * maxTicks,
                            extraTicks = (tickCounter + 1) - cycleTicks,
                            extraPackets = p.getExtraPackets();
                    double totalHorizontalMotion = decimals.get(horizontalKey, 0.0) + horizontalMotionDifference,
                            totalVerticalMotion = decimals.get(verticalKey, 0.0) + verticalMotionDifference,
                            tickAverage = (decimals.get(lastTickKey, 0.0) + lastTick) / ((double) cycleTicks),
                            delay = LagLeniencies.getDelay(p);
                    boolean detected;

                    if ((delay < 1.0 ? Math.round(tickAverage) : tickAverage) <= 50.0 && extraTicks >= 5) {
                        boolean instantExtraTicks = extraTicks >= maxTicks,
                                instantExtraPackets = extraPackets >= (maxTicks / 2),
                                instant = !bedrockPlayer
                                        && (instantExtraPackets
                                        || instantExtraTicks
                                        || hacker);
                        double totalHorizontalMotionLimit = 1.0;
                        String verbose = ", total-horizontal: " + horizontal
                                + ", total-vertical: " + vertical
                                + ", extra-ticks: " + extraTicks
                                + ", extra-packets: " + extraPackets
                                + ", tick-average: " + tickAverage;
                        String horizontalType = totalHorizontalMotion >= totalHorizontalMotionLimit ? "positive" :
                                totalHorizontalMotion <= -totalHorizontalMotionLimit ? "negative" : null;

                        if (horizontalType != null && (instantExtraPackets || instantExtraTicks || extraPackets >= 2)) {
                            if (!instant) {
                                if (totalHorizontalMotion >= (totalHorizontalMotionLimit * tickCycles)) {
                                    instant = true;
                                    CancelViolation.force(p, check);
                                }
                            } else {
                                CancelViolation.force(p, check);
                            }
                            DetectionLocation.set(p, initialLoc);
                            new HackPrevention(p, check, "type: horizontal(" + horizontalType + ")" + verbose, instant ? initialLoc : null, instant ? teleportTicks : 0);
                            detected = true;
                        } else {
                            double totalVerticalMotionLimit = 0.5;

                            if (totalVerticalMotion >= totalVerticalMotionLimit && (instantExtraPackets || extraPackets > 0)) {
                                if (!instant) {
                                    if (totalVerticalMotion >= (totalVerticalMotionLimit * tickCycles)) {
                                        instant = true;
                                        CancelViolation.force(p, check);
                                    }
                                } else {
                                    CancelViolation.force(p, check);
                                }
                                DetectionLocation.set(p, initialLoc);
                                new HackPrevention(p, check, "type: vertical" + verbose, instant ? initialLoc : null, instant ? teleportTicks : 0);
                                detected = true;
                            } else {
                                detected = false;
                            }
                        }
                    } else {
                        detected = false;
                    }

                    if (!detected && cooldowns.canDo(refreshLocationKey)) {
                        initialLocation.put(uuid, to);
                    } else {
                        cooldowns.add(refreshLocationKey, cycleTicks + (maxTicks / 2));
                    }
                    cooldowns.add(dataKey, cycleTicks);
                    buffer.remove(dataKey);
                    decimals.remove(new String[]{horizontalKey, verticalKey, lastTickKey});
                } else {
                    decimals.set(horizontalKey, decimals.get(horizontalKey, 0.0) + horizontalMotionDifference);
                    decimals.set(lastTickKey, decimals.get(lastTickKey, 0.0) + lastTick);

                    if (!p.isFalling(vertical)) { // Do not collect data when falling, it can introduce instabilities and is handled by other checks
                        decimals.set(verticalKey, decimals.get(verticalKey, 0.0) + verticalMotionDifference);
                    }
                }
            }
        } else {
            remove(p);
        }
    }

    private static boolean canDo(SpartanPlayer p) {
        return p.canRunCheck(check, true)
                && !p.getHandlers().has(Handlers.HandlerType.WaterElevator)
                && !p.getHandlers().has(Handlers.HandlerType.BlockBreak)
                && !p.getHandlers().has(Handlers.HandlerType.GameMode)
                && !CrackShot.isUsingScope(p);
    }
}