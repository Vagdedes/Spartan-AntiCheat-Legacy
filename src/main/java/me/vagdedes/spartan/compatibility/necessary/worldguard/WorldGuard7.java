package me.vagdedes.spartan.compatibility.necessary.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;

public class WorldGuard7 {

    static Object getLocationBasedRegions(Location loc) {
        //RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        //RegionContainer container = WorldGuardPlugin.inst().getRegionContainer();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        //ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));
        //ApplicableRegionSet set = query.getApplicableRegions(loc);
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));
        return set.getRegions().toArray(new ProtectedRegion[0]);
    }
}
