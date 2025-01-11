package com.vagdedes.spartan.abstraction.check.implementation.movement.morepackets;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
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

    public MorePackets(HackType hackType, SpartanProtocol protocol) {
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
            PlayerTickEvent event = (PlayerTickEvent) object;
            long delay = event.getDelay();
            TimerBalancer timerBalancer = event.protocol.timerBalancer;
            timerBalancer.pushDelay(delay);
            double multiply = RayUtils.scaleVal((double) TPS.tickTime / delay, 2);

            if (timerBalancer.isNegativeTimer() && delay > 50) {
                if (multiply < 1.0) {
                    punish(
                            this.negative,
                            "type: negative, multiply: " + multiply
                    );
                }
            } else if (delay < 50 && timerBalancer.getResult() > 100) {
                if (multiply >= 1.1) {
                    punish(
                            this.positive_balance,
                            "type: positive(balance), multiply: " + multiply
                    );
                }
            } else if (delay < 50 && timerBalancer.getForced() > 8) {
                if (multiply >= 1.1) {
                    punish(
                            this.positive_net,
                            "type: positive(NET), multiply: " + multiply
                    );
                }
            } else if (delay < 50 && timerBalancer.getLatency() > 40) {
                if (multiply >= 1.1) {
                    punish(
                            this.positive_latency,
                            "type: positive(latency), multiply: " + multiply
                    );
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
        return !this.protocol.spartan.isAFK()
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