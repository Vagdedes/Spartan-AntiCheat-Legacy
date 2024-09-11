package com.vagdedes.spartan.abstraction.event;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import org.bukkit.entity.Player;

public class PlayerTickEvent {

    private final SpartanProtocol protocol;
    private final long time;
    private long delay;

    public PlayerTickEvent(SpartanProtocol protocol, long time) {
        this.protocol = protocol;
        this.time = time;
        this.delay = -1;
    }
    public PlayerTickEvent(SpartanProtocol protocol) {
        this.protocol = protocol;
        this.time = System.currentTimeMillis();
        this.delay = -1;
    }

    public void build() {
        this.delay = this.time - this.protocol.tickTime;
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
