package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameMode;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class GhostHand extends CheckExecutor {

    public GhostHand(SpartanPlayer player) {
        super(HackType.GhostHand, player);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof SpartanBlock) {
            SpartanBlock b = (SpartanBlock) object;

            if (canDo(b)
                    && hackType.getCheck().getBooleanOption("check_block_breaking", true)) {
                SpartanLocation location = player.movement.getLocation();
                double bDistance = location.distance(b.getLocation());
                SpartanBlock t = player.getTargetBlock(bDistance);

                if (t != null) {
                    double tDistance = location.distance(t.getLocation());

                    if (tDistance < bDistance) {
                        cancel(
                                "type: break"
                                        + ", interacted-block: " + BlockUtils.blockToString(b)
                                        + ", blocking-block: " + BlockUtils.blockToString(t)
                                        + ", interacted-distance: " + bDistance
                                        + ", blocking-distance: " + tDistance,
                                1.0,
                                null,
                                1
                        );
                    }
                }
            }
        } else {
            PlayerInteractEvent event = (PlayerInteractEvent) object;

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                SpartanBlock b = new SpartanBlock(event.getClickedBlock());

                if (canDo(b)
                        && hackType.getCheck().getBooleanOption("check_block_interactions", true)
                        && BlockUtils.isChangeable(b.getLocation().getBlock().material)) {
                    SpartanLocation location = player.movement.getLocation();
                    double bDistance = location.distance(b.getLocation());
                    SpartanBlock t = player.getTargetBlock(bDistance);

                    if (t != null) {
                        double tDistance = location.distance(t.getLocation());

                        if (tDistance < bDistance) {
                            cancel(
                                    "type: interact"
                                            + ", interacted-block: " + BlockUtils.blockToString(b)
                                            + ", blocking-block: " + BlockUtils.blockToString(t)
                                            + ", interacted-distance: " + bDistance
                                            + ", blocking-distance: " + tDistance,
                                    1.0,
                                    null,
                                    1
                            );
                        }
                    }
                }
            }
        }
    }

    private boolean canDo(SpartanBlock b) {
        return b.getWorld() == player.getWorld();
    }

    @Override
    protected boolean canRun() {
        if (!player.movement.isFlying()
                && !Vehicles.has(player, "drill")
                && !PrinterMode.isUsing(player)
                && !mcMMO.hasGeneralAbility(player)) {
            GameMode gameMode = player.getGameMode();
            return gameMode == GameMode.SURVIVAL
                    || gameMode == GameMode.ADVENTURE;
        }
        return false;
    }
}
