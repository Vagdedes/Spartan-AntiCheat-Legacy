package com.vagdedes.spartan.abstraction.check.implementation.combat.hitreach;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.math.RayUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.entity.LivingEntity;

import java.util.LinkedList;

public class HitReach extends CheckExecutor {

    private final LinkedList<Double> rayTraceAnalysis = new LinkedList<>();

    public HitReach(HackType hackType, SpartanPlayer player) {
        super(hackType, player);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        PlayerAttackEvent event = (PlayerAttackEvent) object;
        LivingEntity target = event.getTarget();

        if (CombatProcessing.canCheck(player, target)) {
            double bruteforceRayTrace = RayUtils.bruteforceRayTrace(player, target),
                    diff = bruteforceRayTrace - 0.4;
            rayTraceAnalysis.addLast(bruteforceRayTrace);

            if (rayTraceAnalysis.size() >= 10) {
                analyze(player.movement.getLocation(), bruteforceRayTrace);
            }
        }
    }

    public void analyze(SpartanLocation to, double bruteForce) {
        String punish = null;
        double globalValue = 0;

        for (Double value : rayTraceAnalysis) {
            globalValue += value;
        }
        double result = globalValue / rayTraceAnalysis.size();

        if (result > 0.44) punish = "simple";
        int percent = (int) ((result / 0.4) * 100);
        if (punish != null) {
            cancel(
                    "type: " + punish + ", hitbox-size-analysis: "
                            + result + ", hitbox-size-last: " + bruteForce + ", percent-of-deviance: " + percent + "%",
                    1.0,
                    to
            );
        }
        rayTraceAnalysis.clear();

    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player);
    }
}
