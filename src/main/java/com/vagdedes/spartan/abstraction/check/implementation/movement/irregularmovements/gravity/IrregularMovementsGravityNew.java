package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.gravity;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPotionEffect;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.listeners.bukkit.standalone.chunks.Event_Chunks;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.potion.PotionEffectType;

public class IrregularMovementsGravityNew extends DetectionExecutor {

    private int air = 0, ground = 0, top = 0, topReset = 0;
    private boolean slime = false;

    private double vl = 0, fallDistance = 0;

    public IrregularMovementsGravityNew(CheckExecutor executor) {
        super(executor, "gravity_new", true);
    }

    public void run() {
        if (this.protocol().spartanPlayer == null) return;
        SpartanProtocol p = this.protocol().spartanPlayer.protocol;
        boolean isOnGround = p.isOnGround();
        Location to = p.getLocation();
        Location from = this.protocol().spartanPlayer.movement.getEventFromLocation().getBukkitLocation();

        // Another Mc dev skill issue.
        if (to.getX() == from.getX() && to.getY() == from.getY() && to.getZ() == from.getZ()) {
            return;
        }

        double motion = to.getY() - from.getY();
        double gravity = 0.08;
        double jumpStartMotionY = jumpMotion();
        this.air = (isOnGround) ? 0 : air + 1;
        this.ground = (isOnGround) ? this.ground + 1 : 0;

        boolean isOnWater = this.isOnWater(p.getLocation());

        // 0.98 must be the Minecraft developers'
        // favorite number because they have down syndrome 46 / 0.98 = 47~
        if (isOnGround) {
            if (this.ground == 1) {

                double
                pr = this.protocol().spartanPlayer.movement.motionY -= gravity;
                pr *= 0.98F;
                if (motion > pr && motion < 0) {
                    this.setPredict(motion);
                } else {
                    this.setPredict(0);
                }

                if (isOnSlime(to)) slime = true;
            } else {
                this.setPredict(0);
                if (this.abs(motion) < 0.08 && this.abs(motion) > 0 &&
                                (this.isBlocksOnTop(to)
                                || this.isOnSlime(to))) {
                    this.setPredict(motion);
                }
            }

            if (this.isOnSemi(to) && motion != 0 && motion < 0.785) {
                this.setPredict(motion);
            }
        } else if (this.air == 1) {
            //protocol().player.sendMessage("m: " + motion + " " + fallDistance);
            if (motion > 0) {
                this.setPredict((float) jumpStartMotionY);
            } else {
                this.setPredict(-gravity * 0.98F);
            }
            if (slime) {
                double pr2 = this.jumpMotion() + (this.abs(fallDistance) * 0.09);
                if (motion == 0.0) {
                    this.setPredict(0.0);
                } else if (pr2 > motion) {
                    this.setPredict(motion);
                }
            }
        } else {
            this.protocol().spartanPlayer.movement.motionY -= gravity;
            this.protocol().spartanPlayer.movement.motionY *= 0.98F;
            if (this.protocol().spartanPlayer.movement.motionY < -3.92) { // Terminal velocity in Minecraft
                this.protocol().spartanPlayer.movement.motionY = -3.92;
            }
            slime = false;
        }

        // Velocity claiming
        if (this.protocol().claimedVeloGravity != null
                        && this.r(motion) == r(this.protocol().claimedVeloGravity.getVelocity().getY())) {
            this.setPredict(this.protocol().claimedVeloGravity.getVelocity().getY());
            this.protocol().claimedVeloGravity = null;
        }

        double dev1 = abs(abs(motion) - abs(predictY()));

        if (dev1 > 0.008 && isBlocksOnTop(to)) {
            if (motion < this.predictY() && motion > -0.08) {
                this.setPredict(motion);
                this.top = 2;
                this.topReset = 0;
            }
        } else if (dev1 > 0.008 && top > 0) {
            if (motion < this.predictY() && motion > -0.08) {
                this.setPredict(motion);
                this.top--;
                this.topReset = 0;
            }
        } else if (this.topReset > 18 && this.top > 0) {
            this.top--; this.topReset = 0;
        }
        if (dev1 > 0.01 && isOnWater && motion > -3.92) {
            if (motion < 0.41) {
                this.setPredict(motion);
            }
        }


        if (this.protocol().player.isFlying())
            this.setPredict(motion);

        if (!isOnGround) {
            if (this.air == 1) {
                if (motion < 0)
                    fallDistance = motion;
            } else {
                if (motion < 0)
                    fallDistance += motion;
                else fallDistance = 0;
            }
        }

        //protocol().player.sendMessage("m: " + motion + " p: " + protocol().spartanPlayer.movement.motionY + " air: " + air + " isOnGround: " + isOnGround);

        double devTotal = abs(abs(motion) - abs(predictY()));
        if (devTotal > 0.009) {
            this.vl += devTotal;
            if (this.vl > 0.09) {
                StringBuilder alertBuilder = new StringBuilder(String.valueOf(RayUtils.scaleVal(devTotal, 6)));
                if (devTotal < 1) alertBuilder.deleteCharAt(0);
                this.cancel("simulation[y], " + alertBuilder);
                //this.protocol().player.sendMessage("hacks: " + devTotal);
            }
            this.setPredict(motion);
            this.vl /= 1.3;
        } else {
            this.topReset++;
            this.vl /= 1.01;
            if (this.topReset == 48)
                this.topReset = 0;
        }
        //protocol().player.sendMessage("vl: " + vl);
    }

