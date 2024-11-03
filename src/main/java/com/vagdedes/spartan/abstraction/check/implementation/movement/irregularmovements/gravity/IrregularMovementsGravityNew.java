package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.gravity;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPotionEffect;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;

public class IrregularMovementsGravityNew extends DetectionExecutor {

    private int air = 0;

    public IrregularMovementsGravityNew(CheckExecutor executor) {
        super(executor, "gravity_new", true);
    }

    public void run() {
        if (protocol.spartanPlayer == null) return;
        SpartanProtocol p = protocol.spartanPlayer.protocol;
        boolean isOnGround = p.isOnGround();
        Location to = p.getLocation();
        Location from = protocol.spartanPlayer.movement.getEventFromLocation().getBukkitLocation();
        double motion = to.getY() - from.getY();
        double f = 0.0784;
        this.air = (isOnGround) ? 0 : air + 1;

        {
            if (isOnGround) {
                protocol.spartanPlayer.movement.motionY = 0;
            } else  if (air == 1) {
                if (motion > 0) {
                    protocol.spartanPlayer.movement.motionY = jumpMotion();
                } else {
                    protocol.spartanPlayer.movement.motionY = -f;
                }
            } else {
                protocol.spartanPlayer.movement.motionY -= f;
            }
        }

        //player.getInstance().sendMessage("m: " + motion + " " + "p: " + player.movement.motionY);

    }

    public double jumpMotion() {
        double f = 0.42;
        SpartanPotionEffect jumpEffect = null;
        if (PotionEffectType.getByName("JUMP_BOOST") != null) {
            jumpEffect = protocol.spartanPlayer.getPotionEffect(PotionEffectType.getByName("JUMP_BOOST"), 1L);
        } else {
            jumpEffect = protocol.spartanPlayer.getPotionEffect(PotionEffectType.getByName("JUMP"), 1L);
        }

        if (jumpEffect != null) {
            f += 0.1 * (float) (jumpEffect.bukkitEffect.getAmplifier() + 1);
        }

        return f;
    }

}
