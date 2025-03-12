package com.vagdedes.vacan.abstraction.check.implementation.player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.vacan.abstraction.check.CheckDetection;
import com.vagdedes.vacan.abstraction.check.CheckRunner;
import com.vagdedes.vacan.abstraction.check.definition.ImplementedDetection;
import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.functionality.server.MultiVersion;
import com.vagdedes.vacan.functionality.server.PluginBase;
import com.vagdedes.vacan.functionality.server.TPS;
import com.vagdedes.vacan.listeners.protocol.DeathListener;
import com.vagdedes.vacan.utils.math.AlgebraUtils;
import com.vagdedes.vacan.utils.minecraft.server.PluginUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameRule;
import org.bukkit.event.entity.PlayerDeathEvent;

public class AutoRespawn extends CheckRunner {

    private final CheckDetection detection;
    private long memory;

    public AutoRespawn(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.detection = new ImplementedDetection(this, null, null, null, true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object == null) {
            this.detection.call(() -> {
                long time = System.currentTimeMillis() - this.memory,
                        limit = 750L;

                if (time <= limit && !this.protocol.bukkit().isDead()) {
                    memory = 0L;
                    this.detection.cancel(
                            "default, ms: " + time,
                            this.protocol.getFromLocation(),
                            AlgebraUtils.integerCeil(TPS.maximum)
                    );
                }
            });
        } else if (object instanceof PlayerDeathEvent) {
            this.death();
        } else if (PluginBase.packetsEnabled()
                && object instanceof PacketEvent) {
            PacketType eventType = ((PacketEvent) object).getPacketType();

            for (PacketType type : DeathListener.packetTypes) {
                if (type.equals(eventType)) {
                    this.death();
                    break;
                }
            }
        }
    }

    private void death() {
        this.memory = System.currentTimeMillis();
    }

    @Override
    protected boolean canRun() {
        return !PluginUtils.contains("respawn")
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)
                || !(this.protocol.getWorld().getGameRuleValue(GameRule.DO_IMMEDIATE_RESPAWN) == true));
    }
}
