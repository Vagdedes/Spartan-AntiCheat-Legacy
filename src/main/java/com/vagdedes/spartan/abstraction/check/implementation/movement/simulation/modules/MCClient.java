package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules;

import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.Attribute;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.Attributes;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion.*;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.player.SpartanPotionEffect;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.MathHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;

public class MCClient {

    public float jumpMovementFactor;
    public int onAir;
    public SpartanPlayer player;
    private final List<MotionVector> predicted;
    public boolean isAirBorne, isJumping;
    public MotionVector motion;
    public MotionVector verifiedMotion;
    private final List<MotionVector> vectors = new ArrayList<>();
    private final List<KeyBind> binds = new ArrayList<>();

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

    public MCClient(SpartanPlayer player) {
        this.player = player;
        this.isAirBorne = false;
        this.isJumping = false;
        this.onAir = 0;
        this.motion = new MotionVector(0, 0, 0);
        this.jumpMovementFactor = 0.0F;
        this.predicted = new ArrayList<>();
        this.verifiedMotion = new MotionVector(0, 0, 0);
        double[] values = {0.9800000190734863, 0.2940000295639038};

        for (double x : values) {
            for (double z : values) {
                vectors.add(new MotionVector(x, 0, z));
                vectors.add(new MotionVector(x, 0, -z));
                vectors.add(new MotionVector(-x, 0, z));
                vectors.add(new MotionVector(-x, 0, -z));
                vectors.add(new MotionVector(x, 0, 0));
                vectors.add(new MotionVector(-x, 0, 0));
                vectors.add(new MotionVector(0, 0, z));
                vectors.add(new MotionVector(0, 0, -z));
            }
        }
        vectors.add(new MotionVector(0, 0, 0));
        binds.add(new KeyBind(true, false, false, false));
        binds.add(new KeyBind(false, true, false, false));
        binds.add(new KeyBind(true, false, true, false));
        binds.add(new KeyBind(true, false, false, true));
        binds.add(new KeyBind(false, true, true, false));
        binds.add(new KeyBind(false, true, false, true));
        binds.add(new KeyBind(false, false, true, false));
        binds.add(new KeyBind(false, false, false, true));
        binds.add(new KeyBind(false, false, false, false));
    }
    public void movement(double dx, double dy, double dz) {
        this.predicted.clear();
        preTick(new MotionVector(dx, dy, dz));
        for (KeyBind bind : binds) {
            this.motion = this.verifiedMotion.cloneVector();
            tick(bind, new MotionVector(dx, dy, dz));
            this.predicted.add(this.motion);
        }
    }
    public SpartanProtocol p() {
        return SpartanBukkit.getProtocol(this.player.uuid);
    }
    public void preTick(MotionVector currentMotion) {
        boolean onGround = this.p().isOnGround();
        if (!onGround) {
            onAir++;
        } else {
            onAir = 0;
            isJumping = false;
            isAirBorne = false;
        }
    }

    public void tick(KeyBind keyBind, MotionVector currentMotion) {
        boolean onGround = this.p().isOnGround();
        if (!onGround) {
            { // air Y sim
                SpartanPotionEffect levitation = this.p().spartanPlayer.getPotionEffect(PotionEffectType.LEVITATION, 0);
                double d0 = 0.08D;
                boolean flag = this.getMotion().y <= 0.0D;

                if (flag && this.p().spartanPlayer.getPotionEffect(PotionEffectType.SLOW_FALLING, 0) != null)
                {
                    d0 = 0.01D;
                }
                Location blockpos = this.p().getLocation().clone().add(0, -1, 0);
                float f3 = BlockUtils.getSlipperiness(this.p().spartanPlayer.getWorld().getBlockAt(blockpos));
                MotionVector vector3d5 = this.revelantVec(new MotionVector(0, 0, 0), f3);
                double d2 = vector3d5.y;
                if (levitation != null)
                {
                    d2 += (0.05D * (double)(levitation.bukkitEffect.getAmplifier() + 1) - vector3d5.y) * 0.2D;
                    //this.fallDistance = 0.0F;
                }
                if (this.p().player.hasGravity())
                {
                    d2 -= d0;
                }
                this.getMotion().y = d2 * (double)0.98F;
                if (onAir == 1 && currentMotion.y > 0) {
                    jump();
                    isJumping = true;
                }
            }
        } else {
            this.getMotion().y = 0;
        }


    }

    public void livingTick(MotionVector travelVector) {
        SpartanPotionEffect levitation = this.p().spartanPlayer.getPotionEffect(PotionEffectType.LEVITATION, 0);
        this.jumpMovementFactor = 0.02F;

        if (this.p().isSprinting()) {
            this.jumpMovementFactor = (float) ((double) this.jumpMovementFactor + 0.005999999865889549D);
        }
        double d0 = 0.08D;
        boolean flag = this.getMotion().y <= 0.0D;

        if (flag && this.p().spartanPlayer.getPotionEffect(PotionEffectType.SLOW_FALLING, 0) != null)
        {
            d0 = 0.01D;
        }
        Location blockpos = this.p().getLocation().clone().add(0, -1, 0);
        float f3 = BlockUtils.getSlipperiness(this.p().spartanPlayer.getWorld().getBlockAt(blockpos));
        float f4 = this.p().isOnGround() ? f3 * 0.91F : 0.91F;
        MotionVector vector3d5 = this.revelantVec(travelVector, f3);
        double d2 = vector3d5.y;

        if (levitation != null)
        {
            d2 += (0.05D * (double)(levitation.bukkitEffect.getAmplifier() + 1) - vector3d5.y) * 0.2D;
            //this.fallDistance = 0.0F;
        }
        else if (this.p().player.hasGravity())
        {
           d2 -= d0;
        }

        this.setMotion(vector3d5.x * (double)f4, d2 * (double)0.98F, vector3d5.z * (double)f4);


    }

