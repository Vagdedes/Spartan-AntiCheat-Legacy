package me.vagdedes.spartan.checks.combat.killAura;

// Study of the utility

import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import org.bukkit.entity.LivingEntity;

public class Direction {

    private static final String key = Direction.class.getName() + "=";

    static void run(SpartanPlayer player, LivingEntity entity, double width, double height, double distance) {
        double[] widthAndHeight = CombatUtils.getWidthAndHeight(entity);
        double entityWidth = widthAndHeight[0], entityHeight = widthAndHeight[1], absoluteWidth = Math.abs(width),
                widthRatio = absoluteWidth / entityWidth;

        if (widthRatio >= 7.0) { // The raytraced width should remain within half of the entity's width in ideal situations
            KillAura.violate(player, "type: direction(beyond-max)"
                    + ", entity: " + CombatUtils.entityToString(entity)
                    + ", width: " + width
                    + ", entity-width: " + entityWidth
                    + ", width-ratio: " + widthRatio
                    + ", height: " + height
                    + ", entity-height: " + entityHeight
                    + ", diagonal-distance: " + distance);
        } else if (widthRatio >= 4.0) {
            Buffer buffer = player.getBuffer(KillAura.check);

            if (buffer.start(Buffer.BufferType.Combat, key, CombatUtils.getFightTicks(player) * 5) == 2) {
                buffer.remove(key);
                KillAura.violate(player, "type: direction(beyond-buffered)"
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
