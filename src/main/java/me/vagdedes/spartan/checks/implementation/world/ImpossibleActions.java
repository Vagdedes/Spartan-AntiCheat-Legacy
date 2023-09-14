package me.vagdedes.spartan.checks.implementation.world;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.BouncingBlocks;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ImpossibleActions {

    public static final Enums.HackType check = Enums.HackType.ImpossibleActions;

    private static final Map<UUID, TowerPosition> memory = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));

    private static final Material soil = MaterialUtils.get("soil");
    private static final Material lily_pad = MaterialUtils.get("lily_pad");

    // Objects

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

    // Handlers

    public static void remove(SpartanPlayer p) {
        memory.remove(p.getUniqueId());
    }

    public static void handleDeath(SpartanPlayer p) {
        check.getCheck().addDisabledUser(p.getUniqueId(), check.toString(), "actions", 10);
    }

    // Detections

    public static void runInteract(SpartanPlayer p, Action action, SpartanBlock b) {
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                || TPS.get(p) < TPS.excellent || !p.canRunCheck(check, true) || !check.getCheck().getBooleanOption("check_actions", true)
                || Compatibility.CompatibilityType.mcMMO.isFunctional()) {
            return;
        }
        int cases;

        if (p.isSleeping()) {
            cases = 1;
        } else if (p.isDead()) {
            cases = 2;
        } else if (PlayerData.isUsingTheCursor(p)) {
            cases = 3;
        } else {
            cases = 0;
        }

        if (cases != 0) {
            String action_string = action.toString().toLowerCase().replace("_", "-");

            if (b == null) {
                new HackPrevention(p, check, "type: actions, action: " + action_string + ", case: " + cases);
            } else {
                new HackPrevention(p, check, "type: actions, action: " + action_string + ", case: " + cases + ", block: " + BlockUtils.blockToString(b));
            }
        }
    }

    public static void runPlace(SpartanPlayer p, SpartanBlock b, SpartanBlock blockAgainst, BlockFace blockFace) {
        if (!p.canRunCheck(check, true)) {
            return;
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19)) {
            ItemStack leggings = p.getInventory().getLeggings();

            if (leggings != null
                    && leggings.getEnchantmentLevel(Enchantment.SWIFT_SNEAK) > 0) {
                return;
            }
        }
        Material itemStackType = p.getItemInHand().getType();
        SpartanLocation bloc = b.getLocation();
        Material blockType = b.getType();
        Material blockAgainstType = blockAgainst.getType();
        SpartanLocation blockAgainstLocation = blockAgainst.getLocation();
        boolean sameItem = itemStackType == blockType;

        if (check.getCheck().getBooleanOption("check_liquids", true) && blockType.isBlock() && sameItem && blockType != lily_pad
                && !BlockUtils.areEntityBlocks(blockType) && !BlockUtils.areCarpets(blockType) && BlockUtils.isSolid(blockType)

                && (BlockUtils.isBaseLiquid(blockAgainstType)

                || blockFace == BlockFace.SELF
                && BlockUtils.isFullSolid(blockType)
                && blockAgainstLocation.distance(bloc) == 0.0
                && bloc.getBlock().getType() == Material.AIR)) {
            new HackPrevention(p, check,
                    "type: liquids(place), b: " + BlockUtils.materialToString(blockType)
                            + ", block-against: " + BlockUtils.materialToString(blockAgainstType)
                            + ", block-face: " + blockFace.toString().toLowerCase());
        } else if (blockType != Material.FIRE && blockType.isBlock() && !p.wasFlying() && !blockType.equals(soil)) {
            GameMode gameMode = p.getGameMode();

            if (gameMode != GameMode.SURVIVAL && gameMode != GameMode.ADVENTURE) {
                return;
            }

            if (BlockUtils.isFullSolid(itemStackType) // Placed block must be fully solid
                    && BlockUtils.isSolid(blockAgainstType) // Block against must be solid
                    && blockFace != BlockFace.SELF) { // Block-face handled by the BlockReach check
                SpartanLocation to = p.getLocation();
                int blockY = to.getBlockY();
                double box = to.getY() - blockY;

                if (!p.wasInLiquids()  // Ensure the player is not inside liquid blocks
                        && (p.isOnGround()
                        || p.isOnGroundCustom()
                        || PlayerData.isOnGround(p, to, 0, -box, 0)
                        || PlayerData.isOnGround(p, to, 0, -1, 0)
                        || PlayerData.isOnGround(p, to, 0, -(1 + box), 0)
                        || p.getTicksOnAir() <= 11)) { // Ensure the player is on ground or close to it
                    SpartanLocation blockLocation = b.getLocation();
                    double verticalDistance = blockY - blockLocation.getBlockY();

                    if (verticalDistance >= 0 && verticalDistance <= 2) { // Ensure the block is below or at max 2 blocks away vertically
                        double horizontalDistance = AlgebraUtils.getHorizontalDistance(to, blockAgainstLocation);
                        boolean scaffold = blockFace != BlockFace.DOWN && blockFace != BlockFace.UP
                                && blockLocation.getSurroundingSolidBlockCount(1, 1, 2) <= 1;
                        boolean access;

                        if (scaffold) {
                            access = check.getCheck().getBooleanOption("check_scaffold", true)
                                    && blockY == (blockAgainst.getY() + 1) && horizontalDistance < 1.5;
                        } else if (check.getCheck().getBooleanOption("check_tower", true)
                                && !BouncingBlocks.isBelow(p)) {
                            UUID uuid = p.getUniqueId();
                            TowerPosition object = memory.get(uuid);

                            if (object == null) {
                                access = false;
                                memory.put(uuid, new TowerPosition(blockY));
                            } else {
                                access = object.getTimePassed() <= 1_000L && blockY > object.blockY;
                                object.setBlockY(blockY);
                            }
                        } else {
                            access = false;
                        }

                        if (access) {
                            Buffer buffer = p.getBuffer(check);
                            String scenario = scaffold ? "scaffold" : "tower";

                            if (buffer.start(scenario, 20) >= (!scaffold ? 4 : 5)) {
                                buffer.remove(scenario);
                                new HackPrevention(p, check, "type: " + scenario
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

    public static void runBreak(SpartanPlayer p, SpartanBlock b) {
        if (p.canRunCheck(check, true)) {
            SpartanLocation bloc = b.getLocation();

            if (check.getCheck().getBooleanOption("check_item_usage", true)
                    && !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_7) && !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)
                    && p.isUsingItem() && !BlockUtils.isSensitive(p, bloc)) {
                new HackPrevention(p, check, "type: item-usage, event: break, block: " + BlockUtils.blockToString(b));
            } else if (check.getCheck().getBooleanOption("check_liquids", true) && !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                Material m = bloc.getBlock().getType();

                if (BlockUtils.isBaseLiquid(m)) {
                    new HackPrevention(p, check, "type: liquids(break), block: " + BlockUtils.materialToString(m));
                }
            }
        }
    }
}