    protected float getJumpUpwardsMotion() {
        return 0.42F; // * this.getJumpFactor();
    }
    public List<MotionVector> getPredicted() {
        return this.predicted;
    }
    public MotionVector revelantVec(MotionVector vector, float v)
    {
        this.moveRelative(this.getRelevantMoveFactor(v), vector);
        this.setMotion(this.handleOnClimbable(this.getMotion()));
        this.move(MoverType.SELF, this.getMotion());
        MotionVector vector3d = this.getMotion();

        if (this.isJumping && this.canClimb(this.p()))
        {
            vector3d = new MotionVector(vector3d.x, 0.2D, vector3d.z);
        }

        return vector3d;
    }
    private MotionVector handleOnClimbable(MotionVector vector)
    {
        if (this.canClimb(this.p()))
        {
            float f = 0.15F;
            double d0 = MathHelper.clamp_double(vector.x, (double) - 0.15F, (double)0.15F);
            double d1 = MathHelper.clamp_double(vector.z, (double) - 0.15F, (double)0.15F);
            double d2 = Math.max(vector.y, (double) - 0.15F);

            vector = new MotionVector(d0, d2, d1);
        }

        return vector;
    }
    private boolean canClimb(SpartanLocation location) {
        Material material = location.getBlock().material;
        return com.vagdedes.spartan.utils.minecraft.world.BlockUtils.canClimb(material, false)
                        || com.vagdedes.spartan.utils.minecraft.world.BlockUtils.areTrapdoors(material)
                        && (com.vagdedes.spartan.utils.minecraft.world.BlockUtils.canClimb(location.clone().add(0, 1, 0).getBlock().material, false)
                        || com.vagdedes.spartan.utils.minecraft.world.BlockUtils.canClimb(location.clone().add(0, -1, 0).getBlock().material, false));
    }
    private boolean canClimb(SpartanProtocol protocol) {
        SpartanLocation location = new SpartanLocation(protocol.getLocation());
        Material material = location.getBlock().material;
        return com.vagdedes.spartan.utils.minecraft.world.BlockUtils.canClimb(material, false)
                        || com.vagdedes.spartan.utils.minecraft.world.BlockUtils.areTrapdoors(material)
                        && (com.vagdedes.spartan.utils.minecraft.world.BlockUtils.canClimb(location.clone().add(0, 1, 0).getBlock().material, false)
                        || com.vagdedes.spartan.utils.minecraft.world.BlockUtils.canClimb(location.clone().add(0, -1, 0).getBlock().material, false));
    }
    private float getRelevantMoveFactor(float p_213335_1_)
    {
        return this.p().isOnGround() ?
                        (float) Attributes.getAttributeValue(Attribute.MOVEMENT_SPEED, this.p())
                                        * (0.21600002F / (p_213335_1_ * p_213335_1_ * p_213335_1_))
                        : this.jumpMovementFactor;
    }
    public void move(MoverType typeIn, MotionVector pos) {
        // for now - nothing
    }
    public void moveRelative(float friction, MotionVector relative) {
        StrafeEvent event = new StrafeEvent(friction, relative, this.p().getLocation().getYaw());
        MotionVector vector3d = getAbsoluteMotion(event.getRelative(), event.getFriction(), event.getYaw());
        this.getMotion().add(vector3d.x, vector3d.y, vector3d.z);
    }

    private static MotionVector getAbsoluteMotion(MotionVector relative, float friction, float yaw) {
        double d0 = relative.lengthSquared();

        if (d0 < 1.0E-7D) {
            return MotionVector.ZERO;
        } else {
            MotionVector vector3d = (d0 > 1.0D ? relative.normalize() : relative).scale(friction);
            float f = MathHelper.sin(yaw * ((float) Math.PI / 180F));
            float f1 = MathHelper.cos(yaw * ((float) Math.PI / 180F));
            return new MotionVector(vector3d.x * (double) f1 - vector3d.z * (double) f, vector3d.y, vector3d.z * (double) f1 + vector3d.x * (double) f);
        }
    }

    public void jump() {
        float f = this.getJumpUpwardsMotion();
        SpartanPotionEffect jumpEffect = this.p().spartanPlayer.getPotionEffect(PotionEffectType.JUMP, 0);

        if (jumpEffect != null) {
            f += 0.1F * (float) (jumpEffect.bukkitEffect.getAmplifier() + 1);
        }

        MotionVector vector3d = this.getMotion();
        this.setMotion(vector3d.x, (double) f, vector3d.z);

        if (this.p().isSprinting()) {

            JumpEvent jumpEvent = new JumpEvent(this.p().getLocation().getYaw());

            float f1 = jumpEvent.getYaw() * ((float) Math.PI / 180F);
            this.getMotion().add((double) (-Math.sin(f1) * 0.2F), 0.0D, (double) (Math.cos(f1) * 0.2F));
        }

        this.isAirBorne = true;
    }

}
