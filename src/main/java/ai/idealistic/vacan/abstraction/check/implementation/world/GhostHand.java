package ai.idealistic.vacan.abstraction.check.implementation.world;

import ai.idealistic.vacan.abstraction.check.CheckDetection;
import ai.idealistic.vacan.abstraction.check.CheckEnums.HackType;
import ai.idealistic.vacan.abstraction.check.CheckRunner;
import ai.idealistic.vacan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.vacan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.vacan.abstraction.world.ServerLocation;
import ai.idealistic.vacan.compatibility.manual.abilities.ItemsAdder;
import ai.idealistic.vacan.compatibility.manual.building.PrinterMode;
import ai.idealistic.vacan.compatibility.manual.building.TreeFeller;
import ai.idealistic.vacan.compatibility.manual.entity.Vehicles;
import ai.idealistic.vacan.compatibility.manual.vanilla.Attributes;
import ai.idealistic.vacan.utils.minecraft.entity.PlayerUtils;
import ai.idealistic.vacan.utils.minecraft.inventory.MaterialUtils;
import ai.idealistic.vacan.utils.minecraft.world.BlockUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class GhostHand extends CheckRunner {

    private final CheckDetection breaking, interact;

    public GhostHand(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.breaking = new ImplementedDetection(this, null, null, "breaking", true);
        this.interact = new ImplementedDetection(this, null, null, "interact", true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof BlockBreakEvent) {
            this.breaking.call(() -> {
                BlockBreakEvent event = (BlockBreakEvent) object;
                Block b = event.getBlock();

                if (canDo(b)
                        && !ItemsAdder.is(b)) {
                    long breakTime = MaterialUtils.getBlockBreakTime(this.protocol, this.protocol.getItemInHand(), b.getType());

                    if (breakTime <= 500L) {
                        return;
                    }
                    Location location = this.protocol.getLocationOrVehicle().clone().add(
                            0,
                            this.protocol.getEyeHeight(),
                            0
                    );
                    double bDistance = ServerLocation.distance(location, b.getLocation());
                    ServerLocation t = this.protocol.getTargetBlock(bDistance, PlayerUtils.chunk);

                    if (t != null) {
                        double tDistance = t.distance(location);

                        if (tDistance < bDistance) {
                            this.breaking.cancel(
                                    "breaking"
                                            + ", interacted-block: " + BlockUtils.blockToString(b)
                                            + ", blocking-block: " + BlockUtils.blockToString(t.getBlock())
                                            + ", interacted-distance: " + bDistance
                                            + ", blocking-distance: " + tDistance,
                                    null,
                                    1
                            );

                            if (this.breaking.prevent()) {
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            });
        } else if (object instanceof PlayerInteractEvent) {
            this.interact.call(() -> {
                PlayerInteractEvent event = (PlayerInteractEvent) object;

                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Block b = event.getClickedBlock();

                    if (b != null
                            && canDo(b)
                            && BlockUtils.isChangeable(b.getType())) {
                        Location location = this.protocol.getLocationOrVehicle().clone().add(
                                0,
                                this.protocol.getEyeHeight(),
                                0
                        );
                        double bDistance = ServerLocation.distance(location, b.getLocation());
                        ServerLocation t = this.protocol.getTargetBlock(bDistance, PlayerUtils.chunk);

                        if (t != null) {
                            double tDistance = t.distance(location);

                            if (tDistance < bDistance) {
                                this.interact.cancel(
                                        "interact"
                                                + ", interacted-block: " + BlockUtils.blockToString(b)
                                                + ", blocking-block: " + BlockUtils.blockToString(t.getBlock())
                                                + ", interacted-distance: " + bDistance
                                                + ", blocking-distance: " + tDistance,
                                        null,
                                        1
                                );

                                if (this.interact.prevent()) {
                                    event.setCancelled(true);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private boolean canDo(Block b) {
        return b.getWorld() == this.protocol.getWorld()
                && !TreeFeller.canCancel(b);
    }

    @Override
    protected boolean canRun() {
        if (!this.protocol.isFlying()
                && !Vehicles.has(this.protocol, Vehicles.DRILL)
                && !PrinterMode.isUsing(this.protocol)
                && Attributes.getAmount(this.protocol, Attributes.PLAYER_BLOCK_INTERACTION_RANGE) == Double.MIN_VALUE) {
            GameMode gameMode = this.protocol.getGameMode();
            return gameMode == GameMode.SURVIVAL
                    || gameMode == GameMode.ADVENTURE;
        }
        return false;
    }
}
