package com.vagdedes.vacan.compatibility.manual.building;

import com.vagdedes.vacan.Register;
import com.vagdedes.vacan.compatibility.Compatibility;
import com.vagdedes.vacan.functionality.server.Config;
import com.vagdedes.vacan.functionality.server.PluginBase;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import wtf.choco.veinminer.api.event.player.PlayerVeinMineEvent;

public class VeinMiner implements Listener {

    public static void reload() {
        Register.enable(new VeinMiner());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(PlayerVeinMineEvent e) {
        if (Compatibility.CompatibilityType.VEIN_MINER.isFunctional()) {
            Config.compatibility.evadeFalsePositives(
                    PluginBase.getProtocol(e.getPlayer()),
                    Compatibility.CompatibilityType.VEIN_MINER,
                    new Enums.HackType[]{
                            Enums.HackType.NoSwing,
                            Enums.HackType.FastBreak,
                            Enums.HackType.GhostHand,
                            Enums.HackType.BlockReach
                    },
                    30
            );
        }
    }
}
