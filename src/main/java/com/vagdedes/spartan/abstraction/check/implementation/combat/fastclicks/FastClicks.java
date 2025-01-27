package com.vagdedes.spartan.abstraction.check.implementation.combat.fastclicks;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class FastClicks extends CheckRunner {

    private final FastClicksDetections packets;

    public FastClicks(Enums.HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.packets = new FastClicksDetections(this);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof BlockBreakEvent
                || object instanceof PlayerDropItemEvent) {
            this.addDisableCause(
                    null,
                    null,
                    AlgebraUtils.integerCeil(TPS.maximum)
            );
        } else if (object instanceof Long) {
            packets.run((long) object);
        } else if (object instanceof PlayerTickEvent) {
            packets.tick((PlayerTickEvent) object);
        } else if (object instanceof PlayerInteractEvent) {
            if (this.prevent()) {
                ((PlayerInteractEvent) object).setCancelled(true);
            }
        }
    }

}
