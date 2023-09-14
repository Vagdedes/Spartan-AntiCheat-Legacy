package me.vagdedes.spartan.checks.implementation.world;

import me.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import me.vagdedes.spartan.compatibility.manual.abilities.mcmmo.mcMMO;
import me.vagdedes.spartan.compatibility.manual.building.MineBomb;
import me.vagdedes.spartan.compatibility.manual.building.SuperPickaxe;
import me.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import me.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class FastBreak {

    private static final HackType check = HackType.FastBreak;
    private static final Map<UUID, SpartanBlock> lastBlock = new LinkedHashMap<>(Config.getMaxPlayers());

    // Base

    public static void run(SpartanPlayer p, SpartanBlock b) {
        if (p.canRunCheck(check, true)) {
            GameMode gameMode = p.getGameMode();

            if (canDo(p, b, gameMode)) {
                int seconds = 20; // 1 second in ticks
                checkDelay(p, b, seconds, gameMode);
                checkBreaksPerSecond(p, seconds, b, gameMode);
                checkIndirectSurroundingsPerSecond(p, seconds, b, gameMode);
            }
        }
    }

    private static boolean canDo(SpartanPlayer p, SpartanBlock b, GameMode gameMode) {
        return (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE || gameMode == GameMode.CREATIVE)
                && !MineBomb.canUse(p) && !Vehicles.has(p, "drill") && (b == null || !TreeFeller.canCancel(p, b))
                && !SuperPickaxe.canCancel(p) && !AureliumSkills.canCancel(p) && !mcMMO.hasAbility(p)
                && !Compatibility.CompatibilityType.CraftBook.isFunctional();
    }

    // Runnables

    public static void runInteract(SpartanPlayer p, Action action, SpartanBlock b) {
        if (action == Action.LEFT_CLICK_BLOCK && b != null
                && p.canRunCheck(check, false)) {
            p.getTimer(check).set("delay=" + Objects.hash(b.getWorld().getName(), b.getType(), b.getX(), b.getY(), b.getZ()));
        }
    }

    // Detections

    private static void checkDelay(SpartanPlayer p, SpartanBlock b, int seconds, GameMode gameMode) {
        if (check.getCheck().getBooleanOption("check_delay", true)
                && (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE)) {
            long time = p.getTimer(check).get("delay=" + Objects.hash(b.getWorld().getName(), b.getType(), b.getX(), b.getY(), b.getZ()), 0L);

            if (time > 0L) { // Make sure the plugin knows when the player started breaking the block
                Material blockType = b.getType();
                ItemStack itemStack = p.getItemInHand();
                long originalLimit = MaterialUtils.getBlockBreakTime(p, itemStack, blockType),
                        limit = originalLimit;

                if (limit > -1L) { // Make sure the limit can be calculated on this specific server
                    limit -= 250L; // Subtract ms for software/connection inconsistencies

                    if (p.isBedrockPlayer()) {
                        limit *= AlgebraUtils.twoThirds;
                    }

                    if (limit > 100L && time < limit) { // Do not check blocks that take equal or less than 1/10th of a second to break
                        boolean emptyItem = itemStack == null || itemStack.getType() == Material.AIR;
                        Collection<PotionEffect> potionEffects = p.getActivePotionEffects();
                        Set<Map.Entry<Enchantment, Integer>> enchantments =
                                !emptyItem && itemStack.hasItemMeta() && itemStack.getItemMeta().hasEnchants() ?
                                        itemStack.getEnchantments().entrySet() : new HashSet<>();
                        StringBuilder enchantmentsString = new StringBuilder(),
                                effectsString = new StringBuilder();
                        boolean hasEnchantments = enchantments.size() > 0,
                                hasEffects = potionEffects.size() > 0;

                        if (hasEnchantments) {
                            for (Map.Entry<Enchantment, Integer> enchantment : enchantments) {
                                String name = enchantment.getKey().getName().toLowerCase().replace("_", "/");
                                enchantmentsString.append(name).append("-").append(enchantment.getValue()).append(", ");
                            }
                            enchantmentsString = new StringBuilder(enchantmentsString.substring(0, enchantmentsString.length() - 2));
                        }
                        if (hasEffects) {
                            for (PotionEffect effect : potionEffects) {
                                String name = effect.getType().getName().toLowerCase().replace("_", "/");
                                effectsString.append(name).append("-").append(effect.getAmplifier() + 1).append(", ");
                            }
                        }
                        int length = effectsString.length();

                        if (hasEffects = (length > 0)) {
                            effectsString = new StringBuilder(effectsString.substring(0, length - 2));
                        }
                        String prevention = "type: delay"
                                + ", time: " + time
                                + ", original-limit: " + originalLimit
                                + ", limit: " + limit
                                + ", difference: " + (limit - time)
                                + ", block: " + BlockUtils.materialToString(blockType)
                                + ", item: " + (emptyItem ? "none" : BlockUtils.materialToString(itemStack.getType()))
                                + ", enchantments: " + (!hasEnchantments ? "none" : "[" + enchantmentsString + "]")
                                + ", effects: " + (!hasEffects ? "none" : "[" + effectsString + "]");

                        if (BlockUtils.isSensitive(blockType, originalLimit)) {
                            new HackPrevention(p, check, prevention, null, 0, false, 0.0, 0);
                        } else {
                            new HackPrevention(p, check, prevention, null, seconds);
                        }
                    }
                }
            }
        }
    }

    private static void checkBreaksPerSecond(SpartanPlayer p, int seconds, SpartanBlock block, GameMode gameMode) {
        if (gameMode == GameMode.CREATIVE) {
            int limit = check.getCheck().getNumericalOption("max_breaks_per_second", 40);

            if (limit > 0) {
                Buffer buffer = p.getBuffer(check);
                String key = "breaks-per-second";

                if (buffer.start(key, 20) >= limit) {
                    buffer.remove(key);
                    new HackPrevention(p, check,
                            "type: breaks-per-second"
                                    + ", type: " + BlockUtils.blockToString(block)
                                    + ", limit: " + limit, null,
                            seconds);
                }
            }
        }
    }

    private static void checkIndirectSurroundingsPerSecond(SpartanPlayer player, int seconds, SpartanBlock block, GameMode gameMode) {
        if (gameMode != GameMode.CREATIVE) {
            int limit = check.getCheck().getNumericalOption("indirect_surroundings_per_second", 15);

            if (limit > 0) {
                UUID uuid = player.getUniqueId();
                String key = "indirect-surroundings";
                Buffer buffer = player.getBuffer(check);
                int bufferTime = 20, bufferThreshold;
                SpartanBlock previousBlock = lastBlock.get(uuid);

                if (previousBlock != null) {
                    int x = block.getX(), y = block.getY(), z = block.getZ(),
                            previousX = previousBlock.getX(), previousY = previousBlock.getY(), previousZ = previousBlock.getZ();

                    if (!(x == previousX && y == previousY && z == previousZ // Same
                            || x == (previousX + 1) && y == previousY && z == previousZ // Left
                            || x == (previousX - 1) && y == previousY && z == previousZ // Right
                            || x == previousX && y == previousY && z == (previousZ + 1) // Front
                            || x == previousX && y == previousY && z == (previousZ - 1) // Back
                            || x == (previousX + 1) && y == previousY && z == (previousZ + 1) // Left-Diagonal
                            || x == (previousX - 1) && y == previousY && z == (previousZ - 1) // Right-Diagonal
                            || x == (previousX + 1) && y == previousY && z == (previousZ - 1) // Front-Diagonal
                            || x == (previousX - 1) && y == previousY && z == (previousZ + 1) // Back-Diagonal
                            || x == previousX && y == (previousY + 1) && z == previousZ // Up
                            || x == previousX && y == (previousY - 1) && z == previousZ)) { // Down
                        bufferThreshold = buffer.start(key, bufferTime);
                    } else {
                        bufferThreshold = buffer.get(key);
                    }
                } else {
                    bufferThreshold = buffer.start(key, bufferTime);
                }

                if (bufferThreshold >= limit) {
                    buffer.remove(key);
                    Material blockType = block.getType();
                    String prevention = "type: indirect-surroundings-per-second"
                            + ", type: " + BlockUtils.blockToString(block)
                            + ", limit: " + limit;

                    if (BlockUtils.isSensitive(player, blockType)) {
                        new HackPrevention(player, check, prevention, null, 0, false, 0.0, 0);
                    } else {
                        new HackPrevention(player, check, prevention, null, seconds);
                    }
                }
                lastBlock.put(uuid, block);
            }
        }
    }
}
