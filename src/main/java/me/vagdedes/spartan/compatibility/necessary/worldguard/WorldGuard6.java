package me.vagdedes.spartan.compatibility.necessary.worldguard;

import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.RegionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

public class WorldGuard6 {

    static Object getLocationBasedRegions(Location loc) {
        //RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        //RegionContainer container = WorldGuardPlugin.inst().getRegionContainer();
        RegionContainer container = WorldGuardPlugin.inst().getRegionContainer();
        RegionQuery query = container.createQuery();
        //ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));
        //ApplicableRegionSet set = query.getApplicableRegions(loc);
        ApplicableRegionSet set = query.getApplicableRegions(loc);
        return set.getRegions().toArray(new ProtectedRegion[0]);
    }
}
