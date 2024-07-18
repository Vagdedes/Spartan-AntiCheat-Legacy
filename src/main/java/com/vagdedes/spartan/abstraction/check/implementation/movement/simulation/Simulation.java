package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.MCClient;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion.MotionVector;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.Join;
import com.vagdedes.spartan.utils.math.RayUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

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

            double
            dx = RayUtils.scaleVal(to.getX() - from.getX(), 4),
            dy = RayUtils.scaleVal(to.getY() - from.getY(), 4),
            dz = RayUtils.scaleVal(to.getZ() - from.getZ(), 4);
            double
            xDf = 999,
            yDf = 999,
            zDf = 999;

            MotionVector mV = new MotionVector(dx, dy, dz);
            client.movement(dx, dy, dz);
            List<MotionVector> predicted = client.getPredicted();
            //player.sendMessage("v: " + dx + " | " + dy + " | " + dz);

            for (MotionVector pV : predicted) {
                double
                xD = Math.abs(mV.x - pV.x),
                yD = Math.abs(mV.y - pV.y),
                zD = Math.abs(mV.z - pV.z);

                xDf = Math.min(xD, xDf);
                yDf = Math.min(yD, yDf);
                zDf = Math.min(zD, zDf);
            }

            //player.sendMessage("p: " + xDf + " | " + yDf + " | " + zDf);
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
