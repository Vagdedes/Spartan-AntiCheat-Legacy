package com.vagdedes.spartan.abstraction.check.implementation.movement.morepackets;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.RayUtils;
import me.vagdedes.spartan.system.Enums.HackType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MorePackets extends CheckRunner {

    public static final byte TELEPORT = 0;

    // Separator

    private final Map<Long, Integer> bukkitNmsMap;
    private int bukkitBuffer, packetVL;
    private final ImplementedProbabilityDetection
            timer,
            positive_net,
            positive_latency,
            positive_balance,
            negative;
    private long teleported;

    public MorePackets(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.bukkitNmsMap = new LinkedHashMap<>(AlgebraUtils.integerCeil(TPS.maximum) + 1, 1.0f);
        this.bukkitBuffer = 0;
        this.packetVL = 0;
        this.timer = new ImplementedProbabilityDetection(
                this,
                "timer",
                true
        );
        this.positive_net = new ImplementedProbabilityDetection(
                this,
                "positive_net",
                true
        );
        this.positive_latency = new ImplementedProbabilityDetection(
                this,
                "positive_latency",
                true
        );
        this.positive_balance = new ImplementedProbabilityDetection(
                this,
                "positive_balance",
                true
        );
        this.negative = new ImplementedProbabilityDetection(
                this,
                "negative",
                true
        );
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerTickEvent) {
            calculate((PlayerTickEvent) object);
        } else if (object instanceof Byte && object.equals(TELEPORT)) {
            this.teleported = System.currentTimeMillis()
                    + (AlgebraUtils.integerCeil(TPS.maximum) * TPS.tickTime * 4L);
        } else if (!this.protocol().packetsEnabled()) {
            bukkitNmsMap.put(TPS.tick(), bukkitNmsMap.getOrDefault(TPS.tick(), 0) + 1);

            if (bukkitNmsMap.size() > configuredTicks()) {
                Iterator<Long> iterator = bukkitNmsMap.keySet().iterator();
                iterator.next();
                iterator.remove();
            }
        }
    }

    private void calculate(PlayerTickEvent event) {
        long delay = event.getDelay();
        TimerBalancer timerBalancer = event.protocol.timerBalancer;
        timerBalancer.pushDelay(delay);
        double multiply = RayUtils.scaleVal((double) TPS.tickTime / delay, 2);

        if (timerBalancer.isNegativeTimer() && delay > 50) {
            if (multiply >= 1.1) {
                punish(this.negative,
                        "type: negative, multiply: " + multiply,
                        multiply
                );
            }
        } else if (delay < 50 && timerBalancer.getResult() > 100) {
            if (multiply >= 1.1) {
                punish(
                        this.positive_balance,
                        "type: positive(balance), multiply: " + multiply,
                        multiply
                );
            }
        } else if (delay < 50 && timerBalancer.getForced() > 8) {
            if (multiply >= 1.1) {
                punish(
                        this.positive_net,
                        "type: positive(NET), multiply: " + multiply,
                        multiply
                );
            }
        } else if (delay < 50 && timerBalancer.getLatency() > 40) {
            if (multiply >= 1.1) {
                punish(
                        this.positive_latency,
                        "type: positive(latency), multiply: " + multiply,
                        multiply
                );
            }
        }
    }

    private void punish(CheckDetection executor, String info, double multiplier) {
        packetVL += 70;

        if (packetVL > 100) {
            executor.cancel(
                    info,
                    this.protocol().spartan.movement.getDetectionLocation(),
                    0,
                    true
            );
            packetVL -= 10;
        } else if (packetVL > 0) {
            packetVL -= 1;
        }
    }

    @Override
    protected void runInternal(boolean cancelled) {
        if (!bukkitNmsMap.isEmpty()
                && !this.protocol().packetsEnabled()) {
            long currentTick = TPS.tick(),
                    oldestTick = currentTick - configuredTicks();
            int period = 0, collections = 0;
            Map<Integer, Integer> positives = new HashMap<>(),
                    negatives = new HashMap<>();
            Map<Integer, Double> ratio = new HashMap<>();
            boolean change = false;

            for (long tick = oldestTick; tick < currentTick; tick++) {
                int calls = bukkitNmsMap.getOrDefault(tick, 0);

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
                            bukkitNmsMap.remove(tick);
                        }
                    } else {
                        bukkitNmsMap.remove(tick);
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
                    bukkitBuffer++;

                    if (bukkitBuffer > 2) {
                        bukkitBuffer = 0;
                        this.timer.cancel(
                                "type: timer"
                                        + ", sum: " + sum
                                        + ", collections: " + collections
                                        + ", ratio: " + ratioSum,
                                this.protocol().spartan.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    }
                } else {
                    bukkitBuffer = 0;
                }
            } else {
                bukkitBuffer = 0;
            }
        } else {
            bukkitBuffer = 0;
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
        return !this.protocol().spartan.isBedrockPlayer()
                && this.teleported < System.currentTimeMillis()
                && MovementProcessing.canCheck(
                this.protocol(),
                this.protocol().packetsEnabled(),
                true,
                true,
                true
        );
    }
}