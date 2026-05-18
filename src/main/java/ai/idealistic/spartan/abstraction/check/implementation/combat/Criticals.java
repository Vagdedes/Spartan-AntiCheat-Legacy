package ai.idealistic.spartan.abstraction.check.implementation.combat;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckEnums.HackType;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.spartan.abstraction.event.PlayerAttackEvent;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.abstraction.world.ServerLocation;
import ai.idealistic.spartan.functionality.tracking.CheckConditions;
import ai.idealistic.spartan.utils.math.RayUtils;
import ai.idealistic.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.potion.PotionEffectType;

public class Criticals extends CheckRunner {

    private final CheckDetection detection;
    private int buffer = 0; // Injected buffer to prevent 1-tick physics false positives

    public Criticals(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.detection = new ImplementedDetection(this, null, null, "packet", true,
                CheckDetection.DEFAULT_AVERAGE_TIME * 4L,
                CheckDetection.TIME_TO_NOTIFY,
                CheckDetection.TIME_TO_PREVENT,
                CheckDetection.TIME_TO_PUNISH);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerAttackEvent) {
            this.detection.call(
                    () -> {
                        double deltaY = this.protocol.getLocation().getY() - this.protocol.getFromLocation().getY();
                        double absDeltaY = Math.abs(deltaY);

                        // Extended Exemptions: Webs AND Climbables (Ladders/Vines)
                        if (RayUtils.onBlock(this.protocol, this.protocol.getLocationOrVehicle(), BlockUtils.web) ||
                                this.protocol.getEnvironment().isClimb()) {
                            return;
                        }

                        boolean isGroundToAir = !this.protocol.isOnGround() && this.protocol.isOnGroundFrom();
                        boolean hasCeiling = RayUtils.onSolidBlock(this.protocol, this.protocol.getLocation().clone().add(0, 2, 0)) ||
                                new ServerLocation(this.protocol.getLocation()).getBlock(this.protocol).getType().isBlock();

                        if (!hasCeiling) {
                            // Detection 1: The "Zero-Y" Spoof
                            // Client claims to be off the ground, but vertical motion is completely static
                            if (!this.protocol.isOnGround() && deltaY == 0.0 && !this.protocol.isOnGroundFrom()) {
                                if (++buffer > 1) {
                                    this.detection.cancel("type: zero-y_spoof");
                                }
                            }

                            // Detection 2: Micro-Hops & Magic Offsets
                            // Catches standard fractions used by modern bypasses (0.0625, 0.046875, 0.015625)
                            else if (isGroundToAir && deltaY > 0) {
                                if (deltaY < 0.1 || absDeltaY == 0.0625 || absDeltaY == 0.015625 || absDeltaY == 0.046875) {
                                    if (++buffer > 1) {
                                        this.detection.cancel("type: micro_hop, val: " + deltaY);
                                    }
                                } else {
                                    this.decreaseBuffer();
                                }
                            } else {
                                this.decreaseBuffer();
                            }
                        } else {
                            this.decreaseBuffer();
                        }
                    },
                    () -> this.buffer = 0
            );
        }
    }

    private void decreaseBuffer() {
        if (this.buffer > 0) {
            this.buffer--;
        }
    }

    @Override
    protected boolean canRun() {
        return CheckConditions.canCheckCombat(this.protocol)
                && !this.protocol.getEnvironment().isLiquid()
                && !this.protocol.hasPotionEffect(PotionEffectType.BLINDNESS, 0)
                && !this.protocol.hasPotionEffect(PotionEffectType.LEVITATION, 0); // Added Levitation exemption
    }
}