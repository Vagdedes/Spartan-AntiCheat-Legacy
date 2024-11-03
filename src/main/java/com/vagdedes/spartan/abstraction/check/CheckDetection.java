package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import me.vagdedes.spartan.system.Enums;

public abstract class CheckDetection {

    public final CheckExecutor executor;
    public final Enums.HackType hackType;
    public SpartanProtocol protocol;

    // Check
    protected CheckDetection(Enums.HackType hackType, SpartanProtocol protocol) {
        this.executor = (CheckExecutor) this;
        this.hackType = hackType;
        this.protocol = protocol;
    }

    // Detection
    protected CheckDetection(CheckExecutor executor) {
        this.executor = executor;
        this.hackType = executor.hackType;
        this.protocol = executor.protocol;
    }

}
