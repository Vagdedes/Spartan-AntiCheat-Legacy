package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameMode;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class GhostHand extends CheckExecutor { // todo improve

    private static final HackType check = HackType.GhostHand;

    public GhostHand(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        if (object instanceof BlockBreakEvent) {
            BlockBreakEvent event = (BlockBreakEvent) object;

            if (event.isCancelled()) {
                player.getCooldowns(hackType).add("break-protection", 2);
            } else if (canDo()) {
                SpartanBlock b = new SpartanBlock(player, event.getBlock());

                if (canDo(b)
                        && hackType.getCheck().getBooleanOption("check_block_breaking", true)
                        && !BlockUtils.isSensitive(player, b.material)) {
                    SpartanBlock itb = player.getIllegalTargetBlock(b);

                    if (itb != null
                            && (!hackType.getCheck().getBooleanOption("check_fully_solid_blocks_only", true)
                            || BlockUtils.isFullSolid(itb.material))) {
                        cancel("type: break, block: " + BlockUtils.blockToString(b) + ", real-block: " + BlockUtils.blockToString(itb),
                                null, 1);
                    }
                }
            }
        } else if (canDo()) {
            PlayerInteractEvent event = (PlayerInteractEvent) object;

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                SpartanBlock b = new SpartanBlock(player, event.getClickedBlock());

                if (canDo(b)
                        || !hackType.getCheck().getBooleanOption("check_player_interactions", true)
                        || !BlockUtils.isChangeable(b.getLocation().getBlock().material)
                        && !BlockUtils.areEggs(player.getItemInHand().getType())) {
                    SpartanBlock itb = player.getIllegalTargetBlock(b);

                    if (itb != null
                            && (!hackType.getCheck().getBooleanOption("check_fully_solid_blocks_only", true)
                            || BlockUtils.isFullSolid(itb.material))) {
                        cancel("type: interact, block: " + BlockUtils.blockToString(b) + ", real-block: " + BlockUtils.blockToString(itb),
                                null, 1);
                    }
                }
            }
        }
    }

    @Override
    public void runInternal(boolean cancelled) {

    }

    @Override
    public void scheduler() {

    }

    private boolean canDo(SpartanBlock b) {
        return b.world == player.getWorld() && !BlockUtils.areWires(b.material);
    }

    @Override
    protected boolean canDo() {
        if (!player.bedrockPlayer
                && player.canRunCheck(hackType, true)
                && !player.movement.isFlying()
                && !player.getTrackers().has(Trackers.TrackerType.GAME_MODE)
                && !Vehicles.has(player, "drill")
                && !PrinterMode.canCancel(player)
                && !mcMMO.hasGeneralAbility(player)) {
            GameMode gameMode = player.getGameMode();
            return gameMode == GameMode.SURVIVAL
                    || gameMode == GameMode.ADVENTURE;
        }
        return false;
    }
}
