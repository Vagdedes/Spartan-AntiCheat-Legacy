package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedDetection;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
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
                    Location location = this.protocol.getLocationOrVehicle().clone().add(
                            0,
                            this.protocol.bukkit().getEyeHeight(),
                            0
                    );
                    double bDistance = SpartanLocation.distance(location, b.getLocation());
                    SpartanLocation t = this.protocol.bukkitExtra.getTargetBlock(bDistance, PlayerUtils.chunk);

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
                                this.protocol.bukkit().getEyeHeight(),
                                0
                        );
                        double bDistance = SpartanLocation.distance(location, b.getLocation());
                        SpartanLocation t = this.protocol.bukkitExtra.getTargetBlock(bDistance, PlayerUtils.chunk);

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
        return b.getWorld() == this.protocol.getWorld();
    }

    @Override
    protected boolean canRun() {
        if (!this.protocol.bukkitExtra.movement.isFlying()
                && !Vehicles.has(this.protocol, Vehicles.DRILL)
                && !PrinterMode.isUsing(this.protocol)
                && Attributes.getAmount(this.protocol, Attributes.PLAYER_BLOCK_INTERACTION_RANGE) == 0.0) {
            GameMode gameMode = this.protocol.bukkit().getGameMode();
            return gameMode == GameMode.SURVIVAL
                    || gameMode == GameMode.ADVENTURE;
        }
        return false;
    }
}
