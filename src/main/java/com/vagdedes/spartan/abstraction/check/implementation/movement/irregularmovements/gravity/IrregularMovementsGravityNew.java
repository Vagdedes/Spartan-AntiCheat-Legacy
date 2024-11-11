package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.gravity;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.protocol.ExtendedPotionEffect;
import com.vagdedes.spartan.abstraction.protocol.PlayerMovement;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.MultiVersion.MCVersion;
import com.vagdedes.spartan.listeners.bukkit.standalone.chunks.Event_Chunks;
import com.vagdedes.spartan.utils.math.MathHelper;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class IrregularMovementsGravityNew extends DetectionExecutor {
    private int air = 0, ground = 0;
    private int top = 0, topReset = 0;
    private int levitation = 0, glideBlocks = 0, climbBlocks = 0,
                slowFalling = 0, levitationA = 0, slowFallingA = 0;
    private boolean slime = false, slime0Tick = false, trident = false;
    private int slimePunch = 0;
    private double vl = 0.0;
    private double fallDistance = 0.0;

    public IrregularMovementsGravityNew(CheckExecutor executor) {
        super(executor, "gravity_new", true);
    }

    public void run() {
       // if (true) return;
        if (this.protocol().spartan != null) {
            SpartanProtocol p = this.protocol();
            boolean isOnGround = p.isOnGround();
            Location to = p.getLocation();
            Location from = this.protocol().spartan.movement.getEventFromLocation().bukkit();
            boolean slimeEvent = (p.pistonHandle && this.slime0Tick);
            if (slimeEvent) slimePunch = 3;
            p.pistonHandle = false;
            if (to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ()
                            || to.getYaw() != from.getYaw() || to.getPitch() != from.getPitch()) {
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
                        if (this.abs(motion) < 0.08 && this.abs(motion) > 0.0
                                        && (this.isBlocksOnTop(to)
                                        || this.isOnSlime(to)
                                        || isNearGlideBlocks(to))) {
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
                        this.setPredict((float) jumpStartMotionY);
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
                if (!this.protocol().claimedVeloGravity.isEmpty() && pr2 > 0.001) {
                    this.velocity(motion);
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

                if (this.abs(this.abs(motion)
                                - this.abs(this.predictY())) > 0.01
                                && this.trident && motion < 4.2) {
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
                        } else {
                            pr = 0.05 * (double)this.levitationA;
                        }
                        if (motion < pr && motion >= 0.0) {
                            this.setPredict(motion);
                            this.fallDistance = 0.0;
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
                        } else {
                            pr = 0.02 * (double)this.slowFallingA + 0.5;
                        }
                        if (motion < pr && motion < 0.0) {
                            this.setPredict(motion);
                        }

                        if (this.slowFalling > 0) {
                            --this.slowFallingA;
                        }
                    }
                }

                if (this.protocol().bukkit.isFlying()
                    || this.protocol().bukkit.isGliding()) {
                    this.setPredict(motion);
                    this.levitation = 3;
                    this.levitationA = 3;
                }
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
                double devPreTotal = this.abs(this.abs(motion) - this.abs(this.predictY()));

                if (glideBlocks > 0 && devPreTotal > 0.001 && motion < 0 && motion > -0.16) {
                    this.setPredict(motion);
                    glideBlocks--;
                } else if (devPreTotal > 0.001 && motion < 0 && motion > -0.16 && isNearGlideBlocks(to)) {
                    this.setPredict(motion);
                    this.glideBlocks = 2;
                }

                double devPreTotal2 = this.abs(this.abs(motion) - this.abs(this.predictY()));

                if (climbBlocks > 0 && devPreTotal2 > 0.001 && motion < 0.118 && motion > -0.16) {
                    this.setPredict(motion);
                    climbBlocks--;
                } else if (devPreTotal2 > 0.001 && motion < 0.118 && motion > -0.16 && isNearClimbBlocks(to)) {
                    this.setPredict(motion);
                    this.climbBlocks = 2;
                }

                if (this.slimePunch > 0 && motion > 0 && motion < 1.52) {
                    this.setPredict(motion);
                    this.slimePunch--;
                }

                //this.protocol().bukkit.sendMessage("m: " + slimeEvent + " " + motion + " p: " + this.predictY());

                boolean boat = false;
                List<Entity> nearbyEntities = this.getEntitiesAroundPoint(to, 2);
                for (Entity vehicle : nearbyEntities) {
                    if (vehicle.getType().toString().toLowerCase().contains("boat")) {
                        boat = true;
                    }
                }
                if (this.abs(this.abs(motion) - this.abs(this.predictY())) > 0.005
                                && boat && motion < 0.6 && motion > 0)
                    this.setPredict(motion);

                if (this.abs(this.abs(motion) - this.abs(this.predictY())) > 0.005
                                && this.protocol().spartan.getVehicle() != null) {
                    Entity v = this.protocol().spartan.getVehicle();
                    boolean isJumpingEntity = (v.getName().toLowerCase().contains("horse")
                                    || v.getName().toLowerCase().contains("donkey")
                                    || v.getName().toLowerCase().contains("mule")
                                    || v.getName().toLowerCase().contains("llama"));

                    if (this.isOnSolidGround(to) && this.abs(motion) <= ((isJumpingEntity || boat) ? 1.0 : 0.3)) {
                        this.setPredict(motion);
                    } else if (!this.isOnSolidGround(to)
                                    && motion < this.predictY() / 2) {
                        this.setPredict(motion);
                    }
                }

                double devTotal = this.abs(this.abs(motion) - this.abs(this.predictY()));
                if (devTotal > 0.009) {
                    if (this.protocol().spartan.trackers.has(
                                    PlayerTrackers.TrackerType.VEHICLE, "exit_tp")
                                    && devTotal < 1.0) {
                        this.setPredict(0.0);
                    } else {
                        this.vl += devTotal;
                        if (this.vl > 0.09) {
                            StringBuilder alertBuilder = new StringBuilder(String.valueOf(RayUtils.scaleVal(devTotal, 6.0)));
                            if (devTotal < 1.0) {
                                alertBuilder.deleteCharAt(0);
                            }
                            this.cancel("simulation[y], " + alertBuilder);
                            //this.protocol().bukkit.sendMessage("hacks: " + devTotal);
                        }

                        this.setPredict(motion);
                        this.vl /= 1.3;
                    }
                } else {
                    ++this.topReset;
                    this.vl /= 1.01;
                    if (this.topReset == 48) {
                        this.topReset = 0;
                    }
                }
            }
            slime0Tick = (this.isOnSlimeWide(to) || this.isOnSlimeWide(from));
        }
    }

    private void velocity(double motion) {
        PlayerVelocityEvent claimed = null;
        for (PlayerVelocityEvent velo : this.protocol().claimedVeloGravity) {
            if (veloIsValid(motion, velo.getVelocity().getY())) {
                this.setPredict(motion);
                claimed = velo;
                break;
            }
        }
        if (claimed != null) this.protocol().claimedVeloGravity.remove(claimed);
    }

    private boolean veloIsValid(double motion, double velo) {
        return abs(this.abs(motion) - this.abs(velo)) < 0.08;
    }

    public void teleport() {
        this.air = 0;
    }
    public void trident() {
        this.trident = true;
        this.levitation = 13;
        this.levitationA = 4;
    }

    private double jumpMotion() {
        double jumpPower = 0.42;
        ExtendedPotionEffect jumpEffect;
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

    private double abs(double v) {
        return Math.abs(v);
    }

    private double predictY() {
        return this.protocol().spartan.movement.motionY;
    }

    private void setPredict(double v) {
        this.protocol().spartan.movement.motionY = v;
    }

    private List<Entity> getEntitiesAroundPoint(Location location, double radius) {
        List<Entity> entities = new ArrayList<>();
        World world = location.getWorld();
        int smallX = MathHelper.floor_double((location.getX() - radius) / 16.0D);
        int bigX = MathHelper.floor_double((location.getX() + radius) / 16.0D);
        int smallZ = MathHelper.floor_double((location.getZ() - radius) / 16.0D);
        int bigZ = MathHelper.floor_double((location.getZ() + radius) / 16.0D);

        for (int x = smallX; x <= bigX; x++) {
            for (int z = smallZ; z <= bigZ; z++) {
                if (world.isChunkLoaded(x, z)) {
                    entities.addAll(Arrays.asList(world.getChunkAt(x, z).getEntities())); // Add all entities from this chunk to the list
                }
            }
        }
        entities.removeIf(entity -> entity.getLocation().distanceSquared(location) > radius * radius);
        return entities;
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
                    if (material != null &&
                                    (BlockUtils.isSemiSolid(material)
                                    || BlockUtils.areSlimeBlocks(material)
                                    || BlockUtils.areHoneyBlocks(material)
                                    || BlockUtils.areWebs(material))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isNearGlideBlocks(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for(int dx = -1; dx <= 1; ++dx) {
            for(int dy = -1; dy <= 1; ++dy) {
                for(int dz = -1; dz <= 1; ++dz) {
                    Block block = this.getBlockAsync(new Location(this.protocol().spartan.getWorld(), x + (double)dx * 0.3, y + (double)dy * 0.5, z + (double)dz * 0.3));
                    Material material = block != null ? block.getType() : null;
                    if (material != null
                                    && BlockUtils.isPowderSnow(material)
                                    || BlockUtils.areHoneyBlocks(material)
                                    || BlockUtils.areWebs(material)
                                    || BlockUtils.areInteractiveBushes(material)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isNearClimbBlocks(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for(int dx = -1; dx <= 1; ++dx) {
            for(int dy = -1; dy <= 1; ++dy) {
                for(int dz = -1; dz <= 1; ++dz) {
                    Block block = this.getBlockAsync(new Location(this.protocol().spartan.getWorld(), x + (double)dx * 0.3, y + (double)dy * 0.5, z + (double)dz * 0.3));
                    Material material = block != null ? block.getType() : null;
                    if (material != null
                                    && BlockUtils.canClimb(material, false) || BlockUtils.canClimb(material, true)) {
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

    private boolean isOnSlimeWide(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for(int dx = -2; dx <= 2; ++dx) {
            for(int dy = -2; dy <= 2; ++dy) {
                for(int dz = -2; dz <= 2; ++dz) {
                    Block block = this.getBlockAsync(new Location(this.protocol().spartan.getWorld(), x + (double)dx, y + (double)dy, z + (double)dz));
                    Material material = block != null ? block.getType() : null;
                    if (material != null && (BlockUtils.areSlimeBlocks(material))) {
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

    private boolean isOnSolidGround(Location location) {
        double x = location.getX(),
                        y = location.getY() - 0.1,
                        z = location.getZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block block = getBlockAsync(
                                    new Location(
                                                    this.protocol().spartan.getWorld(),
                                                    x + dx,
                                                    y + dy,
                                                    z + dz)
                    );
                    Material material = block != null ? block.getType() : null;

                    if (material == null) {
                        continue;
                    }
                    if (BlockUtils.isSolid(material) || material.name().contains("GRASS")) {
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
