package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.player.PlayerTrackers;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class Velocity extends CheckExecutor {

    private final DetectionExecutor vertical, horizontal;

    public Velocity(Enums.HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.vertical = new ImplementedDetectionExecutor(this, "vertical", true);
        this.horizontal = new ImplementedDetectionExecutor(this, "horizontal", true);
    }

    private boolean veloV = false;
    private boolean veloH = false;
    private double verticalSended = 0;
    private double xSended = 0;
    private double zSended = 0;
    private int arrayV = 0;
    private float vl = 0;

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerVelocityEvent) {
            PlayerVelocityEvent event = (PlayerVelocityEvent) object;
            veloV = true;
            veloH = true;
            arrayV = 0;
            verticalSended = RayUtils.scaleVal(event.getVelocity().getY(), 2);
            xSended = RayUtils.scaleVal(Math.abs(event.getVelocity().getX()), 2);
            zSended = RayUtils.scaleVal(Math.abs(event.getVelocity().getZ()), 2);
        }
        if (object instanceof PlayerMoveEvent) {
            Double eventXDiff = this.protocol().spartanPlayer.movement.getEventXDiff(),
                    eventZDiff = this.protocol().spartanPlayer.movement.getEventZDiff(),
                    vertical = this.protocol().spartanPlayer.movement.getEventVertical();

            if (eventXDiff == null
                    || eventZDiff == null
                    || vertical == null) {
                return;
            }
            double x = RayUtils.scaleVal(Math.abs(eventXDiff), 2);
            double z = RayUtils.scaleVal(Math.abs(eventZDiff), 2);
            boolean blockOnTop = RayUtils.onSolidBlock(this.protocol().spartanPlayer, this.protocol().spartanPlayer.movement.getLocation().clone().add(0, 2, 0));
            vertical = RayUtils.scaleVal(vertical, 2);
            // vertical check
            if (veloV) {
                if (blockOnTop || verticalSended < 0 || vertical < 0) {
                    veloV = false;
                    return;
                }
                if (verticalSended != vertical) {
                    arrayV++;
                    if (arrayV > 2) {
                        vl += 100;
                        veloV = false;
                        if (vl > 300) {
                            this.vertical.cancel(
                                    "type: vertical, speed-vertical: "
                                            + vertical + ", sended-vertical: " + verticalSended,
                                    1,
                                    this.protocol().spartanPlayer.movement.getDetectionLocation()
                            );
                        }
                    }
                } else {
                    veloV = false;
                    if (vl > 0) vl -= 25;
                }
            }
            // horizontal check
            if (veloH) {
                if (blockOnTop) {
                    veloH = false;
                    return;
                }
                double decreace = this.protocol().spartanPlayer.protocol.isSprinting() ? 2.5 : 1.5;
                if (x < xSended / decreace && z < zSended / decreace) {
                    vl += 100;
                    if (vl > 300) {
                        this.horizontal.cancel(
                                "type: horizontal, x: " + x + ", z: " + z
                                        + ", sended-x: " + xSended + ", sended-z: " + zSended,
                                1,
                                this.protocol().spartanPlayer.movement.getDetectionLocation()
                        );
                    }
                } else {
                    if (vl > 0) vl -= 25;
                }
                veloH = false;
            }
        }
    }

    @Override
    protected boolean canRun() {
        return this.protocol().spartanPlayer.protocol.packetsEnabled()
                && !this.protocol().spartanPlayer.trackers.has(PlayerTrackers.TrackerType.BOUNCING_BLOCKS)
                && CombatUtils.canCheck(this.protocol().spartanPlayer)
                && !this.protocol().spartanPlayer.isOutsideOfTheBorder(-1.0)
                && !BlockPatternUtils.isBlockPattern(
                new double[][]{
                        {1, 0, 1},
                        {1, 1, 1},
                        {1, this.protocol().spartanPlayer.getInstance().getEyeHeight(), 1}
                },
                this.protocol().spartanPlayer.movement.getLocation(),
                true,
                BlockUtils.solid, BlockUtils.semi_solid
        );
    }

}
