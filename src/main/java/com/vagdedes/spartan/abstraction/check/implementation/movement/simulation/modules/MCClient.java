package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules;

import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion.JumpEvent;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion.MotionVector;
import com.vagdedes.spartan.abstraction.player.SpartanPotionEffect;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import org.bukkit.potion.PotionEffectType;

public class MCClient {

    public SpartanProtocol protocol;
    public float jumpMovementFactor;
    public boolean isAirBorne;
    public MotionVector motion;

    public MotionVector getMotion() {
        return motion;
    }

    public void setMotion(MotionVector motion) {
        this.motion = motion;
    }

    public void setMotion(double x, double y, double z) {
        this.motion.setX(x);
        this.motion.setY(y);
        this.motion.setZ(z);
    }

    public MCClient(SpartanProtocol protocol) {
        this.protocol = protocol;
        this.isAirBorne = false;
        this.motion = new MotionVector(0, 0, 0);
        this.jumpMovementFactor = 0.0F;
    }

    public void livingTick() {
        this.jumpMovementFactor = 0.02F;

        if (protocol.isSprinting()) {
            this.jumpMovementFactor = (float) ((double) this.jumpMovementFactor + 0.005999999865889549D);
        }
    }

    protected float getJumpUpwardsMotion() {
        return 0.42F; // * this.getJumpFactor();
    }

    public void jump() {
        float f = this.getJumpUpwardsMotion();
        SpartanPotionEffect jumpEffect = protocol.spartanPlayer.getPotionEffect(PotionEffectType.JUMP, 0);

        if (jumpEffect != null) {
            f += 0.1F * (float) (jumpEffect.bukkitEffect.getAmplifier() + 1);
        }

        MotionVector vector3d = this.getMotion();
        this.setMotion(vector3d.x, (double) f, vector3d.z);

        if (this.protocol.isSprinting()) {

            JumpEvent jumpEvent = new JumpEvent(protocol.getLocation().getYaw());

            float f1 = jumpEvent.getYaw() * ((float) Math.PI / 180F);
            this.setMotion(this.getMotion().addWithClone((double) (-Math.sin(f1) * 0.2F), 0.0D, (double) (Math.cos(f1) * 0.2F)));
        }

        this.isAirBorne = true;
    }

}
