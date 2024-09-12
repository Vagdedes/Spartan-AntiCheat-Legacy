package com.vagdedes.spartan.abstraction.check.implementation.movement.morepackets;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.RayUtils;
import me.vagdedes.spartan.system.Enums.HackType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MorePackets extends CheckExecutor {

    private final Map<Long, Integer> nmsMap;
    private int buffer, vl;

    public MorePackets(HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0, true);
        this.nmsMap = new LinkedHashMap<>((int) TPS.maximum + 1, 1.0f);
        this.buffer = 0;
        this.vl = 0;
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {

        if (object instanceof PlayerTickEvent) {
            if (player.bedrockPlayer) return;
            calculate((PlayerTickEvent) object);
        } else {
            if (!player.bedrockPlayer) return;
            nmsMap.put(TPS.tick(), nmsMap.getOrDefault(TPS.tick(), 0) + 1);

            if (nmsMap.size() > configuredTicks()) {
                Iterator<Long> iterator = nmsMap.keySet().iterator();
                iterator.next();
                iterator.remove();
            }
        }
    }

    private void calculate(PlayerTickEvent event) {
        long delay = event.getDelay();
        TimerBalancer timerBalancer = event.getProtocol().timerBalancer;
        timerBalancer.pushDelay(delay);

        if (timerBalancer.isNegativeTimer()) {
            punish("type: negative, multiply: " + RayUtils.scaleVal((double) 50 / delay, 2));
        } else if (delay < 50 && timerBalancer.getResult() > 100) {
            punish("type: positive (balance), multiply: " + RayUtils.scaleVal((double) 50 / delay, 2));
        } else if (delay < 50 && timerBalancer.getForced() > 8) {
            punish("type: positive (NET), multiply: " + RayUtils.scaleVal((double) 50 / delay, 2));
        } else if (delay < 50 && timerBalancer.getLatency() > 40) {
            punish("type: positive (latency), multiply: " + RayUtils.scaleVal((double) 50 / delay, 2));
        }
    }

    private void punish(String info) {
        vl += 70;
        if (vl > 100) {
            cancel(info);
            vl -= 10;
        } else if (vl > 0) {
            vl -= 1;
        }
    }

    @Override
    protected void runInternal(boolean cancelled) {
        if (!player.bedrockPlayer) return;
        if (!nmsMap.isEmpty()) {
            long currentTick = TPS.tick(),
                    oldestTick = currentTick - configuredTicks();
            int period = 0, collections = 0;
            Map<Integer, Integer> positives = new HashMap<>(),
                    negatives = new HashMap<>();
            Map<Integer, Double> ratio = new HashMap<>();
            boolean change = false;

            for (long tick = oldestTick; tick < currentTick; tick++) {
                int calls = nmsMap.getOrDefault(tick, 0);

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
                } else {
                    int positiveCount = positives.getOrDefault(period, 0);

                    if (positiveCount > 0) {
                        change = true;
                        int negativeCount = negatives.getOrDefault(period, 0) + 1;

                        if (negativeCount <= positiveCount) {
                            negatives.put(period, negativeCount);
                            collections++;
                        } else {
                            nmsMap.remove(tick);
                        }
                    } else {
                        nmsMap.remove(tick);
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

            if (!ratio.isEmpty()
                    && collections > 2) {
                double ratioSum = 0.0;

                for (double ratioValue : ratio.values()) {
                    ratioSum += ratioValue;
                }
                double sum = ratioSum;
                ratioSum /= collections;

                if (collections >= 10 && ratioSum >= 1.1) {
                    buffer++;

                    if (buffer > 2) {
                        buffer = 0;
                        cancel(
                                "type: timer"
                                        + ", sum: " + sum
                                        + ", collections: " + collections
                                        + ", ratio: " + ratioSum,
                                ratioSum,
                                player.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    }
                } else {
                    buffer = 0;
                }
            } else {
                buffer = 0;
            }
        } else {
            buffer = 0;
        }
    }

    private int configuredTicks() {
        int minimumTicks = (int) (TPS.maximum * 5);
        return Math.max(
                AlgebraUtils.integerRound(hackType.getCheck().getNumericalOption(
                        "max_player_latency_until_disconnect",
                        minimumTicks * TPS.tickTimeInteger
                ) / TPS.tickTimeDecimal),
                minimumTicks
        );
    }

    @Override
    protected boolean canRun() {
        return MovementProcessing.canCheck(player, false, true, true, true);
    }
}