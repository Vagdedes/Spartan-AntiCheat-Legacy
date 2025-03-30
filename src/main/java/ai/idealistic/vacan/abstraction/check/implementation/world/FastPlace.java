package ai.idealistic.vacan.abstraction.check.implementation.world;

import ai.idealistic.vacan.abstraction.check.CheckDetection;
import ai.idealistic.vacan.abstraction.check.CheckEnums.HackType;
import ai.idealistic.vacan.abstraction.check.CheckRunner;
import ai.idealistic.vacan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.vacan.abstraction.data.Buffer;
import ai.idealistic.vacan.abstraction.event.CBlockPlaceEvent;
import ai.idealistic.vacan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.vacan.compatibility.manual.abilities.ItemsAdder;
import ai.idealistic.vacan.compatibility.manual.building.PrinterMode;
import ai.idealistic.vacan.compatibility.manual.entity.Vehicles;
import ai.idealistic.vacan.functionality.server.MultiVersion;
import ai.idealistic.vacan.utils.minecraft.world.BlockUtils;
import org.bukkit.Material;

public class FastPlace extends CheckRunner {

    private final CheckDetection detection;
    private long time;
    private final Buffer.IndividualBuffer buffer;

    public FastPlace(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.detection = new ImplementedDetection(this, null, null, null, true);
        this.buffer = new Buffer.IndividualBuffer();
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof CBlockPlaceEvent) {
            this.detection.call(() -> {
                CBlockPlaceEvent event = (CBlockPlaceEvent) object;

                if (!ItemsAdder.is(event.placedBlock)
                        && !BlockUtils.isScaffoldingBlock(event.placedBlock.getType())) {
                    Material material = event.placedBlock.getType();

                    if (material != Material.FIRE
                            && material != Material.AIR

                            && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                            || material != Material.CAVE_AIR
                            && material != Material.VOID_AIR)) {
                        long timePassed = System.currentTimeMillis() - time;
                        time = System.currentTimeMillis();
                        int ticks = 20;
                        long max = 150L;

                        if (timePassed <= max
                                && (timePassed > 50L || !this.protocol.isBedrockPlayer())) {
                            double amplitude = max / (double) timePassed;

                            if (buffer.count(amplitude, ticks) >= 9) {
                                this.detection.cancel(
                                        "remaining-ticks: " + buffer.ticksRemaining(ticks)
                                                + ", place-ms: " + timePassed
                                                + ", block: " + BlockUtils.materialToString(material),
                                        null,
                                        ticks
                                );
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    protected boolean canRun() {
        return !this.protocol.isFlying()
                && !PrinterMode.isUsing(this.protocol)
                && !Vehicles.has(this.protocol, new String[]{Vehicles.DRILL, Vehicles.TRACTOR});
    }

}
