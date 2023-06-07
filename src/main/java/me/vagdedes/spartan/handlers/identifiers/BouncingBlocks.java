package me.vagdedes.spartan.handlers.identifiers;

import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;

public class BouncingBlocks {

    private static final int
            slime = 55,
            bed = 25;

    public static void runMove(SpartanPlayer p) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && p.getVehicle() == null) {
            double vertical = p.getNmsVerticalDistance();

            if (vertical != 0.0
                    && vertical >= -MoveUtils.falling[2]

                    && (!p.isJumping(vertical)
                    || p.getBlocksOffGround(3, true, true) <= 2)) {
                SpartanLocation location = p.getLocation();

                if (BlockUtils.isSlime(p, location, slime)) {
                    add(p, "slime");
                }
                if (BlockUtils.isBed(p, location, bed)) {
                    add(p, "bed");
                }
            }
        }
    }

    // Add

    private static void add(SpartanPlayer p, String s) {
        p.getHandlers().add(Handlers.HandlerType.BouncingBlocks, s, 60);
    }

    public static void judge(SpartanPlayer p, SpartanLocation loc) {
        if (BlockUtils.isSlime(p, loc, 3)) {
            add(p, "slime");
        }
        if (BlockUtils.isBed(p, loc, 3)) {
            add(p, "bed");
        }
    }

    // Live

    public static boolean isLiveBelow(SpartanPlayer p, SpartanLocation loc) {
        return BlockUtils.isSlime(p, loc, slime) || BlockUtils.isBed(p, loc, bed);
    }

    // Cached

    public static boolean isSlimeBelow(SpartanPlayer p) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && p.getHandlers().has(Handlers.HandlerType.BouncingBlocks, "slime");
    }

    public static boolean isBedBelow(SpartanPlayer p) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12) && p.getHandlers().has(Handlers.HandlerType.BouncingBlocks, "bed");
    }

    public static boolean isBelow(SpartanPlayer p) {
        return isSlimeBelow(p) || isBedBelow(p);
    }
}
