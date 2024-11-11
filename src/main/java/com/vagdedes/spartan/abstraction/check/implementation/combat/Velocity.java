package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.listeners.bukkit.standalone.chunks.Event_Chunks;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

public class Velocity extends CheckExecutor {

    private final DetectionExecutor vertical, horizontal;

    public Velocity(Enums.HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.vertical = new ImplementedDetectionExecutor(this, "vertical", true);
        this.horizontal = new ImplementedDetectionExecutor(this, "horizontal", true);
    }

    private float vl = 0;
    private long oldTime = System.currentTimeMillis();
    private int timing = 0;
    private Vector velocity = null;

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerVelocityEvent) {
            PlayerVelocityEvent event = (PlayerVelocityEvent) object;
            Location[] locationsToCheck = {
                            this.protocol().getLocation().clone().add(event.getVelocity()),
                            this.protocol().getLocation().clone().add(event.getVelocity()).add(0, 1, 0)
            };
            boolean allClear = true;
            for (Location loc : locationsToCheck) {
                if (isPointWall(loc)) {
                    allClear = false;
                    break;
                }
            }
            if (allClear) {
                this.velocity = event.getVelocity();
                this.timing = 0;
            }

            if (vl > 0) vl -= 5;
        }
        if (object instanceof PlayerMoveEvent) {
            long delay = System.currentTimeMillis() - oldTime;
            PlayerMoveEvent event = (PlayerMoveEvent) object;
            Location from = event.getFrom();
            Location to = event.getTo();

            double x = to.getX() - from.getX();
            double y = to.getY() - from.getY();
            double z = to.getZ() - from.getZ();

            //this.protocol().bukkit.sendMessage("m: " + y);
            if (velocity != null) {
                // time for vertical
                if (abs(abs(y) - abs(velocity.getY())) < 0.0001) {
                    { // time for horizontal
                        double xDiff = x - velocity.getX();
                        double zDiff = z - velocity.getZ();
                        double total = abs(xDiff) + abs(zDiff);
                        double multi = 1.0;
                        if ((!this.protocol().isOnGround() && !this.protocol().isOnGroundFrom())) {
                            // time for horizontal flag 1
                            if (total > 0.05 * multi) {
                                this.flag("velocity manipulation [xz] (air)", 30, horizontal);
                            }
                        } else if (total > 0.2 * multi) {
                            this.flag("velocity manipulation [xz] (ground)", 30, horizontal);
                        }
                    }
                    //this.protocol().bukkit.sendMessage(x + " " + velocity.getX() + " | " + z + " " + velocity.getZ());
                    this.velocity = null;
                    this.timing = 0;
                } else if (delay > 25) {
                    if (timing < 1) {
                        timing++;
                    } else {
                        // time for vertical flag
                        this.flag("velocity manipulation [y]", 25, vertical);
                        this.velocity = null;
                    }
                }
            }
            this.oldTime = System.currentTimeMillis();
        }
    }

    private void flag(String reason, double vl, DetectionExecutor e) {
        this.vl += (float) vl;
        //this.protocol().bukkit.sendMessage("flag: " + reason);
        if (this.vl > 60) {
            e.cancel(reason);
            this.vl = 50;
        }
    }

    private static double abs(double v) {
        return Math.abs(v);
    }

    private static double r(double v) {
        return RayUtils.scaleVal(v, 6);
    }

    private boolean isPointWall(Location location) {
        double x = location.getX();
        double y = location.getY() + 0.1;
        double z = location.getZ();
        for(int dx = -1; dx <= 1; ++dx) {
            for(int dy = -1; dy <= 1; ++dy) {
                for(int dz = -1; dz <= 1; ++dz) {
                    Block block = this.getBlockAsync(new Location(this.protocol().spartan.getWorld(), x + (double)dx * 0.3, y + (double)dy * 0.3, z + (double)dz * 0.3));
                    Material material = block != null ? block.getType() : null;
                    if (material != null &&
                                    (BlockUtils.isSemiSolid(material)
                                                    || BlockUtils.isSolid(material)
                                                    || material.toString().contains("GRASS"))
                                    || BlockUtils.isLiquid(material)) {
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

    @Override
    protected boolean canRun() {
        return this.protocol().packetsEnabled()
                && !this.protocol().spartan.trackers.has(PlayerTrackers.TrackerType.BOUNCING_BLOCKS)
                && CombatUtils.canCheck(this.protocol())
                && !this.protocol().spartan.isOutsideOfTheBorder(-1.0);
    }

}
