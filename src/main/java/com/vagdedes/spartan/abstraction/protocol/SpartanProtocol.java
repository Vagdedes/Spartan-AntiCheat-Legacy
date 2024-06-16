package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SpartanProtocol {

    public final Player player;
    public final SpartanPlayer spartanPlayer;
    private boolean isOnGround, isSprinting, isSneaking, onLoadStatus;
    private Location location, from;
    private Vector velocity;
    private long lastTransaction;
    private int ping;
    private short transactionID;

    public SpartanProtocol(Player player) {
        this.player = player;
        this.isOnGround = false;
        this.location = new Location(player.getWorld(), 0, 0, 0);
        this.from = this.location;
        this.isSprinting = false;
        this.isSneaking = false;
        this.onLoadStatus = true;
        this.velocity = new Vector(0, 0, 0);
        this.spartanPlayer = new SpartanPlayer(this, player);
        this.lastTransaction = Long.MIN_VALUE;
    }

    public boolean isOnGround() {
        return this.isOnGround;
    }

    public boolean isSprinting() {
        return this.isSprinting;
    }

    public boolean isSneaking() {
        return this.isSneaking;
    }

    public boolean isOnLoadStatus() {
        return this.onLoadStatus;
    }

    public Location getLocation() {
        return this.location;
    }

    public Location getFrom() {
        return this.from;
    }

    public Vector getVelocity() {
        return this.velocity;
    }

    public int getPing() {
        return this.ping;
    }

    public short getTransactionID() {
        return this.transactionID;
    }

    // Separator

    public void setOnGround(boolean isOnGround) {
        this.isOnGround = isOnGround;

        if (this.isOnGround) {
            this.spartanPlayer.movement.resetAirTicks();
        }
    }

    public void setSprinting(boolean isSprinting) {
        this.isSprinting = isSprinting;
    }

    public void setSneaking(boolean isSneaking) {
        this.isSneaking = isSneaking;
    }

    public void setOnLoadStatus(boolean onLoadStatus) {
        this.onLoadStatus = onLoadStatus;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setFrom(Location from) {
        this.from = from;
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }

    public void setLastTransaction() {
        if (this.lastTransaction != Long.MIN_VALUE) {
            this.ping = (int) (System.currentTimeMillis() - this.lastTransaction);
        }
        this.lastTransaction = System.currentTimeMillis();
    }

    public short increaseTransactionID() {
        this.transactionID++;

        if (this.transactionID > 1500) {
            this.transactionID = 1488;
        }
        return this.transactionID;
    }

}
