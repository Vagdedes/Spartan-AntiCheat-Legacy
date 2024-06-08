package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.math.RayUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class Velocity extends CheckExecutor {

    public Velocity(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
    }

    private boolean velo = false;
    private double verticalSended = 0;
    private int array = 0;
    private float vl = 0;

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerVelocityEvent) {
            PlayerVelocityEvent event = (PlayerVelocityEvent) object;
            velo = true;
            array = 0;
            verticalSended = RayUtils.scaleVal(event.getVelocity().getY(), 2);
        }
        if (object instanceof PlayerMoveEvent) {
            PlayerMoveEvent event = (PlayerMoveEvent) object;
            SpartanLocation to = player.movement.getEventToLocation();
            SpartanLocation from = player.movement.getEventFromLocation();
            boolean blockOnTop = RayUtils.onSolidBlock(player, to.clone().add(0, 2, 0));
            double vertical = RayUtils.scaleVal(to.getY() - from.getY(), 2);
            if (velo) {
                if (blockOnTop || verticalSended < 0 || vertical < 0) {
                    velo = false;
                    return;
                }
                if (vertical != verticalSended) {
                    array++;
                    if (array > 2) {
                        vl += 100;
                        velo = false;
                        if (vl > 200) {
                            cancel("type: vertical, speed-vertical: "
                                            + vertical + ", sended-vertical: " + verticalSended, 1, from);
                        }
                    }
                } else {
                    velo = false;
                    if (vl > 0) vl -= 25;
                }
            }
        }
    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player)
                && !player.isOutsideOfTheBorder(-1.0);
    }
}
