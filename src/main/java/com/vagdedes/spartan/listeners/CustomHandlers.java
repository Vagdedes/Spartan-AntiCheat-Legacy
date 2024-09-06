package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;

public class CustomHandlers {

    public static void attack(PlayerAttackEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;
        boolean cancelled = e.isCancelled();

        p.getExecutor(Enums.HackType.NoSwing).handle(cancelled, e);
        p.getExecutor(Enums.HackType.HitReach).handle(cancelled, e);
    }

    public static void useEntity(PlayerAttackEvent e) {
        SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer.getExecutor(
                Enums.HackType.KillAura
        ).handle(e.isCancelled(), e);
    }

}
