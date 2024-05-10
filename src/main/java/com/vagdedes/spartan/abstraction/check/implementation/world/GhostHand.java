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
import org.bukkit.event.player.PlayerInteractEvent;

public class GhostHand extends CheckExecutor {

    private static final HackType check = HackType.GhostHand;

    public GhostHand(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        if (canDo()) {
            if (object instanceof SpartanBlock) {
                SpartanBlock b = (SpartanBlock) object;

                if (canDo(b)
                        && hackType.getCheck().getBooleanOption("check_block_breaking", true)) {
                    SpartanBlock itb = player.getIllegalTargetBlock(b);

                    if (itb != null) {
                        cancel("type: break, block: " + BlockUtils.blockToString(b) + ", real-block: " + BlockUtils.blockToString(itb),
                                null, 1);
                    }
                }
            } else {
                PlayerInteractEvent event = (PlayerInteractEvent) object;

                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    SpartanBlock b = new SpartanBlock(player, event.getClickedBlock());

                    if (canDo(b)
                            && hackType.getCheck().getBooleanOption("check_block_interactions", true)
                            && BlockUtils.isChangeable(b.getLocation().getBlock().material)) {
                        SpartanBlock itb = player.getIllegalTargetBlock(b);

                        if (itb != null) {
                            cancel("type: interact, block: " + BlockUtils.blockToString(b) + ", real-block: " + BlockUtils.blockToString(itb),
                                    null, 1);
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

    private boolean canDo(SpartanBlock b) {
        return b.world == player.getWorld();
    }

    @Override
    protected boolean canDo() {
        if (!player.movement.isFlying()
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
