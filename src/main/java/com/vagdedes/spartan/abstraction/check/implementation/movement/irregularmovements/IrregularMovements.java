package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.comphenix.protocol.PacketType;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.event.CPlayerRiptideEvent;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.event.SuperPositionPacketEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.listeners.protocol.TeleportListener;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class IrregularMovements extends CheckRunner {

    private long teleport;
    private final IMStepBukkit step;
    private final IMSimulationGravity gravity;
    private final IMSimulationSpeed speed;
    private final IrregularMovementsJump jump;
    private final IMFoodSprint foodSprint;
    private final IMBaritone baritone;

    public IrregularMovements(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        step = new IMStepBukkit(this);
        gravity = new IMSimulationGravity(this);
        jump = new IrregularMovementsJump(this);
        foodSprint = new IMFoodSprint(this);
        baritone = new IMBaritone(this);
        speed = new IMSimulationSpeed(this);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerTeleportEvent) {
            this.teleport();
        } else if (object instanceof CPlayerRiptideEvent) {
            gravity.trident();
            speed.trident();
        } else if (object instanceof SuperPositionPacketEvent) {
            SuperPositionPacketEvent e = (SuperPositionPacketEvent) object;
            speed.ground(e.packetEvent.getPacket().getBooleans().read(0));
        } else if (object instanceof PlayerMoveEvent) {
            PlayerMoveEvent event = (PlayerMoveEvent) object;
            foodSprint.run();
            baritone.run();
            gravity.run();
            speed.run();
            double vertical = this.protocol.getLocation().getY() - this.protocol.getFromLocation().getY(),
                    box = this.protocol.getLocation().getY(),
                    oldBox = this.protocol.getFromLocation().getY();
            boolean knownBox = GroundUtils.blockHeightExists(box),
                    knownOldBox = GroundUtils.blockHeightExists(oldBox),
                    teleported = teleport >= System.currentTimeMillis();

            if (knownBox || knownOldBox) {
                if (vertical != 0.0) {
                    step.run(knownBox, knownOldBox, box, oldBox, vertical, teleported);
                }
            }
            if (!teleported) {
                jump.run(vertical, box);
            }
        } else if (object instanceof PlayerTickEvent) {
            PlayerTickEvent e = (PlayerTickEvent) object;
            gravity.groundSp(e.onGround);
        } else if (object instanceof VehicleEnterEvent) {
            speed.vhAct();
        } else if (PluginBase.packetsEnabled()
                && object instanceof PacketType) {
            for (PacketType type : TeleportListener.packetTypes) {
                if (type.equals(object)) {
                    this.teleport();
                    break;
                }
            }
        }
    }

    private void teleport() {
        gravity.teleport();
        speed.teleport();
        baritone.teleport();
        jump.teleport();
        teleport = System.currentTimeMillis()
                + (AlgebraUtils.integerCeil(TPS.maximum / 2.0) * TPS.tickTime);
    }

    @Override
    protected boolean canRun() {
        return MovementProcessing.canCheck(
                this.protocol,
                true,
                false,
                true,
                false
        );
    }

}