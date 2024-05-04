package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums.HackType;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MorePackets extends CheckExecutor {

    public static final HackType check = HackType.MorePackets;
    private final Map<Long, Integer> nmsMap;

    public MorePackets(SpartanPlayer player) {
        super(check, player);
        this.nmsMap = Collections.synchronizedMap(new LinkedHashMap<>((int) TPS.maximum + 1, 1.0f));
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        if (canDo()) {
            synchronized (nmsMap) {
                nmsMap.put(TPS.getTick(player), nmsMap.getOrDefault(TPS.getTick(player), 0) + 1);
            }
        }
        return false;
    }

    @Override
    public void scheduler() {
        if (!nmsMap.isEmpty() && canDo()) {
            int minimumTicks = (int) (TPS.maximum * 5),
                    configuredTicks = Math.max(
                            AlgebraUtils.integerRound(hackType.getCheck().getNumericalOption(
                                    "max_player_latency_until_disconnect",
                                    minimumTicks * TPS.tickTimeInteger
                            ) / TPS.tickTimeDecimal),
                            minimumTicks
                    );

            runAsync(() -> {
                long currentTick = TPS.getTick(player),
                        oldestTick = currentTick - configuredTicks;
                int period = 0, collections = 0;
                Map<Integer, Integer> positives = new HashMap<>(),
                        negatives = new HashMap<>();
                Map<Integer, Double> ratio = new HashMap<>();
                boolean change = false;

                for (long tick = oldestTick; tick < currentTick; tick++) {
                    int calls;

                    synchronized (nmsMap) {
                        calls = nmsMap.getOrDefault(tick, 0);
                    }

                    if (calls > 0) {
                        if (change) {
                            change = false;
                            ratio.put(
                                    period,
                                    positives.getOrDefault(period, 0) /
                                            (double) negatives.getOrDefault(period, 1)
                            );
                            period++;
                        }
                        positives.put(period, positives.getOrDefault(period, 0) + calls);
                        collections++;
                    } else if (period != -1) {
                        int positiveCount = positives.getOrDefault(period, 0);

                        if (positiveCount > 0) {
                            change = true;
                            int negativeCount = negatives.getOrDefault(period, 0) + 1;

                            if (negativeCount <= positiveCount) {
                                negatives.put(period, negativeCount);
                                collections++;
                            } else {
                                synchronized (nmsMap) {
                                    nmsMap.remove(tick);
                                }
                            }
                        } else {
                            synchronized (nmsMap) {
                                nmsMap.remove(tick);
                            }
                        }
                    }
                }
                int positiveCount = positives.getOrDefault(period, 0);

                if (change || positiveCount > 0) {
                    ratio.put(
                            period,
                            positiveCount / (double) negatives.getOrDefault(period, 1)
                    );
                }
                synchronized (nmsMap) {
                    nmsMap.remove(oldestTick);
                }

                if (!ratio.isEmpty()) {
                    double ratioSum = 0.0;

                    for (double ratioValue : ratio.values()) {
                        ratioSum += ratioValue;
                    }
                    double sum = ratioSum;
                    ratioSum /= collections;

                    if (true || ratioSum > 1.0) {
                        player.debug(true, false, true,
                                ratioSum,
                                sum,
                                collections);
                    }
                }
            });
        }
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && MovementProcessing.canCheck(player, true, true);
    }
}