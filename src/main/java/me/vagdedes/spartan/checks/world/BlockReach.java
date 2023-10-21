package me.vagdedes.spartan.checks.world;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import me.vagdedes.spartan.compatibility.manual.building.MineBomb;
import me.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import me.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.protections.LagLeniencies;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import org.bukkit.Material;
import org.bukkit.entity.Strider;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockReach extends CheckExecutor {

    private static final HackType check = HackType.BlockReach;
    private static final double
            minimumLimit = 6.0,
            extraMinimumLimit = 10.0;

    public BlockReach(SpartanPlayer player) {
        super(check, player);
    }

    private double getMultiplier(SpartanBlock b, boolean sensitive) {
        return player.isBedrockPlayer() || LagLeniencies.hasHighLatency(player, LagLeniencies.continentPing) || (Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()) ? 2 : // Building
                MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && player.getVehicle() instanceof Strider ? 1.5 :
                        sensitive && BlockUtils.isSensitive(player, b.getType()) ? Math.min(1 + LagLeniencies.getDelay(player), 1.5) : 1; // Building
    }

    private boolean canDo(SpartanBlock b) {
        return !BlockUtils.isScaffoldingBlock(b.getType())
                && !TreeFeller.canCancel(player, b);
    }

    private double getLimit() {
        return Math.max(
                hackType.getCheck().getDecimalOption("overall_distance", 7.5),
                player.wasFlying() ? MoveUtils.chunk : minimumLimit
        );
    }

    @Override
    public boolean handle(Object object) {
        if (object instanceof BlockBreakEvent) {
            if (canDo()) {
                SpartanBlock block = new SpartanBlock(player, ((BlockBreakEvent) object).getBlock());

                if (canDo(block) && BlockUtils.isSensitive(player, block.getType())) {
                    Material item = player.getItemInHand().getType();

                    if (item != Material.FLINT_AND_STEEL
                            && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) || item != Material.END_CRYSTAL)) {
                        SpartanLocation to = player.getLocation();
                        SpartanLocation bloc = block.getLocation();
                        double distance = Math.min(to.distance(bloc), to.clone().add(0, player.getEyeHeight(), 0).distance(bloc)),
                                limit = getLimit(),
                                multiplier = getMultiplier(block, false);

                        if (distance <= MoveUtils.chunk && distance >= (limit * multiplier)) {
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

                    if (BlockUtils.isChangeable(bloc)) {
                        SpartanLocation to = player.getLocation();
                        double distance = Math.min(to.distance(bloc), to.clone().add(0, player.getEyeHeight(), 0).distance(bloc)),
                                limit = getLimit(),
                                multiplier = getMultiplier(block, true);

                        if (distance <= MoveUtils.chunk && distance >= (limit * multiplier)) {
                            cancel("type: interact, distance: " + distance + ", limit: " + limit + ", multiplier: " + multiplier + ", block: " + BlockUtils.blockToString(block));
                        }
                    }
                }
            }
        } else if (canDo()) {
            BlockPlaceEvent event = (BlockPlaceEvent) object;
            SpartanBlock blockAgainst = new SpartanBlock(player, event.getBlockAgainst());
            SpartanLocation baLoc = blockAgainst.getLocation();

            if (BlockUtils.isFullSolid(baLoc)
                    && player.getItemInHand().getType() != (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.BONE_MEAL : Material.getMaterial("INK_SACK"))) {
                SpartanBlock block = new SpartanBlock(player, event.getBlock());
                Material type = block.getType();

                if (!BlockUtils.areBeds(type)
                        && type != Material.BEDROCK) {
                    SpartanLocation to = player.getLocation(), bloc = block.getLocation();
                    double ypos = bloc.getY() - to.getY(),
                            distance = Math.min(to.distance(bloc), to.clone().add(0, player.getEyeHeight(), 0).distance(bloc)),
                            ab_distance = to.distance(baLoc) + 0.4;

                    if (!player.isBedrockPlayer()
                            && player.getVehicle() == null
                            && !Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()
                            && ypos <= 0.5
                            && distance >= 1.4 && distance > ab_distance && distance <= MoveUtils.chunk) {
                        cancel("type: unusual, distance: " + distance + ", block-against-distance: " + ab_distance + ", vertical-distance: " + ypos + ", block: " + BlockUtils.blockToString(block));
                    } else {
                        double limit = getLimit(),
                                multiplier = getMultiplier(block, true);

                        if (distance <= MoveUtils.chunk && distance >= (limit * multiplier)) {
                            cancel("type: place, distance: " + distance + ", limit: " + limit + ", multiplier: " + multiplier + ", block: " + BlockUtils.blockToString(block));
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean run() {
        return false;
    }

    @Override
    protected boolean canDo() {
        return !Compatibility.CompatibilityType.mcMMO.isFunctional()
                && player.canRunCheck(hackType, true)
                && !PrinterMode.canCancel(player)
                && !MineBomb.canUse(player)
                && !AureliumSkills.canCancel(player)
                && !MineBomb.canUse(player)
                && !Compatibility.CompatibilityType.CraftBook.isFunctional();
    }
}
