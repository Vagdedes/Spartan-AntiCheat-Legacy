package me.vagdedes.spartan.compatibility.manual.building.veinminer;

import me.vagdedes.spartan.configuration.Compatibility;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import wtf.choco.veinminer.api.event.PlayerVeinMineEvent;

public class VeinMinerNew implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(PlayerVeinMineEvent e) {
        if (Compatibility.CompatibilityType.VeinMiner.isFunctional()) {
            VeinMiner.cancel(e.getPlayer());
        }
    }
}
