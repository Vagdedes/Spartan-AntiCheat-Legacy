package com.vagdedes.spartan.checks.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.BouncingBlocks;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ImpossibleActions extends CheckExecutor {

    public static final Enums.HackType check = Enums.HackType.ImpossibleActions;

    private TowerPosition cache = null;

    private static final Material soil = MaterialUtils.get("soil");
    private static final Material lily_pad = MaterialUtils.get("lily_pad");

    public ImpossibleActions(SpartanPlayer player) {
        super(check, player);
    }

    private static class TowerPosition {

        private int blockY;
        private long time;

        public TowerPosition(int blockY) {
            this.blockY = blockY;
            this.time = System.currentTimeMillis();
        }

        public void setBlockY(int blockY) {
            this.blockY = blockY;
            this.time = System.currentTimeMillis();
        }

        public long getTimePassed() {
            return System.currentTimeMillis() - time;
        }
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (object instanceof BlockPlaceEvent) {
            if (canDo()) {
                BlockPlaceEvent event = (BlockPlaceEvent) object;
                Block block = event.getBlock(),
                        blockAgainst = event.getBlockAgainst();
                runPlace(
                        new SpartanBlock(player, block),
                        new SpartanBlock(player, blockAgainst),
                        block.getFace(blockAgainst)
                );
            }
        } else if (object instanceof BlockBreakEvent) {
            if (canDo()) {
                SpartanBlock b = new SpartanBlock(player, ((BlockBreakEvent) object).getBlock());
                SpartanLocation bloc = b.getLocation();

                if (hackType.getCheck().getBooleanOption("check_item_usage", true)
                        && !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_7)
                        && !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)
                        && player.isUsingItem() && !BlockUtils.isSensitive(player, bloc)) {
                    cancel("type: item-usage, event: break, block: " + BlockUtils.blockToString(b));
                } else if (hackType.getCheck().getBooleanOption("check_liquids", true)
                        && !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                    Material m = bloc.getBlock().material;

                    if (BlockUtils.isBaseLiquid(m)) {
                        cancel("type: liquids(break), block: " + BlockUtils.materialToString(m));
                    }
                }
            }
        } else if (canDo()) {
            PlayerInteractEvent event = (PlayerInteractEvent) object;
            Action action = event.getAction();

            if ((action == Action.LEFT_CLICK_AIR
                    || action == Action.LEFT_CLICK_BLOCK
                    || action == Action.RIGHT_CLICK_AIR
                    || action == Action.RIGHT_CLICK_BLOCK)
                    && player.canRunCheck(hackType, true)
                    && hackType.getCheck().getBooleanOption("check_actions", true)
                    && !Compatibility.CompatibilityType.mcMMO.isFunctional()) {
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
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        player.getViolations(hackType).addDisableCause(hackType.toString(), "actions", 10);
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true);
    }

    private void runPlace(SpartanBlock block, SpartanBlock blockAgainst, BlockFace blockFace) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19)) {
            ItemStack leggings = player.getInventory().getLeggings();

            if (leggings != null
                    && leggings.getEnchantmentLevel(Enchantment.SWIFT_SNEAK) > 0) {
                return;
            }
        }
        Material itemStackType = player.getItemInHand().getType();
        SpartanLocation bloc = block.getLocation();
        Material blockType = block.material;
        Material blockAgainstType = blockAgainst.material;
        SpartanLocation blockAgainstLocation = blockAgainst.getLocation();
        boolean sameItem = itemStackType == blockType;

        if (hackType.getCheck().getBooleanOption("check_liquids", true)
                && blockType.isBlock() && sameItem && blockType != lily_pad
                && !BlockUtils.areEntityBlocks(blockType) && !BlockUtils.areCarpets(blockType)
                && BlockUtils.isSolid(blockType)

                && (BlockUtils.isBaseLiquid(blockAgainstType)

                || blockFace == BlockFace.SELF
                && BlockUtils.isFullSolid(blockType)
                && blockAgainstLocation.distance(bloc) == 0.0
                && bloc.getBlock().material == Material.AIR)) {
            cancel(
                    "type: liquids(place), block: " + BlockUtils.materialToString(blockType)
                            + ", block-against: " + BlockUtils.materialToString(blockAgainstType)
                            + ", block-face: " + blockFace.toString().toLowerCase());
        } else if (blockFace == BlockFace.SELF
                && hackType.getCheck().getBooleanOption("check_air_placement", true)
                && !BlockUtils.areWoods(blockType)
                && bloc.areBlocks(Material.AIR, 1, false)) {
            cancel("type: air-placement, block: " + BlockUtils.materialToString(blockType)
                    + ", block-against: " + BlockUtils.materialToString(blockAgainstType));
        } else if (blockType != Material.FIRE && blockType.isBlock() && !player.movement.wasFlying() && !blockType.equals(soil)) {
            GameMode gameMode = player.getGameMode();

            if (gameMode != GameMode.SURVIVAL && gameMode != GameMode.ADVENTURE) {
                return;
            }

            if (BlockUtils.isFullSolid(itemStackType) // Placed block must be fully solid
                    && BlockUtils.isSolid(blockAgainstType) // Block against must be solid
                    && blockFace != BlockFace.SELF) { // Block-face handled by the BlockReach check
                SpartanLocation to = player.movement.getLocation();
                int blockY = to.getBlockY();
                double box = to.getY() - blockY;

                if (!player.movement.wasInLiquids()  // Ensure the player is not inside liquid blocks
                        && (player.isOnGround()
                        || player.isOnGroundCustom()
                        || PlayerUtils.isOnGround(player, to, 0, -box, 0)
                        || PlayerUtils.isOnGround(player, to, 0, -1, 0)
                        || PlayerUtils.isOnGround(player, to, 0, -(1 + box), 0)
                        || player.movement.getTicksOnAir() <= 11)) { // Ensure the player is on ground or close to it
                    SpartanLocation blockLocation = block.getLocation();
                    double verticalDistance = blockY - blockLocation.getBlockY();

                    if (verticalDistance >= 0 && verticalDistance <= 2) { // Ensure the block is below or at max 2 blocks away vertically
                        double horizontalDistance = AlgebraUtils.getHorizontalDistance(to, blockAgainstLocation);
                        boolean scaffold = blockFace != BlockFace.DOWN && blockFace != BlockFace.UP
                                && blockLocation.getSurroundingSolidBlockCount(1, 1, 2) <= 1;
                        boolean access;

                        if (scaffold) {
                            access = hackType.getCheck().getBooleanOption("check_scaffold", true)
                                    && blockY == (blockAgainst.y + 1) && horizontalDistance < 1.5;
                        } else if (hackType.getCheck().getBooleanOption("check_tower", true)
                                && !BouncingBlocks.isBelow(player)) {
                            if (cache == null) {
                                access = false;
                                cache = new TowerPosition(blockY);
                            } else {
                                access = cache.getTimePassed() <= 1_000L && blockY > cache.blockY;
                                cache.setBlockY(blockY);
                            }
                        } else {
                            access = false;
                        }

                        if (access) {
                            Buffer buffer = player.getBuffer(hackType);
                            String scenario = scaffold ? "scaffold" : "tower";

                            if (buffer.start(scenario, 20) >= (!scaffold ? 4 : 5)) {
                                buffer.remove(scenario);
                                cancel("type: " + scenario
                                                + ", block: " + BlockUtils.materialToString(blockType)
                                                + ", block-against: " + BlockUtils.materialToString(blockAgainstType)
                                                + ", hand-item: " + BlockUtils.materialToString(itemStackType)
                                                + ", block-face: " + blockFace
                                                + ", vertical-distance: " + verticalDistance
                                                + ", horizontal-distance: " + horizontalDistance
                                                + ", box: " + box,
                                        null, 20);
                            }
                        }
                    }
                }
            }
        }
    }

}
