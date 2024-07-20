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
import org.bukkit.event.player.PlayerVelocityEvent;

import java.util.List;

public class Simulation extends CheckExecutor {

    public double diffVl = 0;

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
            dx = to.getX() - from.getX(),
            dy = to.getY() - from.getY(),
            dz = to.getZ() - from.getZ();
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
                xD = s(dx) - s(pV.getX()),
                yD = s(dy) - s(pV.getY()),
                zD = s(dz) - s(pV.getZ());

                xDf = Math.min(xD, xDf);
                yDf = Math.min(yD, yDf);
                zDf = Math.min(zD, zDf);
            }

            diffVl += Math.abs(yDf);
            if (diffVl > 1) {
               // player.sendMessage("ban wave: you are banned! " + diffVl);
            }

            if (yDf < 0.1) {
                client.verifiedMotion = mV.cloneVector();
            }
            diffVl /= 1.5;
        }
        if (object instanceof PlayerVelocityEvent) {
            PlayerVelocityEvent event = (PlayerVelocityEvent) object;
            org.bukkit.util.Vector velo = event.getVelocity();
            client.motion.add(velo.getX(), velo.getY(), velo.getZ());
        }
    }

    public double s(double v) {
        return RayUtils.scaleVal(v, 4);
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
