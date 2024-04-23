package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.potion.PotionEffectType;

public class IrregularMovementsLevitation extends DetectionExecutor {

    private final IrregularMovements irregularMovements;

    IrregularMovementsLevitation(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        irregularMovements = (IrregularMovements) executor;
    }

    private static final double[][]
            locations = new double[][]{
            {BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 2, BlockUtils.hitbox_max}
    };


    void run(Cooldowns cooldowns,
             boolean canGoUp,
             String prevention, String scenario) {
        if (hackType.getCheck().getBooleanOption("check_levitation", true)) {
            Double vertical = player.movement.getNmsVerticalDistance(),
                    box = player.movement.getNmsBox(),
                    oldBox = player.movement.getPreviousNmsBox();

            if (vertical == null
                    || box == null
                    || oldBox == null) {
                return;
            }
            SpartanLocation to = player.movement.getEventToLocation();

            runAsync(() -> {
                if (!player.getTrackers().has(Trackers.TrackerType.ElytraWear)) {
                    double modifiedBox = AlgebraUtils.cut(box, 5),
                            modifiedOldBox = AlgebraUtils.cut(oldBox, 5);
                    boolean modifiedHeightExists = irregularMovements.cacheBoolean(cooldowns,
                            GroundUtils.blockHeightExists(modifiedBox)
                                    || GroundUtils.blockHeightExists(modifiedOldBox)
                                    || modifiedBox == 0.2
                                    || modifiedOldBox == 0.2,
                            "modified-height-exists", 10);

                    if (canGoUp && vertical >= (0.1 * PlayerUtils.getPotionLevel(player, PotionEffectType.LEVITATION))) {
                        irregularMovements.cancel(prevention, "up-" + scenario, 3);
                    } else if (vertical == 0.0
                            && (!modifiedHeightExists
                            || !PatternUtils.isBlockPattern(locations, to, true, BlockUtils.solid, BlockUtils.semi_solid))) {
                        irregularMovements.cancel(prevention, "horizontal-" + scenario, 3);
                    }  // Do not create a down detection as it is covered by the illegal-down algorithm
                }
            });
        }
    }
}
