package me.vagdedes.spartan.checks.movement;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.compatibility.manual.abilities.crackshot.CrackShot;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.stability.CancelViolation;
import me.vagdedes.spartan.handlers.stability.DetectionLocation;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.data.*;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;

public class MorePackets extends CheckExecutor {

    public static final HackType check = HackType.MorePackets;

    public MorePackets(SpartanPlayer player) {
        super(check, player);
    }

    private SpartanLocation initialLocation = null;

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

    @Override
    public boolean handle(Object object) {
        if (object == null) {
            initialLocation = null;
        } else if (object instanceof BlockBreakEvent) {
            Block block = ((BlockBreakEvent) object).getBlock();

            if (BlockUtils.areOres(block.getType())) {
                hackType.getCheck().addDisabledUser(player.getUniqueId(), hackType.toString(), 5);
            }
        } else {
            initialLocation = ((SpartanLocation) object);
        }
        return false;
    }

    @Override
    public boolean run() {
        if (canDo()) {
            Timer timer = player.getTimer(hackType);
            Buffer buffer = player.getBuffer(hackType);
            Cooldowns cooldowns = player.getCooldowns(hackType);
            Decimals decimals = player.getDecimals(hackType);
            SpartanLocation to = player.getEventToLocation();

            // Last tick
            long lastTick = timer.get(dataKey);
            timer.set(dataKey);

            // Initial location
            if (initialLocation == null) { // Do not start detection if not all cache is available
                initialLocation = to;
                buffer.remove(dataKey);
                timer.remove(dataKey);
                cooldowns.remove(new String[]{dataKey, refreshLocationKey});
                decimals.remove(new String[]{horizontalKey, verticalKey, lastTickKey});
                return false;
            }
            boolean bedrockPlayer = player.isBedrockPlayer(),
                    hacker = player.getProfile().isSuspectedOrHacker(hackType);

            // Detections
            if (timer.has(lastTick)) { // Do not start detection if not all cache is available
                double horizontal = player.getNmsHorizontalDistance(),
                        vertical = player.getNmsVerticalDistance(),
                        horizontalMotionDifference = player.getCustomHorizontalDistance() - horizontal,
                        verticalMotionDifference = player.getCustomVerticalDistance() - vertical;
                int tickCounter = buffer.increase(dataKey, 1);

                // Instant
                if (hackType.getCheck().getBooleanOption("check_instant_movements", true)
                        && lastTick < 10L) { // First stage
                    double delay = LagLeniencies.getDelay(player);
                    boolean decreasedRequirement = false,
                            canHaveDecreasedRequirement =
                                    !bedrockPlayer
                                            && !player.isUsingItem()
                                            && Damage.hasCooldown(player)
                                            && !player.getHandlers().has(Handlers.HandlerType.Explosion)
                                            && !player.getHandlers().has(Handlers.HandlerType.ElytraUse)
                                            && !player.getHandlers().has(Handlers.HandlerType.Trident);
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
                        SpartanLocation customFrom = player.getFromLocation();
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
                            SpartanLocation handlerFrom = DetectionLocation.get(player, true);
                            SpartanLocation teleportLocation = overallDistance > to.distance(handlerFrom) ? customFrom : handlerFrom;

                            if (instant) {
                                DetectionLocation.set(player, teleportLocation);
                            }
                            cancel("type: instant, overall: " + overallDistance + ", last-tick: " + lastTick + ", ticks-requirement: " + ticksRequirement + ", decreased-requirement: " + decreasedRequirement,
                                    instant ? teleportLocation : null, instant ? teleportTicks : 0);
                        }
                    }
                }

                // Horizontal, Vertical
                if (cooldowns.canDo(dataKey)) { // Add to the values as the moment of check they are not positively modified
                    int cycleTicks = tickCycles * maxTicks,
                            extraTicks = (tickCounter + 1) - cycleTicks,
                            extraPackets = player.getExtraPackets();
                    double totalHorizontalMotion = decimals.get(horizontalKey, 0.0) + horizontalMotionDifference,
                            totalVerticalMotion = decimals.get(verticalKey, 0.0) + verticalMotionDifference,
                            tickAverage = (decimals.get(lastTickKey, 0.0) + lastTick) / ((double) cycleTicks),
                            delay = LagLeniencies.getDelay(player);
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
                                    CancelViolation.force(player, hackType);
                                }
                            } else {
                                CancelViolation.force(player, hackType);
                            }
                            DetectionLocation.set(player, initialLocation);
                            cancel("type: horizontal(" + horizontalType + ")" + verbose, instant ? initialLocation : null, instant ? teleportTicks : 0);
                            detected = true;
                        } else {
                            double totalVerticalMotionLimit = 0.5;

                            if (totalVerticalMotion >= totalVerticalMotionLimit && (instantExtraPackets || extraPackets > 0)) {
                                if (!instant) {
                                    if (totalVerticalMotion >= (totalVerticalMotionLimit * tickCycles)) {
                                        instant = true;
                                        CancelViolation.force(player, hackType);
                                    }
                                } else {
                                    CancelViolation.force(player, hackType);
                                }
                                DetectionLocation.set(player, initialLocation);
                                cancel("type: vertical" + verbose, instant ? initialLocation : null, instant ? teleportTicks : 0);
                                detected = true;
                            } else {
                                detected = false;
                            }
                        }
                    } else {
                        detected = false;
                    }

                    if (!detected && cooldowns.canDo(refreshLocationKey)) {
                        initialLocation = to;
                    } else {
                        cooldowns.add(refreshLocationKey, cycleTicks + (maxTicks / 2));
                    }
                    cooldowns.add(dataKey, cycleTicks);
                    buffer.remove(dataKey);
                    decimals.remove(new String[]{horizontalKey, verticalKey, lastTickKey});
                } else {
                    decimals.set(horizontalKey, decimals.get(horizontalKey, 0.0) + horizontalMotionDifference);
                    decimals.set(lastTickKey, decimals.get(lastTickKey, 0.0) + lastTick);

                    if (!player.isFalling(vertical)) { // Do not collect data when falling, it can introduce instabilities and is handled by other checks
                        decimals.set(verticalKey, decimals.get(verticalKey, 0.0) + verticalMotionDifference);
                    }
                }
            }
        } else {
            initialLocation = null;
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && !player.getHandlers().has(Handlers.HandlerType.WaterElevator)
                && !player.getHandlers().has(Handlers.HandlerType.BlockBreak)
                && !player.getHandlers().has(Handlers.HandlerType.GameMode)
                && !CrackShot.isUsingScope(player);
    }
}