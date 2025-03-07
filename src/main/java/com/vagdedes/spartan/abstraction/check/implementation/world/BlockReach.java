package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedDetection;
import com.vagdedes.spartan.abstraction.event.CBlockPlaceEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockReach extends CheckRunner {

    private final CheckDetection
            breaking,
            interact,
            place,
            raytrace;

    public BlockReach(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.breaking = new ImplementedDetection(this, null, null, "breaking", true);
        this.interact = new ImplementedDetection(this, null, null, "interact", true);
        this.place = new ImplementedDetection(this, null, null, "place", true);
        this.raytrace = new ImplementedDetection(this, null, null, "raytrace", true);
    }

    private boolean canDo(Block b) {
        return this.protocol.getWorld().equals(b.getWorld());
    }

    private double getLimit() {
        double limit = 7.0;
        return Math.max(
                hackType.getCheck().getDecimalOption("overall_distance", limit),
                limit
        );
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof BlockBreakEvent) {
            this.breaking.call(() -> {
                BlockBreakEvent event = (BlockBreakEvent) object;
                Block block = event.getBlock();

                if (canDo(block)
                        && !ItemsAdder.is(block)
                        && !TreeFeller.canCancel(block)) {
                    double distance = SpartanLocation.distance(this.protocol.getLocationOrVehicle(), block.getLocation()),
                            limit = getLimit();

                    if (distance >= limit) {
                        this.breaking.cancel(
                                "breaking, distance: " + distance
                                        + ", limit: " + limit
                                        + ", block: " + BlockUtils.blockToString(block)
                        );

                        if (this.breaking.prevent()) {
                            event.setCancelled(true);
                        }
                    }
                }
            });
        } else if (object instanceof PlayerInteractEvent) {
            this.interact.call(() -> {
                Block block = ((PlayerInteractEvent) object).getClickedBlock();

                if (block != null
                        && canDo(block)
                        && !ItemsAdder.is(block)
                        && BlockUtils.isChangeable(block.getType())) {
                    double distance = SpartanLocation.distance(this.protocol.getLocationOrVehicle(), block.getLocation()),
                            limit = getLimit();

                    if (distance >= limit) {
                        this.interact.cancel(
                                "interact, distance: " + distance
                                        + ", limit: " + limit
                                        + ", block: " + BlockUtils.blockToString(block)
                        );
                    }
                }
            });
        } else if (object instanceof CBlockPlaceEvent) {
            if (this.place.canCall()
                    || this.raytrace.canCall()) {
                CBlockPlaceEvent event = (CBlockPlaceEvent) object;

                if (BlockUtils.isSolid(event.placedAgainstBlock.getType())) {
                    if (canDo(event.placedBlock)
                            && !ItemsAdder.is(event.placedBlock)
                            && BlockUtils.isSolid(event.placedBlock.getType())
                            && !BlockUtils.isScaffoldingBlock(event.placedBlock.getType())) {
                        Location to = this.protocol.getLocationOrVehicle().clone().add(
                                0,
                                this.protocol.getEyeHeight(),
                                0
                        );
                        double distance = SpartanLocation.distance(to, event.placedBlock.getLocation()),
                                ab_distance = SpartanLocation.distance(to, event.placedAgainstBlock.getLocation());

                        if (!this.protocol.isBedrockPlayer()
                                && BlockUtils.isFullSolid(event.placedBlock.getType())
                                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                                || event.placedBlock.getType() != Material.BARRIER)
                                && this.protocol.getVehicle() == null
                                && distance > (ab_distance + 0.3)) {
                            this.raytrace.call(() -> this.raytrace.cancel(
                                    "raytrace, distance: " + distance
                                            + ", block-against-distance: " + ab_distance
                                            + ", block: " + BlockUtils.blockToString(event.placedBlock)
                            ));
                        } else {
                            this.place.call(() -> {
                                double limit = getLimit();

                                if (distance >= limit) {
                                    this.place.cancel(
                                            "place, distance: " + distance
                                                    + ", limit: " + limit
                                                    + ", block: " + BlockUtils.blockToString(event.placedBlock)
                                    );
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean canRun() {
        return !Compatibility.CompatibilityType.MC_MMO.isFunctional()
                && !Compatibility.CompatibilityType.CRAFT_BOOK.isFunctional()
                && !PrinterMode.isUsing(this.protocol)
                && !AureliumSkills.isUsing(this.protocol)
                && !MineBomb.isUsing(this.protocol)
                && Attributes.getAmount(this.protocol, Attributes.PLAYER_BLOCK_INTERACTION_RANGE) == 0.0;
    }
}
