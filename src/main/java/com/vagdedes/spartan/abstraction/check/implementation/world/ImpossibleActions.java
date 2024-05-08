package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ImpossibleActions extends CheckExecutor {

    public static final Enums.HackType check = Enums.HackType.ImpossibleActions;

    public ImpossibleActions(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        if (canDo()) {
            if (object instanceof BlockPlaceEvent) {
                // todo tower, scaffold
            } else {
                PlayerInteractEvent event = (PlayerInteractEvent) object;
                Action action = event.getAction();

                if ((action == Action.LEFT_CLICK_AIR
                        || action == Action.LEFT_CLICK_BLOCK
                        || action == Action.RIGHT_CLICK_AIR
                        || action == Action.RIGHT_CLICK_BLOCK)
                        && player.canRunCheck(hackType, true)
                        && hackType.getCheck().getBooleanOption("check_actions", true)
                        && !Compatibility.CompatibilityType.MC_MMO.isFunctional()) {
                    int cases;

                    if (player.isSleeping()) {
                        cases = 1;
                    } else if (player.isDead()) {
                        cases = 2;
                    } else if (player.getOpenInventory().isUsingTheCursor()) {
                        cases = 3;
                    } else {
                        cases = 0;
                    }

                    if (cases != 0) {
                        String action_string = action.toString().toLowerCase().replace("_", "-");
                        Block b = event.getClickedBlock();

                        if (b == null) {
                            cancel("type: actions, action: " + action_string + ", case: " + cases);
                        } else {
                            cancel("type: actions, action: " + action_string + ", case: " + cases + ", block: " + BlockUtils.blockToString(new SpartanBlock(player, b)));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void runInternal(boolean cancelled) {
        player.getViolations(hackType).addDisableCause(hackType.toString(), "actions", 10);
    }

    @Override
    public void scheduler() {

    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true);
    }

}
