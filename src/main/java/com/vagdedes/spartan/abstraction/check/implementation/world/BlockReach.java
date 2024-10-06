package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockReach extends CheckExecutor {

    private final DetectionExecutor
            breaking,
            interact,
            place,
            unusual;

    public BlockReach(HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.breaking = new ImplementedDetectionExecutor(this, "breaking", true);
        this.interact = new ImplementedDetectionExecutor(this, "interact", true);
        this.place = new ImplementedDetectionExecutor(this, "place", true);
        this.unusual = new ImplementedDetectionExecutor(this, "unusual", true);
    }

    private boolean canDo(Block b) {
        return player.getWorld().equals(b.getWorld());
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
            Block block = ((BlockBreakEvent) object).getBlock();

            if (canDo(block)
                    && !TreeFeller.canCancel(player, block)) {
                double distance = player.movement.getLocation().distance(block),
                        limit = getLimit();

                if (distance >= limit) {
                    this.breaking.cancel("type: breaking, distance: " + distance + ", limit: " + limit + ", block: " + BlockUtils.blockToString(block));
                }
            }
        } else if (object instanceof PlayerInteractEvent) {
            Block block = ((PlayerInteractEvent) object).getClickedBlock();

            if (canDo(block)
                    && BlockUtils.isChangeable(block.getType())) {
                double distance = player.movement.getLocation().distance(block),
                        limit = getLimit();

                if (distance >= limit) {
                    this.interact.cancel("type: interact, distance: " + distance + ", limit: " + limit + ", block: " + BlockUtils.blockToString(block));
                }
            }
        } else {
            BlockPlaceEvent event = (BlockPlaceEvent) object;
            Block blockAgainst = event.getBlockAgainst();

            if (BlockUtils.isSolid(blockAgainst.getType())) {
                Block block = event.getBlock();

                if (canDo(block)
                        && BlockUtils.isSolid(block.getType())
                        && !BlockUtils.isScaffoldingBlock(block.getType())) {
                    SpartanLocation to = player.movement.getLocation();
                    double distance = to.distance(block),
                            ab_distance = to.distance(blockAgainst);

                    if (!player.isBedrockPlayer()
                            && BlockUtils.isFullSolid(block.getType())
                            && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                            || block.getType() != Material.BARRIER)
                            && player.getInstance().getVehicle() == null
                            && distance > (ab_distance + GroundUtils.boundingBox + 0.1)) {
                        this.unusual.cancel("type: unusual, distance: " + distance + ", block-against-distance: " + ab_distance + ", block: " + BlockUtils.blockToString(block));
                    } else {
                        double limit = getLimit();

                        if (distance >= limit) {
                            this.place.cancel("type: place, distance: " + distance + ", limit: " + limit + ", block: " + BlockUtils.blockToString(block));
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
                && !PrinterMode.isUsing(player)
                && !AureliumSkills.isUsing(player)
                && !MineBomb.isUsing(player);
    }
}
