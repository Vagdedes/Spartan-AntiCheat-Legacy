package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.comphenix.protocol.PacketType;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.event.CPlayerRiptideEvent;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.event.SuperPositionPacketEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.listeners.protocol.TeleportListener;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class IrregularMovements extends CheckRunner {

    private final IMSimulationGravity gravity;
    private final IMSimulationSpeed speed;
    private final IrregularMovementsJump jump;
    private final IMFoodSprint foodSprint;
    private final IMBaritone baritone;

    public IrregularMovements(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
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
                    box = this.protocol.getLocation().getY();
            jump.run(vertical, box);
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
    }

    @Override
    protected boolean canRun() {
        return MovementProcessing.canCheck(
                this.protocol,
                false,
                false,
                false,
                true,
                false
        );
    }

}