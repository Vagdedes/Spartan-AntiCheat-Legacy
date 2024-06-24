package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.listeners.protocol.Shared;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class Event_Movement implements Listener {

    public static final Enums.HackType[] handledChecks = new Enums.HackType[]{
            Enums.HackType.NoFall,
            Enums.HackType.IrregularMovements,
            Enums.HackType.Speed,
            Enums.HackType.MorePackets,
            Enums.HackType.ImpossibleInventory,
            Enums.HackType.Exploits
    };

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Teleport(PlayerTeleportEvent e) {
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

        // Object
        p.movement.judgeGround();

        // Detections
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

}
