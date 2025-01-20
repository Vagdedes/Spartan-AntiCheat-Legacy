package com.vagdedes.spartan.abstraction.check.implementation.combat.fastclicks;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

public class FastClicks extends CheckRunner {

    private final FastClicksDetections packets;

    public FastClicks(Enums.HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.packets = new FastClicksDetections(this);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object == null) {
            this.addDisableCause(
                    null,
                    null,
                    AlgebraUtils.integerCeil(TPS.maximum)
            );
        } else if (object instanceof Long) {
            packets.run((long) object);
        } else if (object instanceof PlayerTickEvent) {
            packets.tick((PlayerTickEvent) object);
        }
    }

}
