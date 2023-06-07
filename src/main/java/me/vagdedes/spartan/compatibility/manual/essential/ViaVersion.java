package me.vagdedes.spartan.compatibility.manual.essential;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import org.bukkit.entity.Player;
import us.myles.ViaVersion.api.Via;

public class ViaVersion {

    public static final int[] v1_16 = new int[]{735, 736, 751, 753, 754};
    public static final int[] v1_15 = new int[]{573, 575, 578};
    public static final int[] v1_14 = new int[]{490, 485, 480, 477};
    public static final int[] v1_13 = new int[]{404, 401, 393};
    public static final int[] v1_12 = new int[]{340, 338, 335};
    public static final int[] v1_11 = new int[]{316, 315};
    public static final int[] v1_10 = new int[]{210};
    public static final int[] v1_9 = new int[]{110, 109, 108, 107};
    public static final int[] v1_8 = new int[]{47};
    public static final int[] v1_7 = new int[]{5, 4};

    public static boolean isUsing(SpartanPlayer p, int[] versions) {
        if (Compatibility.CompatibilityType.ViaVersion.isFunctional() && p != null) {
            Player n = p.getPlayer();

            if (n != null) {
                int detected = Via.getAPI().getPlayerVersion(n);

                for (int version : versions) {
                    if (version == detected) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
