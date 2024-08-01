package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules;

import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.Attribute;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.Attributes;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion.*;
import com.vagdedes.spartan.abstraction.player.SpartanPotionEffect;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.MathHelper;
import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class MCClient {

    public float jumpMovementFactor;
    public int onAir, onGroundTicks;
    public SpartanProtocol protocol;
    public org.bukkit.util.Vector veloClaim;
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

    public MCClient(SpartanProtocol protocol) {
        this.protocol = protocol;
        this.isAirBorne = false;
        this.isJumping = false;
        this.veloClaim = null;
        this.onAir = 0;
        this.onGroundTicks = 0;
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
            if (veloClaim != null) {
                this.predicted.add(this.verifiedMotion);
            } else {
                this.motion = this.verifiedMotion.cloneVector();
                tick(bind, new MotionVector(dx, dy, dz));
                this.predicted.add(this.motion);
            }
        }
        veloClaim = null;
    }

    public SpartanProtocol p() {
        return SpartanBukkit.getProtocol(this.protocol.spartanPlayer.uuid);
    }

    public void preTick(MotionVector currentMotion) {
        boolean onGround = this.p().isOnGround();
        if (!onGround) {
            onAir++;
            onGroundTicks = 0;
        } else {
            onAir = 0;
            onGroundTicks++;
            isJumping = false;
            isAirBorne = false;
        }
    }

    public boolean checkCollide() {
        Location location = this.p().getLocation();
        for (int x = location.getBlockX() - 1; x <= location.getBlockX() + 1; x++) {
            for (int y = location.getBlockY() - 1; y <= location.getBlockY() + 1; y++) {
                for (int z = location.getBlockZ() - 1; z <= location.getBlockZ() + 1; z++) {
                    if (location.getWorld().getBlockAt(x, y, z).getType().isSolid()
                                    || ignore(location.getWorld().getBlockAt(x, y, z).toString().toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public boolean checkCollideSpecify() {
        Location location = this.p().getLocation();
        for (int x = location.getBlockX() - 1; x <= location.getBlockX() + 1; x++) {
            for (int y = location.getBlockY() - 1; y <= location.getBlockY() + 1; y++) {
                for (int z = location.getBlockZ() - 1; z <= location.getBlockZ() + 1; z++) {
                    if (ignore(location.getWorld().getBlockAt(x, y, z).toString().toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public boolean checkCollideOnTop() {
        Location location = this.p().getLocation();
        World world = location.getWorld();
        double startX = location.getX() - 0.3;
        double endX = location.getX() + 0.3;
        double startY = location.getY();
        double endY = location.getY() + 2;
        double startZ = location.getZ() - 0.3;
        double endZ = location.getZ() + 0.3;
        double increment = 0.3;

        for (double x = startX; x <= endX; x += increment) {
            for (double y = startY; y <= endY; y += 1) {
                for (double z = startZ; z <= endZ; z += increment) {
                    Block block = world.getBlockAt(new Location(world, x, y, z));
                    if (block.getType().isSolid()
                                    || ignore(block.toString().toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean ignore(String block) {
        return block.matches(".*(snow|step|frame|table|slab|stair|ladder|vine|waterlily|wall|carpet|fence|rod|bed|skull|pot|hopper|door|piston|lily|slime).*");
    }
    public void tick(KeyBind keyBind, MotionVector currentMotion) {
        boolean onGround = this.p().isOnGround();
        if (!onGround) {
            { // air Y sim
                SpartanPotionEffect levitation = this.p().spartanPlayer.getPotionEffect(PotionEffectType.LEVITATION, 0);
                double d0 = 0.08D;
                boolean flag = this.getMotion().y <= 0.0D;

                if (flag && this.p().spartanPlayer.getPotionEffect(PotionEffectType.SLOW_FALLING, 0) != null) {
                    d0 = 0.01D;
                }
                Location blockpos = this.p().getLocation().clone().add(0, -1, 0);
                float f3 = BlockUtils.getSlipperiness(this.p().spartanPlayer.getWorld().getBlockAt(blockpos));
                MotionVector vector3d5 = this.revelantVec(new MotionVector(0, 0, 0), f3);
                double d2 = vector3d5.y;
                if (levitation != null) {
                    d2 += (0.05D * (double) (levitation.bukkitEffect.getAmplifier() + 1) - vector3d5.y) * 0.2D;
                    //this.fallDistance = 0.0F;
                }
                if (this.p().player.hasGravity()) {
                    d2 -= d0;
                }
                this.getMotion().y = d2 * (double) 0.98F;
                if (this.onAir == 6) {
                    /*
                    when jumping in the 6th tick, the speed change is different.
                     */
                    double mY = RayUtils.scaleVal(currentMotion.y, 4);
                    double pV = RayUtils.scaleVal(this.getMotion().getY(), 4);
                    if (keyBind.isNone()) {
                        if (mY == -0.0784 && pV == 0.0031) {
                            this.getMotion().y = -0.0784;
                        }
                        if (mY == 0.0 && pV == 0.0031) {
                            this.getMotion().y = 0.0;
                        }
                    }
                }
                if (onAir == 1 && currentMotion.y > 0 && veloClaim == null) {
                    jump();
                    isJumping = true;
                }
                if (this.checkCollideOnTop()) {
                    if (keyBind.isNone()) this.getMotion().y = -0.0784;
                    if (keyBind.is(true, false, false, false)
                                    && this.getMotion().y > currentMotion.y) {
                        this.getMotion().y = RayUtils.scaleVal(currentMotion.getY(), 4);
                    }
                }
            }
        } else {
            if (!this.p().isOnGroundFrom() && currentMotion.y < this.motion.y
                    && Math.abs(currentMotion.y - this.motion.y) < 0.03) {
                this.motion.y = currentMotion.y;
            } else if (!this.p().isOnGroundFrom() && currentMotion.y > this.motion.y) {
                /*
                Let's prevent lagged block falses by allowing moving
                through the blocks at the bottom,
                because it's going to teleport them back anyway.
                 */
                this.motion.y = currentMotion.getY();
            } else {
                this.getMotion().y = 0;
            }
            if (keyBind.is(false, true, false, false)) {
                if (this.getMotion().y == 0 && onGroundTicks < 3
                                && RayUtils.scaleVal(currentMotion.y, 4) == -0.1216) {
                    this.getMotion().y = RayUtils.scaleVal(currentMotion.getY(), 4);
                }
            }
        }

        /*
        Specify actions
        Like collision
         */
        if (checkCollideSpecify() && keyBind.isNone()) {
            if (Math.abs(this.getMotion().y - currentMotion.y) <= 0.5) {
                this.getMotion().y = RayUtils.scaleVal(currentMotion.getY(), 4);
            }
        }


    }

    protected float getJumpUpwardsMotion() {
        return 0.42F; // * this.getJumpFactor();
    }

    public List<MotionVector> getPredicted() {
        return this.predicted;
    }

    public MotionVector revelantVec(MotionVector vector, float v) {
        this.moveRelative(this.getRelevantMoveFactor(v), vector);
        this.setMotion(this.handleOnClimbable(this.getMotion()));
        this.move(MoverType.SELF, this.getMotion());
        MotionVector vector3d = this.getMotion();

        if (this.isJumping && this.canClimb(this.p())) {
            vector3d = new MotionVector(vector3d.x, 0.2D, vector3d.z);
        }

        return vector3d;
    }

    private MotionVector handleOnClimbable(MotionVector vector) {
        if (this.canClimb(this.p())) {
            float f = 0.15F;
            double d0 = MathHelper.clamp_double(vector.x, (double) -0.15F, (double) 0.15F);
            double d1 = MathHelper.clamp_double(vector.z, (double) -0.15F, (double) 0.15F);
            double d2 = Math.max(vector.y, (double) -0.15F);

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

    private float getRelevantMoveFactor(float p_213335_1_) {
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
        PotionEffect jumpEffect = this.p().spartanPlayer.getInstance().getPotionEffect(PotionEffectType.JUMP);

        if (jumpEffect != null) {
            f += 0.1F * (float) (jumpEffect.getAmplifier() + 1);
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

    public void claimVelocity(org.bukkit.util.Vector velocity) {
        this.verifiedMotion.set(velocity.getX(), velocity.getY(), velocity.getZ());
        this.veloClaim = velocity;
    }

}
