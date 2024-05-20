package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.GroundUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.List;

public class ImpossibleActions extends CheckExecutor {

    private final Buffer buffer;

    public ImpossibleActions(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.buffer = new Buffer(player);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof BlockPlaceEvent) {
            BlockPlaceEvent event = (BlockPlaceEvent) object;
            SpartanBlock block = new SpartanBlock(event.getBlock());
            SpartanLocation playerLocation = player.movement.getLocation();
            int playerY = playerLocation.getBlockY(),
                    blockY = event.getBlock().getY();

            if (blockY < playerY) {
                Block blockAgainst = event.getBlockAgainst();
                BlockFace blockFace = blockAgainst.getFace(event.getBlock());

                switch (blockFace) {
                    case EAST:
                    case EAST_NORTH_EAST:
                    case EAST_SOUTH_EAST:
                    case WEST:
                    case WEST_NORTH_WEST:
                    case WEST_SOUTH_WEST:
                    case NORTH:
                    case NORTH_EAST:
                    case NORTH_NORTH_EAST:
                    case SOUTH:
                    case SOUTH_EAST:
                    case SOUTH_SOUTH_EAST:
                        double directionDistance = blockFace.getDirection().distance(playerLocation.getDirection());

                        if (directionDistance <= 1.35) {
                            cancel(
                                    "type: scaffold(direction)"
                                            + ", vector-distance: " + directionDistance
                                            + ", block-distance: " + playerLocation.distance(blockAgainst.getLocation())
                            );
                        } else if (!player.bedrockPlayer) {
                            float pitch = playerLocation.getPitch();

                            if (pitch <= 0.0) {
                                cancel(
                                        "type: scaffold(up)"
                                                + ", pitch: " + pitch
                                                + ", block-distance: " + playerLocation.distance(blockAgainst.getLocation())
                                );
                            } else {
                                List<SpartanLocation> fromLocations = player.movement.getLocations();
                                int requiredSize = 6;

                                if (fromLocations.size() >= requiredSize) { // 6-1 comparisons = 5
                                    fromLocations = fromLocations.subList(fromLocations.size() - requiredSize, fromLocations.size());
                                    Iterator<SpartanLocation> iterator = fromLocations.iterator();
                                    SpartanLocation previousLocation = iterator.next();
                                    double yawDeviation = 0.0;

                                    while (iterator.hasNext()) {
                                        SpartanLocation loopLocation = iterator.next();
                                        double diff = Math.abs(previousLocation.getYaw() - loopLocation.getYaw());
                                        yawDeviation += diff * diff;
                                        previousLocation = loopLocation;
                                    }
                                    yawDeviation = Math.sqrt(yawDeviation / (requiredSize - 1.0));

                                    if (yawDeviation >= 90.0) {
                                        cancel(
                                                "type: scaffold(yaw)"
                                                        + ", deviation: " + yawDeviation
                                                        + ", block-distance: " + playerLocation.distance(blockAgainst.getLocation())
                                        );
                                    }
                                }
                            }
                        }
                        break;
                    case UP:
                        if (AlgebraUtils.getHorizontalDistance(
                                playerLocation.getBlockLocation(),
                                block.getLocation()
                        ) <= 1.0) {
                            ItemStack handItem = player.getItemInHand();
                            int jumpLevel = PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) + 1,
                                    motionTicks = PlayerUtils.getJumpTicks(jumpLevel);
                            double motionSum = PlayerUtils.getJumpMotionSum(jumpLevel),
                                    blockHeight = GroundUtils.getMinHeight(
                                            handItem == null ? Material.AIR : handItem.getType()
                                    ),
                                    deltaXZ = player.movement.getEventHorizontal();
                            int deltaYMinus1 = playerY - blockY,
                                    possiblePlacements = AlgebraUtils.integerFloor(
                                            motionSum / (blockHeight == 0.0 ? 1.0 : blockHeight)
                                    ) + deltaYMinus1;

                            if (deltaXZ > 0.1) {
                                possiblePlacements += AlgebraUtils.integerCeil(deltaXZ / 0.25);
                            }
                            int hash = (block.x * SpartanBukkit.hashCodeMultiplier) + block.z;

                            if (buffer.count("tower" + hash, motionTicks) > possiblePlacements) {
                                buffer.remove("tower" + hash);
                                cancel(
                                        "type: tower"
                                                + ", placements: " + possiblePlacements
                                                + ", horizontal: " + deltaXZ
                                                + ", vertical: " + deltaYMinus1,
                                        1.0,
                                        player.movement.getDetectionLocation(),
                                        0,
                                        true
                                );
                            }
                        }
                    default:
                        break;
                }
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
                        cancel("type: actions, action: " + action_string + ", case: " + cases + ", block: " + BlockUtils.blockToString(new SpartanBlock(b)));
                    }
                }
            }
        }
    }

}
