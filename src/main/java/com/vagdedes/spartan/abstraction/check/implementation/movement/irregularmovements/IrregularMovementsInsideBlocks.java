package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import org.bukkit.Material;

public class IrregularMovementsInsideBlocks extends DetectionExecutor {

    private final IrregularMovements irregularMovements;

    IrregularMovementsInsideBlocks(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        irregularMovements = (IrregularMovements) executor;
    }

    void run(boolean knownBoxes, String prevention) {
        if (hackType.getCheck().getBooleanOption("check_inside_blocks", true)) {
            SpartanLocation to = player.movement.getEventToLocation(),
                    from = player.movement.getEventFromLocation();

            runAsync(() -> {
                if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) { // Make sure player changed blocks
                    Material toMaterial = to.getBlock().material,
                            fromMaterial = from.getBlock().material,
                            to1 = to.clone().add(0, 1, 0).getBlock().material,
                            from1 = from.clone().add(0, 1, 0).getBlock().material;

                    if ((isValidBlock(toMaterial) || isValidBlock(to1)) // Make sure new blocks are solid
                            && (isValidBlock(fromMaterial) || isValidBlock(from1))) {  // Make sure old blocks were solid
                        String extra = "to: [" + BlockUtils.materialToString(toMaterial) + ", " + BlockUtils.materialToString(to1) + "]"
                                + ", from: [" + BlockUtils.materialToString(fromMaterial) + ", " + BlockUtils.materialToString(from1) + "]";
                        irregularMovements.cancel(prevention, "inside-blocks", extra, knownBoxes ? 1 : 2, -IrregularMovements.defaultBufferTime);
                    }
                }
            });
        }
    }

    private boolean isValidBlock(Material type) {
        return BlockUtils.isFullSolid(type)
                && !BlockUtils.areHoneyBlocks(type)
                && !GroundUtils.getHeights(type).contains(GroundUtils.maxPossibleStep); // Surface blocks
    }
}
