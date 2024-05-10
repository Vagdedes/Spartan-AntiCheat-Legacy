package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class ImpossibleActions extends CheckExecutor {

    public ImpossibleActions(SpartanPlayer player) {
        super(Enums.HackType.ImpossibleActions, player);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        if (object instanceof BlockPlaceEvent) {
            BlockPlaceEvent event = (BlockPlaceEvent) object;
            SpartanBlock block = new SpartanBlock(player, event.getBlock());
            SpartanLocation playerLocation = player.movement.getLocation();
            int playerY = playerLocation.getBlockY(),
                    blockY = event.getBlock().getY();

            if (blockY < playerY) {
                if (AlgebraUtils.getHorizontalDistance(playerLocation.getBlockLocation(), block.getLocation()) <= 1.0) {
                    ItemStack handItem = player.getItemInHand();
                    int jumpLevel = PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) + 1,
                            motionTicks = PlayerUtils.getJumpTicks(jumpLevel);
                    double motionSum = PlayerUtils.getJumpMotionSum(jumpLevel),
                            blockHeight = GroundUtils.getMinHeight(
                                    handItem == null ? Material.AIR : handItem.getType()
                            ),
                            deltaXZ = player.movement.getNmsHorizontalDistance();
                    int deltaYMinus1 = playerY - blockY - 1,
                            possiblePlacements = AlgebraUtils.integerFloor(
                                    motionSum / (blockHeight == 0.0 ? 1.0 : blockHeight)
                            ) + deltaYMinus1;

                    if (deltaXZ > 0.1) {
                        possiblePlacements += AlgebraUtils.integerCeil(deltaXZ / 0.25);
                    }

                    if (this.getBuffer().count("tower", motionTicks) > possiblePlacements) {
                        this.getBuffer().remove("tower");
                        cancel(
                                "type: tower"
                                        + ", placements: " + possiblePlacements
                                        + ", horizontal: " + deltaXZ
                                        + ", vertical: " + deltaYMinus1,
                                player.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    }
                }
            } else if (blockY == playerY) {
                // todo horizontal bridge
            } else {
                // todo upwards bridge
            }
        } else {
            PlayerInteractEvent event = (PlayerInteractEvent) object;
            Action action = event.getAction();

            if ((action == Action.LEFT_CLICK_AIR
                    || action == Action.LEFT_CLICK_BLOCK
                    || action == Action.RIGHT_CLICK_AIR
                    || action == Action.RIGHT_CLICK_BLOCK)
                    && hackType.getCheck().getBooleanOption("check_actions", true)
                    && !Compatibility.CompatibilityType.MC_MMO.isFunctional()) {
                int cases;

                if (player.isSleeping()) {
                    cases = 1;
                } else if (player.isDead()) {
                    cases = 2;
                } else if (BlockUtils.hasMaterial(player.getOpenInventory().getCursor())) {
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

    @Override
    public void runInternal(boolean cancelled) {

    }

    @Override
    public void schedulerInternal() {

    }

    @Override
    protected boolean canDo() {
        return false;
    }

}
