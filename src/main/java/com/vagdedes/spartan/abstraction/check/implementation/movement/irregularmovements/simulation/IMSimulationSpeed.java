package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.simulation;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.HardcodedDetection;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.utils.math.MathHelper;
import com.vagdedes.spartan.utils.minecraft.vector.SpartanVector2F;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.PI;

public class IMSimulationSpeed extends HardcodedDetection {

    public int ground = 0, air = 0;

    private static final double FRICTION = 0.91;
    private static final double ICE_FRICTION = 0.98;
    private static final double ACCELERATION = 0.02;
    private double motionX = 0, motionZ = 0;
    private double lastDx = 0, lastDz = 0;

    public IMSimulationSpeed(CheckRunner executor) {
        super(executor, "speed", true);
    }

    public final void run() {
        if (true) {
            //return;
        }
        if (this.protocol().spartan != null) {
            SpartanProtocol p = this.protocol();
            boolean isOnGround = p.isOnGround();
            Location to = p.getLocation();
            Location from = this.protocol().getFromLocation();
            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            double speed = Math.sqrt(dx * dx + dz * dz);
            if (to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ()) {
                {

                }
                this.lastDx = dx;
                this.lastDz = dz;
            }
        }
    }

    private void applyAccel(double yawDelta, double accelMulti) {
        this.motionX += -Math.sin(Math.toRadians(this.protocol().getLocation().getYaw() + yawDelta)) * (ACCELERATION * accelMulti);
        this.motionZ += Math.cos(Math.toRadians(this.protocol().getLocation().getYaw() + yawDelta)) * (ACCELERATION * accelMulti);
    }

    private double getDeviantity(double a, double b) {
        return this.abs(this.abs(a) - this.abs(b));
    }

    private void velocity(double motion) {
        PlayerVelocityEvent claimed = null;
        for (PlayerVelocityEvent velo : this.protocol().claimedVeloGravity) {
            if (this.veloIsValid(motion, velo.getVelocity().getY())) {
                // stub
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

    }

    private double abs(double v) {
        return Math.abs(v);
    }

    private void setPredict(double x, double z) {
        this.motionX = x;
        this.motionZ = z;
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
        entities.removeIf(entity -> SpartanLocation.distanceSquared(entity.getLocation(), location) > radius * radius);
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

    public static SpartanVector2F calculateVec2Vec(final Vector from, final Vector to) {
        final Vector diff = to.subtract(from);
        final double distance = Math.hypot(diff.getX(), diff.getZ());
        final float yaw = (float) (Math.atan2(diff.getZ(), diff.getX()) * 180.0F / PI) - 90.0F;
        final float pitch = (float) (-(Math.atan2(diff.getY(), distance) * 180.0F / PI));
        return new SpartanVector2F(yaw, pitch);
    }

}
