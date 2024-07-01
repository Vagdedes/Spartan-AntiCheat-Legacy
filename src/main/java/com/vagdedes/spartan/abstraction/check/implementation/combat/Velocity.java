package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.math.RayUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
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
        if (!SpartanBukkit.packetsEnabled()) return;
        if (blockCause(player)) return;
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
            PlayerMoveEvent event = (PlayerMoveEvent) object;
            SpartanLocation to = player.movement.getEventToLocation();
            SpartanLocation from = player.movement.getEventFromLocation();
            double x = RayUtils.scaleVal(Math.abs(to.getX() - from.getX()), 2);
            double z = RayUtils.scaleVal(Math.abs(to.getZ() - from.getZ()), 2);
            double diffX = Math.abs(x - xSended);
            double diffZ = Math.abs(z - zSended);
            boolean blockOnTop = RayUtils.onSolidBlock(player, to.clone().add(0, 2, 0));
            double vertical = RayUtils.scaleVal(to.getY() - from.getY(), 2);
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
                            cancel("type: vertical, speed-vertical: "
                                    + vertical + ", sended-vertical: " + verticalSended, 1, from);
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
                        cancel("type: horizontal, x: " + x + ", z: " + z
                                + ", sended-x: " + xSended + ", sended-z: " + zSended, 1, from);
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
        return CombatProcessing.canCheck(player)
                && !player.isOutsideOfTheBorder(-1.0);
    }

    private boolean blockCause(SpartanPlayer player) {
        SpartanLocation location = player.movement.getRawLocation();

        if (location.getBlock().material != Material.AIR) {
            return true;
        }
        for (int x = location.getBlockX() - 1; x <= location.getBlockX() + 1; x++) {
            for (int y = location.getBlockY(); y <= location.getBlockY() + 2; y++) {
                for (int z = location.getBlockZ() - 1; z <= location.getBlockZ() + 1; z++) {
                    if (!new SpartanLocation(
                            location.world,
                            location.getX(),
                            location.getY(),
                            location.getZ(),
                            0.0f,
                            0.0f
                    ).getBlock().material.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
