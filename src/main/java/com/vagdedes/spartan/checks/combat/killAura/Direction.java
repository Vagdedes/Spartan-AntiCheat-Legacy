package com.vagdedes.spartan.checks.combat.killAura;

// Study of the utility

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.abstraction.DetectionExecutor;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import org.bukkit.entity.LivingEntity;

public class Direction extends DetectionExecutor {

    Direction(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    private static final String key = Direction.class.getName() + "=";

    void run(LivingEntity entity, double width, double height, double distance) {
        double[] widthAndHeight = CombatUtils.getWidthAndHeight(entity);
        double entityWidth = widthAndHeight[0], entityHeight = widthAndHeight[1], absoluteWidth = Math.abs(width),
                widthRatio = absoluteWidth / entityWidth;

        if (widthRatio >= 7.0) { // The raytraced width should remain within half of the entity's width in ideal situations
            cancel("type: direction(beyond-max)"
                    + ", entity: " + CombatUtils.entityToString(entity)
                    + ", width: " + width
                    + ", entity-width: " + entityWidth
                    + ", width-ratio: " + widthRatio
                    + ", height: " + height
                    + ", entity-height: " + entityHeight
                    + ", diagonal-distance: " + distance);
        } else if (widthRatio >= 4.0) {
            Buffer buffer = player.getBuffer(hackType);

            if (buffer.start(key, CombatUtils.getFightTicks(player) * 5) == 2) {
                buffer.remove(key);
                cancel("type: direction(beyond-buffered)"
                        + ", entity: " + CombatUtils.entityToString(entity)
                        + ", width: " + width
                        + ", entity-width: " + entityWidth
                        + ", width-ratio: " + widthRatio
                        + ", height: " + height
                        + ", entity-height: " + entityHeight
                        + ", diagonal-distance: " + distance);
            }
        }
    }
}
