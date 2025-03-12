package com.vagdedes.vacan.compatibility.manual.abilities;

import be.anybody.api.advancedabilities.ability.event.AbilityCallEvent;
import com.vagdedes.vacan.compatibility.Compatibility;
import com.vagdedes.vacan.functionality.server.Config;
import com.vagdedes.vacan.functionality.server.PluginBase;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AdvancedAbilities implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void AbilityEvent(AbilityCallEvent e) {
        if (Compatibility.CompatibilityType.ADVANCED_ABILITIES.isFunctional()) {
            Config.compatibility.evadeFalsePositives(
                    PluginBase.getProtocol(e.getPlayer()),
                    Compatibility.CompatibilityType.ADVANCED_ABILITIES,
                    new Enums.HackCategoryType[]{
                            Enums.HackCategoryType.MOVEMENT,
                            Enums.HackCategoryType.COMBAT
                    },
                    60
            );
        }
    }
}
