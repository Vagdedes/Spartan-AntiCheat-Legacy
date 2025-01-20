package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.simulation;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.abstraction.data.EncirclementData;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import org.bukkit.Location;

public class IMSimulationElytra extends ProbabilityDetection {

    private double vl = 0, oldY = 0, oldSpeed, oldAccel;
    private int ground = 0, air = 0, ticks = 0, postSpeed = 0;
    private Location from = null;

    public IMSimulationElytra(CheckRunner executor) {
        super(executor, null, Check.DetectionType.PACKETS, "elytra", true);
    }

    public void run() {
        this.call(() -> {
            SpartanProtocol p = this.protocol;
            if (!p.spartan.movement.isGliding()) {
                this.ticks = 0;
                this.air = 0;
                return;
            }
            this.ticks++;
            boolean isOnGround = p.isOnGround();
            this.ground(isOnGround);
            Location to = p.getLocation();
            if (from == null) from = to.clone();
            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            double speed = Math.sqrt(dx * dx + dz * dz);
            double accel = getDeviantity(speed, (oldSpeed * 0.91));
            double diffAccel = getDeviantity(accel, oldAccel);
            double y = to.getY() - from.getY();
            final EncirclementData ec = protocol.getEncirclementData();
            double pr = Math.abs(Math.abs(y) - Math.abs(oldY));
            //protocol.bukkit().sendMessage("p: " + speed + " " + y);
            if (ticks > 1) {
                if (speed > 0.05 && ec.isAllFalse() && diffAccel < 1e-7) {
                    //this.protocol.bukkit().sendMessage("d: " + postSpeed);
                    this.postSpeed++;
                    if (this.postSpeed > 7) {
                        this.cancel("type: invalid elytra speed " + speed);
                        this.postSpeed = 6;
                    }
                } else if (this.postSpeed > 0) this.postSpeed -= 2;
                if (pr == 0.0 && ec.isAllFalse()) {
                    this.vl += 2.0;
                    if (this.vl >= 5.0) {
                        this.cancel("type: invalid elytra gravity " + y);
                        this.vl /= 1.3;
                    }
                }
                if (speed > 2.4 || y > 1.7) {
                    this.cancel("type: elytra limit");
                }
                this.vl /= 1.15;
            }
            from = to.clone();
            oldY = y;
            oldSpeed = speed;
            oldAccel = accel;
        });
    }

    private static double getDeviantity(double a, double b) {
        return abs(abs(a) - abs(b));
    }

    public void teleport() {
        this.call(() -> {
            this.air = 0;
            this.ticks = 0;
        });
    }

    public void ground(boolean isOnGround) {
        this.call(() -> {
            this.air = isOnGround ? 0 : this.air + 1;
            this.ground = isOnGround ? this.ground + 1 : 0;
        });
    }

    private static double abs(double v) {
        return Math.abs(v);
    }

    private boolean isOnWater() {
        return this.protocol.getEncirclementData().isWater();
    }

    private boolean isOnIce(Location location) {
        return this.protocol.getEncirclementData().isIce();
    }

    private boolean isOnSemi(Location location) {
        return this.protocol.getEncirclementData().isSemi();
    }

    private boolean isOnSlime(Location location) {
        return this.protocol.getEncirclementData().isSlime();
    }

    private boolean isOnSlimeWide(Location location) {
        return this.protocol.getEncirclementData().isSlimeWide();
    }

}