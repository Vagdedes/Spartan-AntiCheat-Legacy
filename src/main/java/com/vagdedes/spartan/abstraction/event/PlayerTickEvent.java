package com.vagdedes.spartan.abstraction.event;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import org.bukkit.entity.Player;

public class PlayerTickEvent {

    private final SpartanProtocol protocol;
    private final long time;
    private boolean legacy;
    private long delay;

    public PlayerTickEvent(SpartanProtocol protocol, long time, boolean legacy) {
        this.protocol = protocol;
        this.time = time;
        this.delay = -1;
        this.legacy = legacy;
    }
    public PlayerTickEvent(SpartanProtocol protocol, boolean legacy) {
        this.protocol = protocol;
        this.time = System.currentTimeMillis();
        this.delay = -1;
        this.legacy = legacy;
    }

    public void build() {
        this.delay = this.time - this.protocol.tickTime;
        if (this.legacy) this.delay -= 1000;
        this.protocol.tickTime = this.time;
    }

    public SpartanProtocol getProtocol() {
        return protocol;
    }
    public long getTime() {
        return this.time;
    }
    public long getDelay() {
        return this.delay;
    }
}
