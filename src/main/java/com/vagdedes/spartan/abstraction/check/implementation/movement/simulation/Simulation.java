package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion.MotionVector;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.MCClient;
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
        MCClient client = player.protocol.mcClient;

        if (object instanceof PlayerMoveEvent) {
            PlayerMoveEvent event = (PlayerMoveEvent) object;
            Location to = event.getTo();
            Location from = event.getFrom();

            double
            dx = RayUtils.scaleVal(to.getX() - from.getX(), 6),
            dy = RayUtils.scaleVal(to.getY() - from.getY(), 6),
            dz = RayUtils.scaleVal(to.getZ() - from.getZ(), 6);
            double
            xDf = 999,
            yDf = 999,
            zDf = 999;

            double
            xMin = 999,
            yMin = 999,
            zMin = 999,
            xMax = -999,
            yMax = -999,
            zMax = -999;

            MotionVector mV = new MotionVector(dx, dy, dz);
            client.movement(dx, dy, dz);
            double horizontal = RayUtils.scaleVal(Math.sqrt(dx * dx + dz * dz), 6);
            List<MotionVector> predicted = client.getPredicted();
           // player.sendMessage("v: " + horizontal + " | " + client.getRelevantMoveFactor());

            for (MotionVector pV : predicted) {
                double
                xD = s(dx) - s(pV.getX()),
                yD = s(dy) - s(pV.getY()),
                zD = s(dz) - s(pV.getZ());
                if (xD > xMax) xMax = xD + 0.03;
                if (yD > yMax) yMax = yD + 0.03;
                if (zD > zMax) zMax = zD + 0.03;
                if (xD < xMin) xMin = xD - 0.03;
                if (yD < yMin) yMin = yD - 0.03;
                if (zD < zMin) zMin = zD - 0.03;

                xDf = Math.min(Math.abs(xD), xDf);
                yDf = Math.min(Math.abs(yD), yDf);
                zDf = Math.min(Math.abs(zD), zDf);
            }

            // 0.003 / 0.03 packets is a real problem
            // not best solve idea, but it works
            if (yDf == 0.003 || yDf == 0.0031) yDf = 0;
            if (xDf == 0.003 || xDf == 0.0031) xDf = 0;
            if (zDf == 0.003 || zDf == 0.0031) zDf = 0;

            boolean isXValid = (dx >= xMin && dx <= xMax) || Math.abs(xDf) < 0.01;
            boolean isYValid = (dy >= yMin && dy <= yMax) || Math.abs(yDf) < 0.01;
            boolean isZValid = (dz >= zMin && dz <= zMax) || Math.abs(zDf) < 0.01;


            if (isXValid && isYValid && isZValid) {
                client.verifiedMotion = mV.cloneVector();
            } else {
                client.verifiedMotion = predicted.get(predicted.size() - 1);
            }

            /*
            player.getInstance().sendMessage("dx: " + dx + ", dy: " + dy + ", dz: " + dz + ", xMin: " + xMin + " / xMax: " + xMax + ", yMin: " + yMin + " / yMax: " + yMax + ", zMin: " + zMin + " / zMax: " + zMax);
            player.getInstance().sendMessage(isXValid + " " + isYValid + " "  + isZValid);

             */

        }
        if (object instanceof PlayerVelocityEvent) {
            PlayerVelocityEvent event = (PlayerVelocityEvent) object;
            client.claimVelocity(event.getVelocity());
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
