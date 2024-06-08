package com.vagdedes.spartan.abstraction.player.packets;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;


public class IPlayer {
    private Player player;
    public float vl;
    private boolean isOnGround, isSprinting, isSneaking, onLoadStatus;
    private Location location, from;
    private Vector velocity;

    public IPlayer(Player player) {
        this.player = player;
        this.isOnGround = false;
        this.location = new Location(player.getWorld(), 0, 0, 0);
        this.from = this.location;
        this.isSprinting = false;
        this.isSneaking = false;
        this.onLoadStatus = true;
        this.velocity = new Vector(0, 0, 0);
        this.vl = 0;
    }

    public IPlayer() {
    }

    public Player getPlayer() {
        return this.player;
    }

    public float getVl() {
        return this.vl;
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

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setVl(float vl) {
        this.vl = vl;
    }

    public void setOnGround(boolean isOnGround) {
        this.isOnGround = isOnGround;
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
}
