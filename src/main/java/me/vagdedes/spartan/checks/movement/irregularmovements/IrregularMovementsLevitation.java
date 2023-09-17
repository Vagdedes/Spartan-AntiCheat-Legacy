package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.abstraction.DetectionExecutor;
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
        SpartanPlayer player = irregularMovements.player;
        SpartanLocation to = player.getEventToLocation();
        double vertical = player.getNmsVerticalDistance(),
                box = player.getNmsBox(),
                oldBox = player.getOld_NmsBox();

        if (hackType.getCheck().getBooleanOption("check_levitation", true)) {
            runAsync(() -> {
                if (!player.getHandlers().has(Handlers.HandlerType.ElytraWear)) {
                    double modifiedBox = AlgebraUtils.cut(box, 5),
                            modifiedOldBox = AlgebraUtils.cut(oldBox, 5);
                    boolean modifiedHeightExists = irregularMovements.cacheBoolean(cooldowns,
                            GroundUtils.heightExists(modifiedBox)
                                    || GroundUtils.heightExists(modifiedOldBox)
                                    || modifiedBox == 0.2
                                    || modifiedOldBox == 0.2,
                            "modified-height-exists", 10);

                    if (canGoUp && vertical >= (0.1 * PlayerData.getPotionLevel(player, PotionEffectType.LEVITATION))) {
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
