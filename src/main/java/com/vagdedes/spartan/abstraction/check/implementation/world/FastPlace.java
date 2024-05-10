package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.event.block.BlockPlaceEvent;

public class FastPlace extends CheckExecutor {

    public FastPlace(SpartanPlayer player) {
        super(HackType.FastPlace, player);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        if (canDo()) {
            SpartanBlock b = new SpartanBlock(player, ((BlockPlaceEvent) object).getBlock());

            if (!BlockUtils.isScaffoldingBlock(b.material)) {
                Material material = b.material;

                if (material != Material.FIRE
                        && material != Material.AIR

                        && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                        || material != Material.CAVE_AIR
                        && material != Material.VOID_AIR)) {
                    Timer timer = this.getTimer();
                    long time = this.getTimer().get("time");

                    if (timer.has(time)) {
                        Buffer buffer = this.getBuffer();
                        int ticks = 20;

                        if (time <= 150L) {
                            if (buffer.count("slow", ticks) >= 8) {
                                cancel("type: slow, ms: " + time + ", block: " + BlockUtils.materialToString(material),
                                        null, ticks);
                            }
                            if (time <= 100L) {
                                if (buffer.count("medium", ticks) >= 7) {
                                    cancel("type: medium, ms: " + time + ", block: " + BlockUtils.materialToString(material),
                                            null, ticks);
                                }
                                if (time <= 50L) {
                                    String detection = "type: fast";

                                    if (!player.bedrockPlayer
                                            && hackType.getCheck().getBooleanOption("check_fast", true)
                                            && !Compatibility.CompatibilityType.VEIN_MINER.isFunctional()
                                            && buffer.count("fast", ticks) >= 4) {
                                        cancel(detection + ", ms: " + time + ", block: " + BlockUtils.materialToString(material),
                                                null, ticks);
                                    }
                                }
                            }
                        }
                    }
                    timer.set("time");
                }
            }
        }
    }

    @Override
    public void runInternal(boolean cancelled) {

    }

    @Override
    public void schedulerInternal() {

    }

    @Override
    protected boolean canDo() {
        return !player.movement.isFlying()
                && !PrinterMode.canCancel(player)
                && !Vehicles.has(player, new String[]{"drill", "tractor"});
    }
}
