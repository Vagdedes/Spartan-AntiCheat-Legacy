package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.gravity;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.protocol.PlayerMovement;
import com.vagdedes.spartan.abstraction.protocol.ExtendedPotionEffect;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.MultiVersion.MCVersion;
import com.vagdedes.spartan.listeners.bukkit.standalone.chunks.Event_Chunks;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.potion.PotionEffectType;

public class IrregularMovementsGravityNew extends DetectionExecutor {
    private int air = 0;
    private int ground = 0;
    private int top = 0;
    private int topReset = 0;
    private int levitation = 0;
    private int slowFalling = 0;
    private int levitationA = 0;
    private int slowFallingA = 0;
    private boolean slime = false;
    private double vl = 0.0;
    private double fallDistance = 0.0;

    public IrregularMovementsGravityNew(CheckExecutor executor) {
        super(executor, "gravity_new", true);
    }

    public void run() {
        if (true) return;
        if (this.protocol().spartan != null) {
            SpartanProtocol p = this.protocol();
            boolean isOnGround = p.isOnGround();
            Location to = p.getLocation();
            Location from = this.protocol().spartan.movement.getEventFromLocation().bukkit();
            if (to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ()) {
                double motion = to.getY() - from.getY();
                double gravity = 0.08;
                double jumpStartMotionY = this.jumpMotion();
                this.air = isOnGround ? 0 : this.air + 1;
                this.ground = isOnGround ? this.ground + 1 : 0;
                boolean isOnWater = this.isOnWater(p.getLocation());
                double pr2;
                PlayerMovement playerMovement;
                if (isOnGround) {
                    if (this.ground != 1) {
                        this.setPredict(0.0);
                        if (this.abs(motion) < 0.08 && this.abs(motion) > 0.0 && (this.isBlocksOnTop(to) || this.isOnSlime(to))) {
                            this.setPredict(motion);
                        }
                    } else {
                        playerMovement = this.protocol().spartan.movement;
                        pr2 = playerMovement.motionY -= gravity;
                        pr2 *= 0.9800000190734863;
                        if (motion > pr2 && motion < 0.0) {
                            this.setPredict(motion);
                        } else {
                            this.setPredict(0.0);
                        }

                        if (this.isOnSlime(to)) {
                            this.slime = true;
                        }
                    }

                    if (this.isOnSemi(to) && motion != 0.0 && motion < 0.785) {
                        this.setPredict(motion);
                    }
                } else if (this.air == 1) {
                    if (motion > 0.0) {
                        this.setPredict((double)((float)jumpStartMotionY));
                    } else {
                        this.setPredict(-gravity * 0.9800000190734863);
                    }

                    if (this.slime) {
                        pr2 = this.jumpMotion() + this.abs(this.fallDistance) * 0.09;
                        if (motion == 0.0) {
                            this.setPredict(0.0);
                        } else if (pr2 > motion) {
                            this.setPredict(motion);
                        }
                    }
                } else {
                    playerMovement = this.protocol().spartan.movement;
                    playerMovement.motionY -= gravity;
                    playerMovement = this.protocol().spartan.movement;
                    playerMovement.motionY *= 0.9800000190734863;
                    if (this.protocol().spartan.movement.motionY < -3.92) {
                        this.protocol().spartan.movement.motionY = -3.92;
                    }

                    this.slime = false;
                }

                pr2 = this.abs(this.abs(motion) - this.abs(this.predictY()));
                if (this.protocol().claimedVeloGravity != null && pr2 > 0.001 && this.r(motion) == this.r(this.protocol().claimedVeloGravity.getVelocity().getY())) {
                    this.setPredict(this.protocol().claimedVeloGravity.getVelocity().getY());
                    this.protocol().claimedVeloGravity = null;
                }

                double dev1 = this.abs(this.abs(motion) - this.abs(this.predictY()));
                if (dev1 > 0.008 && this.isBlocksOnTop(to)) {
                    if (motion < this.predictY() && motion > -0.08) {
                        this.setPredict(motion);
                        this.top = 2;
                        this.topReset = 0;
                    }
                } else if (dev1 > 0.008 && this.top > 0) {
                    if (motion < this.predictY() && motion > -0.08) {
                        this.setPredict(motion);
                        --this.top;
                        this.topReset = 0;
                    }
                } else if (this.topReset > 18 && this.top > 0) {
                    --this.top;
                    this.topReset = 0;
                }

                if (dev1 > 0.01 && isOnWater && motion > -3.92 && motion < 0.41) {
                    this.setPredict(motion);
                }

                ExtendedPotionEffect slowFalling;
                double pr;
                if (MultiVersion.isOrGreater(MCVersion.V1_9)) {
                    slowFalling = this.protocol().spartan.getPotionEffect(PotionEffectType.LEVITATION, 0L);
                    if (slowFalling != null || this.levitation > 0) {
                        if (slowFalling != null) {
                            this.levitation = 2;
                            pr = 0.05 * (double)slowFalling.bukkitEffect.getAmplifier() + 1.0;
                            this.levitationA = slowFalling.bukkitEffect.getAmplifier() + 1;
                            if (motion < pr && motion >= 0.0) {
                                this.setPredict(motion);
                                this.fallDistance = 0.0;
                            }
                        } else {
                            pr = 0.05 * (double)this.levitationA;
                            if (motion < pr && motion >= 0.0) {
                                this.setPredict(motion);
                                this.fallDistance = 0.0;
                            }
                        }

                        if (this.levitation > 0) {
                            --this.levitationA;
                        }
                    }
                }

                if (MultiVersion.isOrGreater(MCVersion.V1_13)) {
                    slowFalling = this.protocol().spartan.getPotionEffect(PotionEffectType.SLOW_FALLING, 0L);
                    if (slowFalling != null || this.slowFalling > 0) {
                        if (slowFalling != null) {
                            this.slowFalling = 2;
                            pr = 0.02 * (double)slowFalling.bukkitEffect.getAmplifier() + 0.5;
                            this.slowFallingA = slowFalling.bukkitEffect.getAmplifier() + 1;
                            if (motion < pr && motion < 0.0) {
                                this.setPredict(motion);
                            }
                        } else {
                            pr = 0.02 * (double)this.slowFallingA + 0.5;
                            if (motion < pr && motion < 0.0) {
                                this.setPredict(motion);
                            }
                        }

                        if (this.slowFalling > 0) {
                            --this.slowFallingA;
                        }
                    }
                }

                if (this.protocol().bukkit.isFlying()) {
                    this.setPredict(motion);
                }

                this.levitation = 2;
                this.levitationA = 1;
                if (!isOnGround) {
                    if (this.air == 1) {
                        if (motion < 0.0) {
                            this.fallDistance = motion;
                        }
                    } else if (motion < 0.0) {
                        this.fallDistance += motion;
                    } else {
                        this.fallDistance = 0.0;
                    }
                }

                double devTotal = this.abs(this.abs(motion) - this.abs(this.predictY()));
                if (devTotal > 0.009) {
                    this.vl += devTotal;
                    if (this.vl > 0.09) {
                        StringBuilder alertBuilder = new StringBuilder(String.valueOf(RayUtils.scaleVal(devTotal, 6.0)));
                        if (devTotal < 1.0) {
                            alertBuilder.deleteCharAt(0);
                        }

                        this.cancel("simulation[y], " + alertBuilder);
                        this.protocol().bukkit.sendMessage("hacks: " + devTotal);
                    }

                    this.setPredict(motion);
                    this.vl /= 1.3;
                } else {
                    ++this.topReset;
                    this.vl /= 1.01;
                    if (this.topReset == 48) {
                        this.topReset = 0;
                    }
                }

            }
        }
    }

