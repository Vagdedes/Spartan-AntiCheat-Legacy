package com.vagdedes.spartan.checks.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.*;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.crackshot.CrackShot;
import com.vagdedes.spartan.functionality.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums.HackType;
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
            suspiciouslastTickKey = "suspicious-last-tick",
            refreshLocationKey = "refresh-location",
            instantMotionKey = "instant-motion";

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (object == null) {
            initialLocation = null;
        } else if (object instanceof BlockBreakEvent) {
            Block block = ((BlockBreakEvent) object).getBlock();

            if (BlockUtils.areOres(block.getType())) {
                player.getViolations(hackType).addDisableCause(hackType.toString(), null, 5);
            }
        } else {
            initialLocation = ((SpartanLocation) object);
        }
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        if (canDo()) {
            Double horizontal = player.movement.getNmsHorizontalDistance(),
                    vertical = player.movement.getNmsVerticalDistance();

            if (horizontal == null
                    || vertical == null) {
                return false;
            }
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
            boolean hacker = player.getProfile().isSuspectedOrHacker(hackType);

            // Detections
            if (timer.has(lastTick)) { // Do not start detection if not all cache is available
                double horizontalMotionDifference = player.movement.getCustomHorizontalDistance() - horizontal,
                        verticalMotionDifference = player.movement.getCustomVerticalDistance() - vertical;
                int tickCounter = buffer.increase(dataKey, 1);

                // Instant
                if (lastTick < 10L) { // First stage
                    if (hackType.getCheck().getBooleanOption("check_instant_movements", true)) {
                        boolean decreasedRequirement;
                        int tickBuffer = buffer.start(instantMotionKey + "=first", 1),
                                zeroTickBuffer = lastTick == 0L ? buffer.start(instantMotionKey + "=first=special", 1) : buffer.get(instantMotionKey + "=first=special"),
                                cases;

                        if (tickBuffer >= 10) {
                            cases = 1; // Allow the player to pass when they reach the ticks requirement
                            decreasedRequirement = false;
                        } else if (!player.bedrockPlayer
                                && !player.isUsingItem()
                                && !Damage.hasCooldown(player)
                                && !player.getHandlers().has(Handlers.HandlerType.Explosion)
                                && !player.getHandlers().has(Handlers.HandlerType.ElytraUse)
                                && !player.getHandlers().has(Handlers.HandlerType.Trident)
                                && zeroTickBuffer > (tickBuffer / 2)) {
                            cases = 2; // Alternatively, allow them to pass if more than half of the recorded ticks have been instant in relative to the previous tick
                            decreasedRequirement = true;
                        } else {
                            cases = 0;
                            decreasedRequirement = false;
                        }
                        // Second stage
                        if (cases != 0) {
                            double overallDistance = to.distance(player.getFromLocation()),
                                    overallLimit = 1.5 + (decreasedRequirement ? 0.0 : PlayerUtils.chunk / 3.0);
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
                                boolean instant = !player.bedrockPlayer && (hacker || overallDistance >= (overallLimit * tickCycles));
                                cancel("type: instant, overall: " + overallDistance + ", last-tick: " + lastTick + ", decreased-requirement: " + decreasedRequirement,
                                        instant ? initialLocation : null, instant ? teleportTicks : 0);
                            }
                        }
                    }
                }

                // Horizontal, Vertical
                if (cooldowns.canDo(dataKey)) { // Add to the values as the moment of check they are not positively modified
                    int cycleTicks = tickCycles * maxTicks,
                            extraTicks = (tickCounter + 1) - cycleTicks,
                            extraPackets = player.movement.getExtraPackets();
                    double totalHorizontalMotion = decimals.get(horizontalKey, 0.0) + horizontalMotionDifference,
                            totalVerticalMotion = decimals.get(verticalKey, 0.0) + verticalMotionDifference,
                            tickAverage = (decimals.get(lastTickKey, 0.0) + lastTick) / ((double) cycleTicks);
                    boolean detected;

                    if (Math.round(tickAverage) <= 50.0 && extraTicks >= 5) {
                        boolean instantExtraTicks = extraTicks >= maxTicks,
                                instantExtraPackets = extraPackets >= (maxTicks / 2),
                                instant = !player.bedrockPlayer
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
                                }
                            }
                            cancel("type: horizontal(" + horizontalType + ")" + verbose, initialLocation, instant ? teleportTicks : 0);
                            detected = true;
                        } else {
                            double totalVerticalMotionLimit = 0.5;

                            if (totalVerticalMotion >= totalVerticalMotionLimit && (instantExtraPackets || extraPackets > 0)) {
                                if (!instant) {
                                    if (totalVerticalMotion >= (totalVerticalMotionLimit * tickCycles)) {
                                        instant = true;
                                    }
                                }
                                cancel("type: vertical" + verbose, initialLocation, instant ? teleportTicks : 0);
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

                    if (!player.movement.isFalling(vertical)) { // Do not collect data when falling, it can introduce instabilities and is handled by other checks
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
                && !player.getHandlers().has(Handlers.HandlerType.SensitiveBlockBreak)
                && !CrackShot.isUsingScope(player);
    }
}