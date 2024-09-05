package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class Velocity extends CheckExecutor {

    public Velocity(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0, false);
    }

    private boolean veloV = false;
    private boolean veloH = false;
    private double verticalSended = 0;
    private double xSended = 0;
    private double zSended = 0;
    private int arrayH = 0;
    private int arrayV = 0;
    private float vl = 0;

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerVelocityEvent) {
            PlayerVelocityEvent event = (PlayerVelocityEvent) object;
            veloV = true;
            veloH = true;
            arrayV = 0;
            arrayH = 0;
            verticalSended = RayUtils.scaleVal(event.getVelocity().getY(), 2);
            xSended = RayUtils.scaleVal(Math.abs(event.getVelocity().getX()), 2);
            zSended = RayUtils.scaleVal(Math.abs(event.getVelocity().getZ()), 2);
        }
        if (object instanceof PlayerMoveEvent) {
            double x = RayUtils.scaleVal(Math.abs(player.movement.getEventXDiff()), 2);
            double z = RayUtils.scaleVal(Math.abs(player.movement.getEventXDiff()), 2);
            boolean blockOnTop = RayUtils.onSolidBlock(player, player.movement.getLocation().clone().add(0, 2, 0));
            double vertical = RayUtils.scaleVal(player.movement.getEventVertical(), 2);
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
                            cancel(
                                    "type: vertical, speed-vertical: "
                                            + vertical + ", sended-vertical: " + verticalSended,
                                    1,
                                    player.movement.getDetectionLocation()
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
                double decreace = player.protocol.isSprinting() ? 2.5 : 1.5;
                if (x < xSended / decreace && z < zSended / decreace) {
                    vl += 100;
                    if (vl > 300) {
                        cancel(
                                "type: horizontal, x: " + x + ", z: " + z
                                        + ", sended-x: " + xSended + ", sended-z: " + zSended,
                                1,
                                player.movement.getDetectionLocation()
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
    protected void cannotHandle(boolean cancelled, Object object) {
        veloV = false;
        veloH = false;
        arrayV = 0;
        arrayH = 0;
        verticalSended = 0;
        xSended = 0;
        zSended = 0;
    }


    @Override
    protected boolean canRun() {
        return player.protocol.packetsEnabled()
                && CombatProcessing.canCheck(player)
                && !player.isOutsideOfTheBorder(-1.0)
                && !BlockPatternUtils.isBlockPattern(
                new double[][]{
                        {1, 0, 1},
                        {1, 1, 1},
                        {1, player.getInstance().getEyeHeight(), 1}
                },
                player.movement.getLocation(),
                true,
                BlockUtils.solid, BlockUtils.semi_solid
        );
    }

}
