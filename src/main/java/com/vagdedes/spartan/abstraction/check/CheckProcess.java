package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import me.vagdedes.spartan.system.Enums;

public abstract class CheckProcess {

    public final Enums.HackType hackType;
    private SpartanProtocol protocol;
    private String lastName;
    private final Object synchronizer;

    protected CheckProcess(Enums.HackType hackType, SpartanProtocol protocol) {
        this.hackType = hackType;
        this.protocol = protocol;
        this.synchronizer = new Object();
    }

    public final SpartanProtocol protocol() {
        synchronized (this.synchronizer) {
            return this.protocol;
        }
    }

    public final String lastName() {
        synchronized (this.synchronizer) {
            return this.lastName;
        }
    }

    public final PlayerProfile profile() {
        synchronized (this.synchronizer) {
            if (this.protocol != null) {
                return this.protocol.getProfile();
            } else {
                return ResearchEngine.getPlayerProfile(this.lastName, true);
            }
        }
    }

    public final void setProtocol(SpartanProtocol protocol) {
        synchronized (this.synchronizer) {
            this.protocol = protocol;
            this.lastName = protocol.bukkit.getName();
        }
    }

}
