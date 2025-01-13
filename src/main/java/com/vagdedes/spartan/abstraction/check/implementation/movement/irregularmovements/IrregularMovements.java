package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.HardcodedDetection;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedHardcodedDetection;
import com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.simulation.IMSimulationGravity;
import com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.simulation.IMSimulationSpeed;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.event.SuperPositionPacketEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class IrregularMovements extends CheckRunner {

    private long teleport;
    public final HardcodedDetection limitServerFlying;
    private final IMStepBukkit step;
    private final IMSimulationGravity gravityPackets;
    private final IMBukkitSpeed speedBukkit;
    private final IMSimulationSpeed speedPackets;
    private final IrregularMovementsJump jump;
    private final IMHorizontal horizontal;
    private final IMFoodSprint foodSprint;
    private final IMBaritone baritone;

    public IrregularMovements(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        limitServerFlying = new ImplementedHardcodedDetection(this, null, null, "limit_server_flying", false);
        step = new IMStepBukkit(this);
        gravityPackets = new IMSimulationGravity(this);
        jump = new IrregularMovementsJump(this);
        horizontal = new IMHorizontal(this);
        foodSprint = new IMFoodSprint(this);
        baritone = new IMBaritone(this);
        speedBukkit = new IMBukkitSpeed(this);
        speedPackets = new IMSimulationSpeed(this);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof Integer) {
            gravityPackets.trident();
            speedPackets.trident();
            speedBukkit.trident((int) object);
        } else if (object instanceof SuperPositionPacketEvent) {
            SuperPositionPacketEvent e = (SuperPositionPacketEvent) object;
            speedPackets.ground(e.packetEvent.getPacket().getBooleans().read(0));
        } else if (object instanceof PlayerMoveEvent) {
            PlayerMoveEvent event = (PlayerMoveEvent) object;
            speedBukkit.run(event);
            foodSprint.run();
            baritone.run();
            gravityPackets.run();
            speedPackets.run();
            double vertical = this.protocol.getLocation().getY() - this.protocol.getFromLocation().getY(),
                    box = this.protocol.getLocation().getY(),
                    oldBox = this.protocol.getFromLocation().getY();
            boolean knownBox = GroundUtils.blockHeightExists(box),
                    knownOldBox = GroundUtils.blockHeightExists(oldBox),
                    teleported = teleport >= System.currentTimeMillis();

            if (knownBox || knownOldBox) {
                if (vertical == 0.0) {
                    this.horizontal.run(knownBox, knownOldBox, box, oldBox, vertical);
                } else {
                    step.run(knownBox, knownOldBox, box, oldBox, vertical, teleported);
                }
            }
            if (!teleported) {
                jump.run(vertical, box);
            }
        } else if (object instanceof PlayerVelocityEvent) {
            speedBukkit.velocity((PlayerVelocityEvent) object);
        } else if (object instanceof PlayerTickEvent) {
            PlayerTickEvent e = (PlayerTickEvent) object;
            gravityPackets.groundSp(e.onGround);
        } else if (object instanceof VehicleEnterEvent) {
            speedPackets.vhAct();
        } else {
            gravityPackets.teleport();
            speedPackets.teleport();
            baritone.teleport();
            this.teleport = System.currentTimeMillis()
                    + (AlgebraUtils.integerCeil(TPS.maximum / 2.0) * TPS.tickTime);
        }
    }

    @Override
    protected boolean canRun() {
        return MovementProcessing.canCheck(
                this.protocol,
                false,
                false,
                true,
                false
        ) && !this.protocol.spartan.movement.wasGliding();
    }

}