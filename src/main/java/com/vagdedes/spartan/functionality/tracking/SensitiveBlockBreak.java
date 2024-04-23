package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;

public class SensitiveBlockBreak {

    public static void run(SpartanPlayer p, SpartanBlock b) {
        if (BlockUtils.isSensitive(p, b.material)) {
            p.getTrackers().add(Trackers.TrackerType.SensitiveBlockBreak, (int) TPS.maximum);
        }
    }

}
