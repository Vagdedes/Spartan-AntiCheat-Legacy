package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.GroundUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockReach extends CheckExecutor {

    private static final double minimumLimit = 6.0;

    public BlockReach(SpartanPlayer player) {
        super(HackType.BlockReach, player);
    }

    private boolean canDo(SpartanBlock b) {
        return player.getWorld().equals(b.getLocation().world);
    }

    private double getLimit() {
        return Math.max(
                hackType.getCheck().getDecimalOption("overall_distance", minimumLimit),
                minimumLimit
        );
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof BlockBreakEvent) {
            SpartanBlock block = new SpartanBlock(player, ((BlockBreakEvent) object).getBlock());

            if (canDo(block)
                    && !TreeFeller.canCancel(player, block)) {
                double distance = player.movement.getLocation().distance(block.getLocation()),
                        limit = getLimit();

                if (distance >= limit) {
                    cancel("type: break, distance: " + distance + ", limit: " + limit + ", block: " + BlockUtils.blockToString(block));
                }
            }
        } else if (object instanceof PlayerInteractEvent) {
            SpartanBlock block = new SpartanBlock(player, ((PlayerInteractEvent) object).getClickedBlock());

            if (canDo(block)) {
                SpartanLocation bloc = block.getLocation();

                if (BlockUtils.isChangeable(bloc.getBlock().material)) {
                    double distance = player.movement.getLocation().distance(bloc),
                            limit = getLimit();

                    if (distance >= limit) {
                        cancel("type: interact, distance: " + distance + ", limit: " + limit + ", block: " + BlockUtils.blockToString(block));
                    }
                }
            }
        } else {
            BlockPlaceEvent event = (BlockPlaceEvent) object;
            SpartanBlock blockAgainst = new SpartanBlock(player, event.getBlockAgainst());

            if (BlockUtils.isSolid(blockAgainst.material)) {
                SpartanBlock block = new SpartanBlock(player, event.getBlock());

                if (canDo(block)
                        && BlockUtils.isSolid(block.material)
                        && !BlockUtils.isScaffoldingBlock(block.material)) {
                    SpartanLocation to = player.movement.getLocation();
                    double distance = to.distance(block.getLocation()),
                            ab_distance = to.distance(blockAgainst.getLocation());

                    if (!player.bedrockPlayer
                            && player.getVehicle() == null
                            && distance > (ab_distance + GroundUtils.boundingBox)) {
                        cancel("type: unusual, distance: " + distance + ", block-against-distance: " + ab_distance + ", block: " + BlockUtils.blockToString(block));
                    } else {
                        double limit = getLimit();

                        if (distance >= limit) {
                            cancel("type: place, distance: " + distance + ", limit: " + limit + ", block: " + BlockUtils.blockToString(block));
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
