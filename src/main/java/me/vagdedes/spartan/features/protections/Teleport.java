package me.vagdedes.spartan.features.protections;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import me.vagdedes.spartan.compatibility.necessary.worldguard.WorldGuardCompatibility;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;

import java.util.Map;
import java.util.UUID;

public class Teleport {

    public static final int ticks = 3;
    public static final String reason = "Teleport-Protection";

    public static void run(SpartanPlayer p) {
        if (!p.getHandlers().isDisabled(Handlers.HandlerType.Teleport)) {
            UUID uuid = p.getUniqueId();

            if (Settings.getBoolean("Protections.use_teleport_protection")) {
                for (Enums.HackType hackType : Enums.hackTypes()) {
                    hackType.getCheck().addSilentUser(uuid, reason, ticks);
                }
            } else {
                for (Enums.HackType hackType : Enums.hackTypes()) {
                    Check check = hackType.getCheck();

                    if (!check.hasMaximumDefaultCancelViolation()) {
                        hackType.getCheck().addSilentUser(uuid, reason, ticks);
                    }
                }
            }
        }
    }

    private static boolean isInWorldGuardRegion(SpartanLocation location, boolean full) {
        for (SpartanLocation loc : location.getSurroundingLocations( 1, 0, 1, true)) {
            Object regionsObject = WorldGuardCompatibility.getLocationBasedRegions(loc.getBukkitLocation());

            if (regionsObject != null) {
                for (ProtectedRegion worldguardRegion : (ProtectedRegion[]) regionsObject) {
                    if (worldguardRegion.getType() != RegionType.GLOBAL) {
                        for (Map.Entry<Flag<?>, Object> flag : worldguardRegion.getFlags().entrySet()) {
                            String name = flag.getKey().getName().toLowerCase();

                            if ((name.equals("entry") || full && name.equals("exit")) &&
                                    flag.getValue().toString().equalsIgnoreCase("deny")) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
