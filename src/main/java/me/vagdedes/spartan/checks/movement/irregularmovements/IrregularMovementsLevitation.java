package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.GroundUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.potion.PotionEffectType;

public class IrregularMovementsLevitation {

    private static final double[][]
            locations = new double[][]{
            {BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 2, BlockUtils.hitbox_max}
    };

    static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from, Cooldowns cooldowns,
                    double vertical,
                    boolean canGoUp,
                    double box, double oldBox,
                    String prevention, String scenario) {
        if (IrregularMovements.check.getCheck().getBooleanOption("check_levitation", true)) {
            AsyncDetection.run(p, IrregularMovements.check, () -> {
                if (!p.getHandlers().has(Handlers.HandlerType.ElytraWear)) {
                    double modifiedBox = AlgebraUtils.cut(box, 5),
                            modifiedOldBox = AlgebraUtils.cut(oldBox, 5);
                    boolean modifiedHeightExists = IrregularMovements.cacheBoolean(cooldowns,
                            GroundUtils.heightExists(modifiedBox)
                                    || GroundUtils.heightExists(modifiedOldBox)
                                    || modifiedBox == 0.2
                                    || modifiedOldBox == 0.2,
                            "modified-height-exists", 10);

                    if (canGoUp && vertical >= (0.1 * PlayerData.getPotionLevel(p, PotionEffectType.LEVITATION))) {
                        IrregularMovements.cancel(p, from, prevention, "up-" + scenario, 3);
                    } else if (vertical == 0.0
                            && (!modifiedHeightExists
                            || !PatternUtils.isBlockPattern(locations, to, true, BlockUtils.solid, BlockUtils.semi_solid))) {
                        IrregularMovements.cancel(p, from, prevention, "horizontal-" + scenario, 3);
                    }  // Do not create a down detection as it is covered by the illegal-down algorithm
                }
            });
        }
    }
}
