package com.vagdedes.vacan.compatibility.manual.building;

import com.bgsoftware.wildtools.api.events.ToolUseEvent;
import com.vagdedes.vacan.compatibility.Compatibility;
import com.vagdedes.vacan.functionality.server.Config;
import com.vagdedes.vacan.functionality.server.PluginBase;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class WildTools implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(ToolUseEvent e) {
        Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.WILD_TOOLS;

        if (compatibilityType.isFunctional()) {
            Config.compatibility.evadeFalsePositives(
                    PluginBase.getProtocol(e.getPlayer()),
                    compatibilityType,
                    new Enums.HackType[]{
                            Enums.HackType.FastBreak,
                            Enums.HackType.BlockReach,
                            Enums.HackType.GhostHand,
                    },
                    40
            );
        }
    }
}
