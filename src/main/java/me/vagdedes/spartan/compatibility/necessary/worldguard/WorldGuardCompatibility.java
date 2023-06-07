package me.vagdedes.spartan.compatibility.necessary.worldguard;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.utils.server.PluginUtils;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class WorldGuardCompatibility {

    public static boolean isEnabled() {
        return PluginUtils.exists(new String[]{"WorldEdit", "WorldGuard"});
    }

    public static Object getLocationBasedRegions(Location loc) {
        if (isEnabled()) {
            Plugin plugin = Register.manager.getPlugin("WorldGuard");

            if (plugin != null) {
                String version = plugin.getDescription().getVersion();

                if (version.contains("7.")) {
                    return WorldGuard7.getLocationBasedRegions(loc);
                } else if (version.contains("6.")) {
                    return WorldGuard6.getLocationBasedRegions(loc);
                }
            }
            return null;
        }
        return null;
    }
}
