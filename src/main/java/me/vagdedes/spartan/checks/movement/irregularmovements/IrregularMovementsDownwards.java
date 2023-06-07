package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class IrregularMovementsDownwards {

    static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                    double vertical,
                    int airTicks, int blockY,
                    long repeatedMilliseconds,
                    boolean slowFalling, boolean jumpEffect,
                    Timer timer, Buffer buffer,
                    String detection, String prevention) { // Attention: Do not make asynchronous as it already is called this way
        if (!slowFalling && !jumpEffect && vertical >= -2.0 && blockY > BlockUtils.getMinHeight(p.getWorld())) {
            double cutVertical = AlgebraUtils.cut(vertical, IrregularMovements.desiredDecimalAccuracy);
            String timerKey = detection + "=repeated",
                    timerSpecificKey = timerKey + "=" + cutVertical;
            long ms = timer.get(timerSpecificKey);
            timer.set(timerSpecificKey);
            int oldBlockY = buffer.get(timerKey);
            buffer.set(timerKey, blockY);

            if (airTicks >= IrregularMovements.fullOffGroundTicks) {
                boolean semiSolidFree = true;

                for (int i = -1; i <= 1; i++) {
                    if (!BlockUtils.isNonSolidFree(to, BlockUtils.hitbox_max, i, BlockUtils.hitbox_max)) {
                        semiSolidFree = false;
                        break;
                    }
                }

                if (semiSolidFree) {
                    if (ms <= repeatedMilliseconds) { // + detection added recently
                        IrregularMovements.cancel(p, from, prevention, "repeated-" + detection + "-" + cutVertical, "ms: " + ms, 2, IrregularMovements.defaultBufferTime);
                    } else if (airTicks >= (IrregularMovements.fullOffGroundTicks * 3) && vertical >= -1.0 && oldBlockY >= blockY) { // vertical >= -1.0 added recently
                        int difference = Math.abs(blockY - oldBlockY);

                        if (difference <= 1) {
                            IrregularMovements.cancel(p, from, prevention, "position-" + detection, "difference: " + difference, 2, -IrregularMovements.defaultBufferTime);
                        }
                    }
                }
            }
        }
    }
}