    public double jumpMotion() {
        double jumpPower = 0.42; // Default jump height in Minecraft
        SpartanPotionEffect jumpEffect = null;

        if (PotionEffectType.getByName("JUMP_BOOST") != null) {
            jumpEffect = this.protocol().spartanPlayer.getPotionEffect(PotionEffectType.getByName("JUMP_BOOST"), 1L);
        } else {
            jumpEffect = this.protocol().spartanPlayer.getPotionEffect(PotionEffectType.getByName("JUMP"), 1L);
        }

        if (jumpEffect != null) {
            jumpPower += 0.1 * (jumpEffect.bukkitEffect.getAmplifier() + 1);
        }

        return jumpPower;
    }

    private double r(double v) {
        return RayUtils.scaleVal(v, 1);
    }
    private double abs(double v) {
        return Math.abs(v);
    }
    private double predictY() {
        return this.protocol().spartanPlayer.movement.motionY;
    }
    private void setPredict(double v) {
        this.protocol().spartanPlayer.movement.motionY = v;
    }

    private boolean isOnWater(Location location) {
        double x = location.getX(),
                        y = location.getY() - 0.1,
                        z = location.getZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block block = getBlockAsync(
                                    new Location(
                                                    this.protocol().spartanPlayer.getWorld(),
                                                    x + (dx * 0.3),
                                                    y + (dy * 0.5),
                                                    z + (dz * 0.3))
                    );
                    Material material = block != null ? block.getType() : null;

                    if (material == null) {
                        continue;
                    }
                    if (BlockUtils.isLiquidOrWaterLogged(block, true)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOnSemi(Location location) {
        double x = location.getX(),
                        y = location.getY() - 0.1,
                        z = location.getZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block block = getBlockAsync(
                                    new Location(
                                                    this.protocol().spartanPlayer.getWorld(),
                                                    x + (dx * 0.3),
                                                    y + (dy * 0.5),
                                                    z + (dz * 0.3))
                    );
                    Material material = block != null ? block.getType() : null;

                    if (material == null) {
                        continue;
                    }
                    if (BlockUtils.isSemiSolid(material)
                                    || BlockUtils.areSlimeBlocks(material)
                                    || BlockUtils.areHoneyBlocks(material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOnSlime(Location location) {
        double x = location.getX(),
                        y = location.getY() - 0.1,
                        z = location.getZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block block = getBlockAsync(
                                    new Location(
                                                    this.protocol().spartanPlayer.getWorld(),
                                                    x + (dx * 0.3),
                                                    y + (dy * 0.5),
                                                    z + (dz * 0.3))
                    );
                    Material material = block != null ? block.getType() : null;

                    if (material == null) {
                        continue;
                    }
                    if (BlockUtils.areHoneyBlocks(material) || BlockUtils.areSlimeBlocks(material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isBlocksOnTop(Location location) {
        double x = location.getX(),
                        y = location.getY() - 0.1,
                        z = location.getZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (double dy = 1; dy <= 2.0; dy += 0.5) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block block = getBlockAsync(
                                    new Location(
                                                    this.protocol().spartanPlayer.getWorld(),
                                                    x + (dx * 0.3),
                                                    y + dy,
                                                    z + (dz * 0.3))
                    );
                    Material material = block != null ? block.getType() : null;

                    if (material == null) {
                        continue;
                    }
                    if (BlockUtils.isSemiSolid(material) || material.isSolid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Block getBlockAsync(final Location location) {
        if (Event_Chunks.isLoaded(
                        location.getWorld(),
                        location.getBlockX() >> 4,
                        location.getBlockZ() >> 4
        )) {
            return location.getWorld().getBlockAt(location);
        } else {
            return null;
        }
    }
}
