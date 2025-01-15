package com.vagdedes.spartan.abstraction.check.implementation.world.impossibleactions;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.List;

public class ImpossibleActions extends CheckRunner {

    private final CheckDetection
            scaffoldDirection,
            scaffoldUp,
            scaffoldYaw,
            actions;
    private final Tower tower;
    private final ScaffoldAnalysis scaffoldAnalysis;

    public ImpossibleActions(Enums.HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.scaffoldDirection = new ImplementedProbabilityDetection(this, null, null, "scaffold_direction", true);
        this.scaffoldUp = new ImplementedProbabilityDetection(this, Check.DataType.JAVA, null, "scaffold_up", true);
        this.scaffoldYaw = new ImplementedProbabilityDetection(this, Check.DataType.JAVA, null, "scaffold_yaw", true);
        this.actions = new ImplementedProbabilityDetection(this, null, null, "actions", true);
        this.tower = new Tower(this);
        this.scaffoldAnalysis = new ScaffoldAnalysis(this);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof Block[]) {
            Block[] blocks = (Block[]) object;
            Block block = blocks[0];
            Location playerLocation = this.protocol.getLocationOrVehicle();
            int playerY = playerLocation.getBlockY(),
                    blockY = block.getY();
            this.scaffoldAnalysis.run(block);

            if (blockY < playerY) {
                Block blockAgainst = blocks[1];
                BlockFace blockFace = blockAgainst.getFace(block);

                if (blockFace != null) {
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
                            if (this.scaffoldDirection.canCall()
                                    || this.scaffoldUp.canCall()
                                    || this.scaffoldYaw.canCall()) {
                                double directionDistance = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                                        ? this.getDirection(blockFace).distance(playerLocation.getDirection())
                                        : Double.MAX_VALUE;

                                if (directionDistance <= 1.25) {
                                    this.scaffoldDirection.call(() -> this.scaffoldDirection.cancel(
                                            "scaffold(direction)"
                                                    + ", vector-distance: " + directionDistance
                                                    + ", block-distance: " + SpartanLocation.distance(playerLocation, blockAgainst.getLocation())
                                    ));
                                } else {
                                    float pitch = playerLocation.getPitch();

                                    if (pitch <= 0.0) {
                                        this.scaffoldUp.call(() -> this.scaffoldUp.cancel(
                                                "scaffold(up)"
                                                        + ", pitch: " + pitch
                                                        + ", block-distance: " + SpartanLocation.distance(playerLocation, blockAgainst.getLocation())
                                        ));
                                    } else {
                                        this.scaffoldYaw.call(() -> {
                                            List<SpartanLocation> fromLocations = this.protocol.spartan.movement.getLocations();
                                            int requiredComparisons = 5, requiredSize = requiredComparisons + 1;

                                            if (fromLocations.size() >= requiredSize) { // 6-1 comparisons = 5
                                                fromLocations = fromLocations.subList(fromLocations.size() - requiredSize, fromLocations.size());
                                                Iterator<SpartanLocation> iterator = fromLocations.iterator();
                                                SpartanLocation previousLocation = iterator.next();
                                                double yawDeviation = 0.0;

                                                while (iterator.hasNext()) {
                                                    SpartanLocation loopLocation = iterator.next();

                                                    if (loopLocation.timePassed() <= requiredComparisons * 2 * TPS.tickTime) {
                                                        double diff = (previousLocation.getYaw() - loopLocation.getYaw()) % 360.0f;
                                                        yawDeviation += diff * diff;
                                                        previousLocation = loopLocation;
                                                    } else {
                                                        return;
                                                    }
                                                }
                                                yawDeviation = Math.sqrt(yawDeviation / ((double) requiredComparisons));

                                                if (yawDeviation >= 270.0) {
                                                    this.scaffoldYaw.cancel(
                                                            "scaffold(yaw)"
                                                                    + ", deviation: " + yawDeviation
                                                                    + ", block-distance: " + SpartanLocation.distance(playerLocation, blockAgainst.getLocation())
                                                    );
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                            break;
                        case UP:
                            this.tower.run(block, playerLocation, playerY, blockY);
                            break;
                        default:
                            break;
                    }
                }
            }
        } else {
            this.actions.call(() -> {
                PlayerInteractEvent event = (PlayerInteractEvent) object;
                Action action = event.getAction();

                if ((action == Action.LEFT_CLICK_AIR
                        || action == Action.LEFT_CLICK_BLOCK
                        || action == Action.RIGHT_CLICK_AIR
                        || action == Action.RIGHT_CLICK_BLOCK)
                        && !Compatibility.CompatibilityType.MC_MMO.isFunctional()) {
                    int cases;

                    if (this.protocol.bukkit().isSleeping()) {
                        cases = 1;
                    } else if (this.protocol.spartan.isDead()) {
                        cases = 2;
                    } else if (BlockUtils.hasMaterial(this.protocol.bukkit().getOpenInventory().getCursor())) {
                        cases = 3;
                    } else {
                        cases = 0;
                    }

                    if (cases != 0) {
                        String action_string = action.toString().toLowerCase().replace("_", "-");
                        Block b = event.getClickedBlock();

                        if (b == null) {
                            this.actions.cancel("actions, action: " + action_string + ", case: " + cases);
                        } else {
                            this.actions.cancel("actions, action: " + action_string + ", case: " + cases + ", block: " + BlockUtils.blockToString(b));
                        }
                    }
                }
            });
        }
    }

    private Vector getDirection(BlockFace blockFace) {
        Vector direction = new Vector(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ());

        if (blockFace.getModX() != 0 || blockFace.getModY() != 0 || blockFace.getModZ() != 0) {
            direction.normalize();
        }
        return direction;
    }

}
