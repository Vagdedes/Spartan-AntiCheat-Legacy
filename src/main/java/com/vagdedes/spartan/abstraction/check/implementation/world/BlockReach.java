package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
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
            unusual;

    public BlockReach(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.breaking = new ImplementedProbabilityDetection(this, null, null, "breaking", true);
        this.interact = new ImplementedProbabilityDetection(this, null, null, "interact", true);
        this.place = new ImplementedProbabilityDetection(this, null, null, "place", true);
        this.unusual = new ImplementedProbabilityDetection(this, null, null, "unusual", true);
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
                Block block = ((BlockBreakEvent) object).getBlock();

                if (canDo(block)
                        && !TreeFeller.canCancel(block)) {
                    double distance = SpartanLocation.distance(this.protocol.getLocationOrVehicle(), block.getLocation()),
                            limit = getLimit();

                    if (distance >= limit) {
                        this.breaking.cancel(
                                "breaking, distance: " + distance
                                        + ", limit: " + limit
                                        + ", block: " + BlockUtils.blockToString(block)
                        );
                    }
                }
            });
        } else if (object instanceof PlayerInteractEvent) {
            this.interact.call(() -> {
                Block block = ((PlayerInteractEvent) object).getClickedBlock();

                if (canDo(block)
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
        } else if (this.place.canCall()
                || this.unusual.canCall()) {
            Block[] blocks = (Block[]) object;
            Block blockAgainst = blocks[1];

            if (BlockUtils.isSolid(blockAgainst.getType())) {
                Block block = blocks[0];

                if (canDo(block)
                        && BlockUtils.isSolid(block.getType())
                        && !BlockUtils.isScaffoldingBlock(block.getType())) {
                    Location to = this.protocol.getLocationOrVehicle();
                    double distance = SpartanLocation.distance(to, block.getLocation()),
                            ab_distance = SpartanLocation.distance(to, blockAgainst.getLocation());

                    if (!this.protocol.bukkitExtra.isBedrockPlayer()
                            && BlockUtils.isFullSolid(block.getType())
                            && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                            || block.getType() != Material.BARRIER)
                            && this.protocol.bukkitExtra.getVehicle() == null
                            && distance > (ab_distance + GroundUtils.boundingBox + 0.1)) {
                        this.unusual.call(() -> {
                            this.unusual.cancel(
                                    "unusual, distance: " + distance
                                            + ", block-against-distance: " + ab_distance
                                            + ", block: " + BlockUtils.blockToString(block)
                            );
                        });
                    } else {
                        this.place.call(() -> {
                            double limit = getLimit();

                            if (distance >= limit) {
                                this.place.cancel(
                                        "place, distance: " + distance
                                                + ", limit: " + limit
                                                + ", block: " + BlockUtils.blockToString(block)
                                );
                            }
                        });
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
                && !MineBomb.isUsing(this.protocol.bukkitExtra)
                && Attributes.getAmount(this.protocol, Attributes.PLAYER_BLOCK_INTERACTION_RANGE) == 0.0;
    }
}
