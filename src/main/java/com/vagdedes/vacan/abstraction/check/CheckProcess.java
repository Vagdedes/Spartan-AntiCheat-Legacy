package com.vagdedes.vacan.abstraction.check;

import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import me.vagdedes.spartan.system.Enums;

public abstract class CheckProcess {

    public final Enums.HackType hackType;
    public final PlayerProtocol protocol;

    protected CheckProcess(Enums.HackType hackType, PlayerProtocol protocol) {
        this.hackType = hackType;
        this.protocol = protocol;
    }

}
