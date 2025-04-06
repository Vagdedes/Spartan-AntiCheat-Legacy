package ai.idealistic.vacan.abstraction.check.implementation.movement.irregularmovements;

import ai.idealistic.vacan.abstraction.check.CheckEnums.HackType;
import ai.idealistic.vacan.abstraction.check.CheckRunner;
import ai.idealistic.vacan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.vacan.functionality.server.PluginBase;
import ai.idealistic.vacan.functionality.tracking.MovementProcessing;
import ai.idealistic.vacan.listeners.protocol.TeleportListener;
import ai.idealistic.vacan.utils.minecraft.entity.PlayerUtils;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class IrregularMovements extends CheckRunner {

    private final IMFoodSprint foodSprint;
    private final IMHeadPosition headPosition;
    private final IMSimulationElytra elytra;
    private final IMSimulationVehicle vehicle;

    public IrregularMovements(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        foodSprint = new IMFoodSprint(this);
        headPosition = new IMHeadPosition(this);
        elytra = new IMSimulationElytra(this);
        vehicle = new IMSimulationVehicle(this);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerTeleportEvent) {
            this.teleport();
        } else if (object instanceof PlayerMoveEvent) {
            foodSprint.run();
            headPosition.run();
            elytra.runSimulation();
            vehicle.run();
        } else if (PlayerUtils.elytra
                && object instanceof EntityToggleGlideEvent) {
            elytra.runExploit();
        } else if (PluginBase.packetsEnabled()
                && object instanceof PacketEvent) {
            PacketType eventType = ((PacketEvent) object).getPacketType();

            for (PacketType type : TeleportListener.packetTypes) {
                if (type.equals(eventType)) {
                    this.teleport();
                    break;
                }
            }
        }
    }

    private void teleport() {
        elytra.teleport();
        vehicle.teleport();
    }

    @Override
    protected boolean canRun() {
        return MovementProcessing.canCheck(
                this.protocol,
                true,
                true,
                false,
                true,
                false
        );
    }

}