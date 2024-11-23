package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.simulation;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.HardcodedDetection;
import com.vagdedes.spartan.abstraction.protocol.ExtendedPotionEffect;
import com.vagdedes.spartan.abstraction.protocol.PlayerMovement;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.MultiVersion.MCVersion;
import com.vagdedes.spartan.utils.math.MathHelper;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimulationGravity extends HardcodedDetection {
    private int air = 0, ground = 0;
    private int top = 0, topReset = 0;
    private int levitation = 0, glideBlocks = 0, climbBlocks = 0,
            slowFalling = 0, levitationA = 0, slowFallingA = 0;
    private boolean slime = false, slime0Tick = false, trident = false, cringeBlockClimb = false;
    private int slimePunch = 0, slimeTicks = 0, pistonTicks = 0, sub = 0;
    private double vl = 0.0;
    private double fallDistance = 0.0;

    public SimulationGravity(CheckRunner executor) {
        super(executor, "gravity_new", true);
    }

    public void run() {
        //if (true) return;
        if (this.protocol().spartan != null) {
            SpartanProtocol p = this.protocol();
            boolean isOnGround = p.isOnGround();
            Location to = p.getLocation();
            Location from = this.protocol().spartan.movement.getEventFromLocation().bukkit();
            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            double speed = Math.sqrt(dx * dx + dz * dz);
            //this.protocol().bukkit.sendMessage(protocol().pistonHandle + " " + protocol().pistonTick + " " + protocol().explosionHandle);
            if (to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ()) {
                double motion = to.getY() - from.getY();
                double gravity = 0.08;
                double jumpStartMotionY =
                                (atJumpModifyBlock(to)
                                && this.jumpMotion() * 1.5 > motion)
                                ? motion : this.jumpMotion();
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
                        this.setPredict(0.0);
                        if (motion > pr2 && motion < 0.0) {
                            this.setPredict(motion);
                        } else if (cringeBlockClimb && motion < 0.5) {
                            // skull
                            this.setPredict(motion);
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
                        this.slimeTicks = 3;
                    }
                    if (this.slimeTicks > 0) {
                        pr2 = this.jumpMotion() + this.abs(this.fallDistance) * 0.09;
                        if (motion == 0.0) {
                            this.setPredict(0.0);
                        } else if (pr2 > motion) {
                            this.setPredict(motion);
                        }
                        this.slimeTicks--;
                    }
                } else {
                    this.cringeBlockClimb = (this.air == 14);
                    playerMovement = this.protocol().spartan.movement;
                    playerMovement.motionY -= gravity;
                    playerMovement = this.protocol().spartan.movement;
                    playerMovement.motionY *= 0.9800000190734863;
                    if (this.protocol().spartan.movement.motionY < -3.92) {
                        this.protocol().spartan.movement.motionY = -3.92;
                    }
                    this.slime = false;
                }
                if (p.getComponentY().pistonTick)
                    this.pistonTicks = 2;
                if (this.getDeviantity(motion) > 0.01
                                && p.getComponentY().pistonTick
                                && motion <= 0.25) {
                    this.setPredict(motion);
                }
                if (pistonTicks > 0) pistonTicks--;
                if (this.getDeviantity(motion) > 0.01
                                && p.getComponentY().pistonTick
                                && motion <= 1.0) {
                    this.setPredict(motion);
                    p.getComponentY().pistonTick = false;
                }
                boolean slimeEvent = (this.getDeviantity(motion) > 0.001)
                                && (p.getComponentY().pistonHandle
                                && this.slime0Tick);
                if (slimeEvent) {
                    p.getComponentY().pistonHandle = false;
                    slimePunch = 4;
                }
                if ((this.isOnSemi(to) || this.isOnSlimeHeight(to)) && motion != 0.0 && motion < 0.51) {
                    this.setPredict(motion);
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
                    this.sub = 5;
                }

                if (this.abs(this.abs(motion)
                        - this.abs(this.predictY())) > 0.01
                        && this.trident && motion < 4.2) {
                    this.setPredict(motion);
                }

                if (this.sub > 0 && this.abs(this.abs(motion) - this.abs(this.predictY())) < 0.14) {
                    this.sub--;
                    this.setPredict(motion);
                }

                ExtendedPotionEffect slowFalling;
                double pr;
                if (MultiVersion.isOrGreater(MCVersion.V1_9)) {
                    slowFalling = this.protocol().spartan.getPotionEffect(PotionEffectType.LEVITATION, 0L);
                    if (slowFalling != null || this.levitation > 0) {
                        if (slowFalling != null) {
                            this.levitation = 2;
                            pr = 0.05 * (double) slowFalling.bukkitEffect.getAmplifier() + 1.0;
                            this.levitationA = slowFalling.bukkitEffect.getAmplifier() + 1;
                        } else {
                            pr = 0.05 * (double) this.levitationA;
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
                            pr = 0.02 * (double) slowFalling.bukkitEffect.getAmplifier() + 0.5;
                            this.slowFallingA = slowFalling.bukkitEffect.getAmplifier() + 1;
                        } else {
                            pr = 0.02 * (double) this.slowFallingA + 0.5;
                        }
                        if (motion < pr && motion < 0.0) {
                            this.setPredict(motion);
                        }

                        if (this.slowFalling > 0) {
                            --this.slowFallingA;
                        }
                    }
                }

                if (this.protocol().spartan.movement.isFlying()
                        || this.protocol().spartan.movement.isGliding()) {
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

                if (this.glideBlocks > 0 && devPreTotal > 0.001 && motion < 0 && motion > -0.16) {
                    this.setPredict(motion);
                    this.glideBlocks--;
                } else if (devPreTotal > 0.001 && motion < 0 && motion > -0.16 && isNearGlideBlocks(to)) {
                    this.setPredict(motion);
                    this.glideBlocks = 2;
                }

                double devPreTotal2 = this.abs(this.abs(motion) - this.abs(this.predictY()));

                if (this.climbBlocks > 0 && devPreTotal2 > 0.001 && motion < 0.118 && motion > -0.16) {
                    this.setPredict(motion);
                    this.climbBlocks--;
                } else if (devPreTotal2 > 0.001 && motion < 0.118 && motion > -0.16 && isNearClimbBlocks(to)) {
                    this.setPredict(motion);
                    this.climbBlocks = 2;
                }

                if (this.getDeviantity(motion) > 0.001
                                && this.slimePunch > 0
                                && motion > 0 && motion < 1.52) {
                    this.setPredict(motion);
                    this.slimePunch--;
                }

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


                /*
                Brainrot 1.18< version cringe fix...
                 */
                if (!this.protocol().isUsingVersionOrGreater(MCVersion.V1_19)
                                && (air > 5 && air < 10)
                                && (motion < this.predictY() + 0.04)
                                && speed < 0.1) {
                    double allowedDeviantity = 0.076;
                    ExtendedPotionEffect jumpEffect;
                    if (PotionEffectType.getByName("JUMP_BOOST") != null) {
                        jumpEffect = this.protocol().spartan.getPotionEffect(PotionEffectType.getByName("JUMP_BOOST"), 1L);
                    } else {
                        jumpEffect = this.protocol().spartan.getPotionEffect(PotionEffectType.getByName("JUMP"), 1L);
                    }

                    if (jumpEffect != null) {
                        allowedDeviantity += 0.04 * (double) (jumpEffect.bukkitEffect.getAmplifier() + 1);
                        if (allowedDeviantity > 0.16) allowedDeviantity = 0.16;
                    }

                    if (this.getDeviantity(motion) < allowedDeviantity)
                        this.setPredict(motion);
                }

                /*
                Sometimes after the explosion comes first
                the recoil from the explosion,
                and then with in the next tick already from the recoil,
                let's take into account this nonsense
                 */
                if (this.protocol().getComponentY().explosionTick && this.getDeviantity(motion) < 1.2) {
                    this.setPredict(motion);
                    this.protocol().getComponentY().explosionTick = false;
                }

                double devTotal = this.abs(this.abs(motion) - this.abs(this.predictY()));
                //this.protocol().bukkit.sendMessage("m: " + motion + " p: " + this.predictY() + " air: " + air);

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
                            /*
                            Bukkit.getScheduler().runTask(Register.plugin, () -> {
                                this.protocol().bukkit.teleport(from);
                            });
                             */
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

    private double getDeviantity(double motion) {
        return this.abs(this.abs(motion) - this.abs(this.predictY()));
    }

    private void velocity(double motion) {
        PlayerVelocityEvent claimed = null;
        for (PlayerVelocityEvent velo : this.protocol().claimedVeloGravity) {
            if (this.veloIsValid(motion, velo.getVelocity().getY())) {
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

    public void ground(boolean isOnGround) {
        this.air = isOnGround ? 0 : this.air + 1;
        this.ground = isOnGround ? this.ground + 1 : 0;
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
            jumpPower += 0.1 * (double) (jumpEffect.bukkitEffect.getAmplifier() + 1);
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

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    SpartanBlock block = new SpartanLocation(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                    ).getBlock();

                    Material m = this.protocol().packetWorld.getBlock(
                                    new Location(this.protocol().spartan.getWorld(),
                                                    x + (double) dx * 0.3,
                                                    y + (double) dy * 0.5,
                                                    z + (double) dz * 0.3)
                    );

                    if (BlockUtils.isLiquidOrWaterLogged(block, true)
                                    || BlockUtils.isLiquid(m)) {
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

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                    );

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

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                    );

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

    private boolean atJumpModifyBlock(Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -2; dy <= 2; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                                    new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                    );

                    if (material != null && material
                                    .toString().equalsIgnoreCase("SWEET_BERRY_BUSH")
                                    || BlockUtils.isPowderSnow(material)) {
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

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                    );

                    if (material != null
                            && BlockUtils.canClimb(material, false)) {
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

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                    );

                    if (material != null
                            && (BlockUtils.areHoneyBlocks(material)
                            || BlockUtils.areBeds(material)
                            || BlockUtils.areSlimeBlocks(material))) {
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

        for (int dx = -2; dx <= 2; ++dx) {
            for (int dy = -2; dy <= 2; ++dy) {
                for (int dz = -2; dz <= 2; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx, y + (double) dy, z + (double) dz)
                    );

                    if (material != null && (BlockUtils.areSlimeBlocks(material))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOnSlimeHeight(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -2; dy <= 2; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                                    new Location(this.protocol().spartan.getWorld(), x + (double) dx, y + (double) dy, z + (double) dz)
                    );

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

        for (int dx = -2; dx <= 2; ++dx) {
            for (double dy = 1.0; dy <= 2.0; dy += 0.5) {
                for (int dz = -2; dz <= 2; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + dy, z + (double) dz * 0.3)
                    );

                    if (material != null && BlockUtils.isSolid(material)) {
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
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(
                                    this.protocol().spartan.getWorld(),
                                    x + dx,
                                    y + dy,
                                    z + dz)
                    );

                    if (material == null) {
                        continue;
                    }
                    if (BlockUtils.isSolid(material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
