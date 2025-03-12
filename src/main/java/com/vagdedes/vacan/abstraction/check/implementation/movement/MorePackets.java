package com.vagdedes.vacan.abstraction.check.implementation.movement;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.vacan.abstraction.check.Check;
import com.vagdedes.vacan.abstraction.check.CheckDetection;
import com.vagdedes.vacan.abstraction.check.CheckRunner;
import com.vagdedes.vacan.abstraction.check.definition.ImplementedDetection;
import com.vagdedes.vacan.abstraction.data.TimerBalancer;
import com.vagdedes.vacan.abstraction.event.PlayerTickEvent;
import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.functionality.server.PluginBase;
import com.vagdedes.vacan.functionality.server.TPS;
import com.vagdedes.vacan.functionality.tracking.MovementProcessing;
import com.vagdedes.vacan.listeners.protocol.TeleportListener;
import com.vagdedes.vacan.utils.math.AlgebraUtils;
import com.vagdedes.vacan.utils.math.RayUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.event.player.PlayerTeleportEvent;

public class MorePackets extends CheckRunner {

    private int packetVL;
    private final ImplementedDetection
            positive_net,
            positive_latency,
            positive_balance,
            negative;
    private long teleported;

    public MorePackets(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.packetVL = 0;
        this.positive_net = new ImplementedDetection(
                this,
                Check.DataType.JAVA,
                Check.DetectionType.PACKETS,
                "positive_net",
                true
        );
        this.positive_latency = new ImplementedDetection(
                this,
                Check.DataType.JAVA,
                Check.DetectionType.PACKETS,
                "positive_latency",
                true
        );
        this.positive_balance = new ImplementedDetection(
                this,
                Check.DataType.JAVA,
                Check.DetectionType.PACKETS,
                "positive_balance",
                true
        );
        this.negative = new ImplementedDetection(
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
        } else if (object instanceof PlayerTeleportEvent) {
            this.teleport();
        } else if (PluginBase.packetsEnabled()
                && object instanceof PacketEvent) {
            PacketType eventType = ((PacketEvent) object).getPacketType();

            for (PacketType type : TeleportListener.packetTypes) {
                if (type.equals(eventType)) {
                    this.teleport();
                    break;
                }
            }
        }
    }

    private void teleport() {
        this.teleported = System.currentTimeMillis()
                + (AlgebraUtils.integerCeil(TPS.maximum) * TPS.tickTime * 4L);
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
        return !this.protocol.isAFK()
                && this.teleported < System.currentTimeMillis()
                && MovementProcessing.canCheck(
                this.protocol,
                true,
                true,
                true,
                true,
                true
        );
    }
}