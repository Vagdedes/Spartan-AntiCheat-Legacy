package com.vagdedes.spartan.abstraction.check.implementation.combat.fastclicks;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

public class FastClicks extends CheckRunner {

    private final FastClicksBukkit bukkit;
    private final FastClicksPackets packets;

    public FastClicks(Enums.HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.bukkit = new FastClicksBukkit(this);
        this.packets = new FastClicksPackets(this);
    }

    @Override
    protected void runInternal(boolean cancelled) {
        bukkit.run();
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object == null) {
            this.addDisableCause(
                    null,
                    null,
                    AlgebraUtils.integerCeil(TPS.maximum)
            );
        }
    }

}
