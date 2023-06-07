package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;

public class IrregularMovementsInsideBlocks {

    static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                    boolean knownBoxes,
                    String prevention) {
        if (IrregularMovements.check.getCheck().getBooleanOption("check_inside_blocks", true)) {
            AsyncDetection.run(p, IrregularMovements.check, () -> {
                if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) { // Make sure player changed blocks
                    SpartanLocation to1 = to.clone().add(0, 1, 0);
                    SpartanLocation from1 = from.clone().add(0, 1, 0);

                    if ((BlockUtils.isFullSolid(to, true) || BlockUtils.isFullSolid(to1, true)) // Make sure new blocks are solid
                            && (BlockUtils.isFullSolid(from, true) || BlockUtils.isFullSolid(from1, true))) {  // Make sure old blocks were solid
                        String extra = "to: [" + BlockUtils.materialToString(to.getBlock().getType()) + ", " + BlockUtils.materialToString(to1.getBlock().getType()) + "]"
                                + ", from: [" + BlockUtils.materialToString(from.getBlock().getType()) + ", " + BlockUtils.materialToString(from1.getBlock().getType()) + "]";
                        IrregularMovements.cancel(p, from, prevention, "inside-blocks", extra, knownBoxes ? 1 : 2, -IrregularMovements.defaultBufferTime);
                    }
                }
            });
        }
    }
}
