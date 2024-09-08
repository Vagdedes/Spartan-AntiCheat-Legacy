package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.bukkit.replicated.partly.completed.Packet_Teleport;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class SpartanProtocol {

    private final long time;
    public final Player player;
    public final SpartanPlayer spartanPlayer;
    private boolean onGround, onGroundFrom;
    public boolean mutateTeleport, sprinting, sneaking;
    public boolean loaded;
    private Location location;
    private PlayerProfile profile;
    public List<Packet_Teleport.TeleportData> teleportEngine;
    public final MCClient mcClient;

    public SpartanProtocol(Player player) {
        this.time = System.currentTimeMillis();
        this.player = player;

        this.onGround = false;
        this.onGroundFrom = false;
        this.sprinting = false;
        this.sneaking = false;
        this.mutateTeleport = false;
        this.loaded = false;
        this.location = player.getLocation();
        this.teleportEngine = new LinkedList<>();

        this.spartanPlayer = new SpartanPlayer(this);
        this.mcClient = new MCClient(player);

        this.setProfile(ResearchEngine.getPlayerProfile(this, false));
    }

    public long timePassed() {
        return System.currentTimeMillis() - this.time;
    }

    public boolean isOnGround() {
        return packetsEnabled()
                ? this.onGround
                : this.player.isOnGround();
    }

    public boolean isOnGroundFrom() {
        return packetsEnabled()
                ? this.onGroundFrom
                : this.player.isOnGround();
    }

    public boolean isSprinting() {
        return packetsEnabled()
                ? this.sprinting
                : this.player.isSprinting();
    }

    public boolean isSneaking() {
        return packetsEnabled()
                ? this.sneaking
                : this.player.isSneaking();
    }

    public boolean isLoading() {
        return packetsEnabled() && !this.loaded;
    }

    public Location getLocation() {
        return packetsEnabled()
                ? this.location
                : this.player.getLocation();
    }

    public int getPing() {
        return Latency.ping(this.player);
    }
    public UUID getUUID() {
        return this.player.getUniqueId();
    }

    public PlayerProfile getProfile() {
        return this.profile;
    }

    // Separator

    public void setOnGround(boolean isOnGround) {
        this.onGroundFrom = this.onGround;
        this.onGround = isOnGround;

        if (this.onGround) {
            this.spartanPlayer.movement.resetAirTicks();
        }
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setProfile(PlayerProfile profile) {
        this.profile = profile;
    }

    public boolean packetsEnabled() {
        return SpartanBukkit.packetsEnabled() && !this.spartanPlayer.bedrockPlayer;
    }

}