    public void teleport() {
        this.air = 0;
    }

    private double jumpMotion() {
        double jumpPower = 0.42;
        ExtendedPotionEffect jumpEffect = null;
        if (PotionEffectType.getByName("JUMP_BOOST") != null) {
            jumpEffect = this.protocol().spartan.getPotionEffect(PotionEffectType.getByName("JUMP_BOOST"), 1L);
        } else {
            jumpEffect = this.protocol().spartan.getPotionEffect(PotionEffectType.getByName("JUMP"), 1L);
        }

        if (jumpEffect != null) {
            jumpPower += 0.1 * (double)(jumpEffect.bukkitEffect.getAmplifier() + 1);
        }

        return jumpPower;
    }

    private double r(double v) {
        return RayUtils.scaleVal(v, 1.0);
    }

    private double abs(double v) {
        return Math.abs(v);
    }

    private double predictY() {
        return this.protocol().spartan.movement.motionY;
    }

    private void setPredict(double v) {
        this.protocol().spartan.movement.motionY = v;
    }

    private boolean isOnWater(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for(int dx = -1; dx <= 1; ++dx) {
            for(int dy = -1; dy <= 1; ++dy) {
                for(int dz = -1; dz <= 1; ++dz) {
                    Block block = this.getBlockAsync(new Location(this.protocol().spartan.getWorld(), x + (double)dx * 0.3, y + (double)dy * 0.5, z + (double)dz * 0.3));
                    Material material = block != null ? block.getType() : null;
                    if (material != null && BlockUtils.isLiquidOrWaterLogged(block, true)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isOnSemi(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for(int dx = -1; dx <= 1; ++dx) {
            for(int dy = -1; dy <= 1; ++dy) {
                for(int dz = -1; dz <= 1; ++dz) {
                    Block block = this.getBlockAsync(new Location(this.protocol().spartan.getWorld(), x + (double)dx * 0.3, y + (double)dy * 0.5, z + (double)dz * 0.3));
                    Material material = block != null ? block.getType() : null;
                    if (material != null && (BlockUtils.isSemiSolid(material) || BlockUtils.areSlimeBlocks(material) || BlockUtils.areHoneyBlocks(material))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isOnSlime(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for(int dx = -1; dx <= 1; ++dx) {
            for(int dy = -1; dy <= 1; ++dy) {
                for(int dz = -1; dz <= 1; ++dz) {
                    Block block = this.getBlockAsync(new Location(this.protocol().spartan.getWorld(), x + (double)dx * 0.3, y + (double)dy * 0.5, z + (double)dz * 0.3));
                    Material material = block != null ? block.getType() : null;
                    if (material != null && (BlockUtils.areHoneyBlocks(material) || BlockUtils.areSlimeBlocks(material))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isBlocksOnTop(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for(int dx = -1; dx <= 1; ++dx) {
            for(double dy = 1.0; dy <= 2.0; dy += 0.5) {
                for(int dz = -1; dz <= 1; ++dz) {
                    Block block = this.getBlockAsync(new Location(this.protocol().spartan.getWorld(), x + (double)dx * 0.3, y + dy, z + (double)dz * 0.3));
                    Material material = block != null ? block.getType() : null;
                    if (material != null && (BlockUtils.isSemiSolid(material) || material.isSolid())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private Block getBlockAsync(Location location) {
        return Event_Chunks.isLoaded(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4) ? location.getWorld().getBlockAt(location) : null;
    }
}
