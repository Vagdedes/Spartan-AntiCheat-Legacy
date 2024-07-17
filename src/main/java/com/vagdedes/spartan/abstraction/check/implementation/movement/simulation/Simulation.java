package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.MCClient;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.Join;
import com.vagdedes.spartan.utils.math.RayUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;

public class Simulation extends CheckExecutor {

    public Simulation(HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0, false);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player.uuid);
        MCClient client = Join.getContainer().get(player.uuid);

        if (object instanceof PlayerMoveEvent) {
            PlayerMoveEvent event = (PlayerMoveEvent) object;
            Location to = event.getTo();
            Location from = event.getFrom();

            double dx = RayUtils.scaleVal(to.getX() - from.getX(), 4),
                    dy = RayUtils.scaleVal(to.getY() - from.getY(), 4),
                    dz = RayUtils.scaleVal(to.getZ() - from.getZ(), 4);
            client.movement(dx, dy, dz);
            //player.sendMessage("v: " + dx + " | " + dy + " | " + dz);

        }
    }

    @Override
    public void cannotHandle(boolean cancelled, Object object) {

    }

    @Override
    public void runInternal(boolean cancelled) {

    }

    @Override
    protected void cannotRun(boolean cancelled) {
        this.handleInternal(cancelled, null);
    }

    @Override
    protected boolean canRun() {
        return true;
    }

}
