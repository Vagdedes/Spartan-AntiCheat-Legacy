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
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.entity.Strider;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockReach extends CheckExecutor { // todo improve

    private static final HackType check = HackType.BlockReach;
    private static final double minimumLimit = 6.0;

    public BlockReach(SpartanPlayer player) {
        super(check, player);
    }

    private double getMultiplier() {
        return player.bedrockPlayer ? 2 : // Building
                MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && player.getVehicle() instanceof Strider ? 1.5 :
                        1; // Building
    }

    private boolean canDo(SpartanBlock b) {
        return !BlockUtils.isScaffoldingBlock(b.material)
                && !TreeFeller.canCancel(player, b);
    }

    private double getLimit() {
        return Math.max(
                hackType.getCheck().getDecimalOption("overall_distance", 7.5),
                player.movement.isFlying() ? PlayerUtils.chunk : minimumLimit
        );
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        if (object instanceof BlockBreakEvent) {
            if (canDo()) {
                SpartanBlock block = new SpartanBlock(player, ((BlockBreakEvent) object).getBlock());

                if (canDo(block) && BlockUtils.isSensitive(player, block.material)) {
                    Material item = player.getItemInHand().getType();

                    if (item != Material.FLINT_AND_STEEL
                            && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) || item != Material.END_CRYSTAL)) {
                        SpartanLocation to = player.movement.getLocation();
                        SpartanLocation bloc = block.getLocation();
                        double distance = Math.min(to.distance(bloc), to.clone().add(0, player.getEyeHeight(), 0).distance(bloc)),
                                limit = getLimit(),
                                multiplier = getMultiplier();

                        if (distance <= PlayerUtils.chunk && distance >= (limit * multiplier)) {
                            cancel("type: break, distance: " + distance + ", limit: " + limit + ", multiplier: " + multiplier + ", block: " + BlockUtils.blockToString(block));
                        }
                    }
                }
            }
        } else if (object instanceof PlayerInteractEvent) {
            if (canDo()) {
                SpartanBlock block = new SpartanBlock(player, ((PlayerInteractEvent) object).getClickedBlock());

                if (canDo(block)) {
                    SpartanLocation bloc = block.getLocation();

                    if (BlockUtils.isChangeable(bloc.getBlock().material)) {
                        SpartanLocation to = player.movement.getLocation();
                        double distance = Math.min(to.distance(bloc), to.clone().add(0, player.getEyeHeight(), 0).distance(bloc)),
                                limit = getLimit(),
                                multiplier = getMultiplier();

                        if (distance <= PlayerUtils.chunk && distance >= (limit * multiplier)) {
                            cancel("type: interact, distance: " + distance + ", limit: " + limit + ", multiplier: " + multiplier + ", block: " + BlockUtils.blockToString(block));
                        }
                    }
                }
            }
        } else if (canDo()) {
            BlockPlaceEvent event = (BlockPlaceEvent) object;
            SpartanBlock blockAgainst = new SpartanBlock(player, event.getBlockAgainst());
            SpartanLocation baLoc = blockAgainst.getLocation();

            if (BlockUtils.isFullSolid(baLoc.getBlock().material)
                    && player.getItemInHand().getType() != (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.BONE_MEAL : Material.getMaterial("INK_SACK"))) {
                SpartanBlock block = new SpartanBlock(player, event.getBlock());

                if (!BlockUtils.areBeds(block.material)
                        && block.material != Material.BEDROCK) {
                    SpartanLocation to = player.movement.getLocation(), bloc = block.getLocation();
                    double ypos = bloc.getY() - to.getY(),
                            distance = Math.min(to.distance(bloc), to.clone().add(0, player.getEyeHeight(), 0).distance(bloc)),
                            ab_distance = to.distance(baLoc) + 0.4;

                    if (!player.bedrockPlayer
                            && player.getVehicle() == null
                            && ypos <= 0.5
                            && distance >= 1.4 && distance > ab_distance && distance <= PlayerUtils.chunk) {
                        cancel("type: unusual, distance: " + distance + ", block-against-distance: " + ab_distance + ", vertical-distance: " + ypos + ", block: " + BlockUtils.blockToString(block));
                    } else {
                        double limit = getLimit(),
                                multiplier = getMultiplier();

                        if (distance <= PlayerUtils.chunk && distance >= (limit * multiplier)) {
                            cancel("type: place, distance: " + distance + ", limit: " + limit + ", multiplier: " + multiplier + ", block: " + BlockUtils.blockToString(block));
                        }
                    }
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
        return !Compatibility.CompatibilityType.MC_MMO.isFunctional()
                && !PrinterMode.canCancel(player)
                && !MineBomb.canUse(player)
                && !AureliumSkills.canCancel(player)
                && !MineBomb.canUse(player)
                && !Compatibility.CompatibilityType.CRAFT_BOOK.isFunctional();
    }
}
