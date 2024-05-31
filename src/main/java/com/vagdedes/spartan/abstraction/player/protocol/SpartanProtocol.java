package com.vagdedes.spartan.abstraction.player.protocol;

import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesContainer;
import com.vagdedes.spartan.listeners.protocol.modules.RotationData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpartanProtocol {

    private final Player player;
    public final AbilitiesContainer abilities;
    public final RotationData lastRotation;
    public Boolean
            onGround,
            spawnStatus,
            canCheck;
    public Location
            position,
            verifiedPosition,
            lastTeleport;
    public int ignore;

    public SpartanProtocol(Player player) {
        this.player = player;
        this.abilities = new AbilitiesContainer(false, false, false);
        this.lastRotation = new RotationData();
        this.ignore = 0;
    }

    // Utilities

    public boolean hasDataFor(Object object) {
        return object != null
                && (!(object instanceof SpartanProtocolField)
                || ((SpartanProtocolField) object).hasData());
    }

    public boolean trueOrFalse(Boolean bool) {
        return bool != null && bool;
    }

    // Implementations

    public Location getLocation() {
        if (hasDataFor(position)) {
            if (hasDataFor(lastRotation)) {
                position.setYaw(lastRotation.getYaw());
                position.setPitch(lastRotation.getPitch());
            }
            return position;
        } else {
            return player.getLocation();
        }
    }

}
