package com.vagdedes.spartan.abstraction.check.implementation.movement.morepackets;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.RayUtils;
import me.vagdedes.spartan.system.Enums.HackType;

public class MorePackets extends CheckRunner {

    public static final byte TELEPORT = 0;

    // Separator

    private int packetVL;
    private final ImplementedProbabilityDetection
            positive_net,
            positive_latency,
            positive_balance,
            negative;
    private long teleported;

    public MorePackets(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.packetVL = 0;
        this.positive_net = new ImplementedProbabilityDetection(
                this,
                Check.DataType.JAVA,
                Check.DetectionType.PACKETS,
                "positive_net",
                true
        );
        this.positive_latency = new ImplementedProbabilityDetection(
                this,
                Check.DataType.JAVA,
                Check.DetectionType.PACKETS,
                "positive_latency",
                true
        );
        this.positive_balance = new ImplementedProbabilityDetection(
                this,
                Check.DataType.JAVA,
                Check.DetectionType.PACKETS,
                "positive_balance",
                true
        );
        this.negative = new ImplementedProbabilityDetection(
                this,
                Check.DataType.JAVA,
                Check.DetectionType.PACKETS,
                "negative",
                true
        );
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerTickEvent) {
            if (this.positive_balance.canCall()
                    || this.positive_latency.canCall()
                    || this.positive_net.canCall()
                    || this.negative.canCall()) {
                PlayerTickEvent event = (PlayerTickEvent) object;
                long delay = event.getDelay();
                TimerBalancer timerBalancer = event.protocol.timerBalancer;
                timerBalancer.pushDelay(delay);
                double multiply = RayUtils.scaleVal((double) TPS.tickTime / delay, 2);

                if (timerBalancer.isNegativeTimer() && delay > 50) {
                    this.negative.call(() -> {
                        if (multiply < 1.0) {
                            punish(
                                    this.negative,
                                    "negative, multiply: " + multiply
                            );
                        }
                    });
                } else if (delay < 50 && timerBalancer.getResult() > 100) {
                    this.positive_balance.call(() -> {
                        if (multiply >= 1.1) {
                            punish(
                                    this.positive_balance,
                                    "positive(balance), multiply: " + multiply
                            );
                        }
                    });
                } else if (delay < 50 && timerBalancer.getForced() > 8) {
                    this.positive_net.call(() -> {
                        if (multiply >= 1.1) {
                            punish(
                                    this.positive_net,
                                    "positive(NET), multiply: " + multiply
                            );
                        }
                    });
                } else if (delay < 50 && timerBalancer.getLatency() > 40) {
                    this.positive_latency.call(() -> {
                        if (multiply >= 1.1) {
                            punish(
                                    this.positive_latency,
                                    "positive(latency), multiply: " + multiply
                            );
                        }
                    });
                }
            }
        } else {
            this.teleported = System.currentTimeMillis()
                    + (AlgebraUtils.integerCeil(TPS.maximum) * TPS.tickTime * 4L);
        }
    }

    private void punish(CheckDetection executor, String info) {
        packetVL += 70;

        if (packetVL > 100) {
            executor.cancel(
                    info,
                    this.protocol.getFromLocation(),
                    0,
                    true
            );
            packetVL -= 10;
        } else if (packetVL > 0) {
            packetVL -= 1;
        }
    }

    @Override
    protected boolean canRun() {
        return !this.protocol.bukkitExtra.isAFK()
                && this.teleported < System.currentTimeMillis()
                && MovementProcessing.canCheck(
                this.protocol,
                true,
                true,
                true,
                true
        );
    }
}