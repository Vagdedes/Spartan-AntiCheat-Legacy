package com.vagdedes.vacan.compatibility.manual.damage;

import com.evill4mer.RealDualWield.Api.PlayerOffhandAnimationEvent;
import com.vagdedes.vacan.compatibility.Compatibility;
import com.vagdedes.vacan.functionality.server.Config;
import com.vagdedes.vacan.functionality.server.PluginBase;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class RealDualWield implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(PlayerOffhandAnimationEvent e) {
        Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.REAL_DUAL_WIELD;

        if (compatibilityType.isFunctional()) {
            Config.compatibility.evadeFalsePositives(
                    PluginBase.getProtocol(e.getPlayer()),
                    compatibilityType,
                    new Enums.HackType[]{
                            Enums.HackType.KillAura,
                            Enums.HackType.HitReach,
                            Enums.HackType.Criticals,
                    },
                    5
            );
        }
    }
}
