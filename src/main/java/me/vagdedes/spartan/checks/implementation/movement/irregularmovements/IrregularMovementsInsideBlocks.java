package me.vagdedes.spartan.checks.implementation.movement.irregularmovements;

import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.GroundUtils;
import org.bukkit.Material;

public class IrregularMovementsInsideBlocks {

    static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                    boolean knownBoxes,
                    String prevention) {
        if (IrregularMovements.check.getCheck().getBooleanOption("check_inside_blocks", true)) {
            AsyncDetection.run(p, IrregularMovements.check, () -> {
                if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) { // Make sure player changed blocks
                    Material toMaterial = to.getBlock().getType(),
                            fromMaterial = from.getBlock().getType(),
                            to1 = to.clone().add(0, 1, 0).getBlock().getType(),
                            from1 = from.clone().add(0, 1, 0).getBlock().getType();

                    if ((isValidBlock(toMaterial) || isValidBlock(to1)) // Make sure new blocks are solid
                            && (isValidBlock(fromMaterial) || isValidBlock(from1))) {  // Make sure old blocks were solid
                        String extra = "to: [" + BlockUtils.materialToString(toMaterial) + ", " + BlockUtils.materialToString(to1) + "]"
                                + ", from: [" + BlockUtils.materialToString(fromMaterial) + ", " + BlockUtils.materialToString(from1) + "]";
                        IrregularMovements.cancel(p, from, prevention, "inside-blocks", extra, knownBoxes ? 1 : 2, -IrregularMovements.defaultBufferTime);
                    }
                }
            });
        }
    }

    private static boolean isValidBlock(Material type) {
        return BlockUtils.isFullSolid(type)
                && !BlockUtils.areHoneyBlocks(type)
                && !GroundUtils.getHeights(type).contains(GroundUtils.maxPossibleStep); // Surface blocks
    }
}
