package com.vagdedes.spartan.checks.world;

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.abilities.mcmmo.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.SuperPickaxe;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.objects.replicates.SpartanBlock;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class FastBreak extends CheckExecutor {

    private static final HackType check = HackType.FastBreak;
    private SpartanBlock lastBlock = null;

    public FastBreak(SpartanPlayer player) {
        super(check, player);
    }

    private void checkDelay(SpartanBlock block, int seconds) {
        if (hackType.getCheck().getBooleanOption("check_delay", true)) {
            GameMode gameMode = player.getGameMode();

            if (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE) {
                long time = player.getTimer(hackType).get("delay=" + Objects.hash(block.getWorld().getName(), block.getType(), block.getX(), block.getY(), block.getZ()), 0L);

                if (time > 0L) { // Make sure the plugin knows when the player started breaking the block
                    Material blockType = block.getType();
                    ItemStack itemStack = player.getItemInHand();
                    long originalLimit = MaterialUtils.getBlockBreakTime(player, itemStack, blockType),
                            limit = originalLimit;

                    if (limit > -1L) { // Make sure the limit can be calculated on this specific server
                        limit -= 250L; // Subtract ms for software/connection inconsistencies

                        if (player.isBedrockPlayer()) {
                            limit *= AlgebraUtils.twoThirds;
                        }

                        if (limit > 100L && time < limit) { // Do not check blocks that take equal or less than 1/10th of a second to break
                            boolean emptyItem = itemStack == null || itemStack.getType() == Material.AIR;
                            Collection<PotionEffect> potionEffects = player.getActivePotionEffects();
                            Set<Map.Entry<Enchantment, Integer>> enchantments =
                                    !emptyItem && itemStack.hasItemMeta() && itemStack.getItemMeta().hasEnchants() ?
                                            itemStack.getEnchantments().entrySet() : new HashSet<>();
                            StringBuilder enchantmentsString = new StringBuilder(),
                                    effectsString = new StringBuilder();
                            boolean hasEnchantments = !enchantments.isEmpty(),
                                    hasEffects = !potionEffects.isEmpty();

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
                                    + ", seconds: " + (time / 1000.0)
                                    + ", original-limit: " + originalLimit
                                    + ", limit: " + limit
                                    + ", difference: " + (limit - time)
                                    + ", block: " + BlockUtils.materialToString(blockType)
                                    + ", item: " + (emptyItem ? "none" : BlockUtils.materialToString(itemStack.getType()))
                                    + ", enchantments: " + (!hasEnchantments ? "none" : "[" + enchantmentsString + "]")
                                    + ", effects: " + (!hasEffects ? "none" : "[" + effectsString + "]");

                            if (BlockUtils.isSensitive(blockType, originalLimit)) {
                                cancel(prevention, null, 0, false, 0.0, 0);
                            } else {
                                cancel(prevention, null, seconds);
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkBreaksPerSecond(int seconds, SpartanBlock block) {
        GameMode gameMode = player.getGameMode();

        if (gameMode == GameMode.CREATIVE) {
            int limit = hackType.getCheck().getNumericalOption("max_breaks_per_second", 40);

            if (limit > 0) {
                Buffer buffer = player.getBuffer(hackType);
                String key = "breaks-per-second";

                if (buffer.start(key, 20) >= limit) {
                    buffer.remove(key);
                    cancel(
                            "type: breaks-per-second"
                                    + ", type: " + BlockUtils.blockToString(block)
                                    + ", limit: " + limit, null,
                            seconds);
                }
            }
        }
    }

    private void checkIndirectSurroundingsPerSecond(int seconds, SpartanBlock block) {
        GameMode gameMode = player.getGameMode();

        if (gameMode != GameMode.CREATIVE) {
            int limit = hackType.getCheck().getNumericalOption("indirect_surroundings_per_second", 15);

            if (limit > 0) {
                UUID uuid = player.getUniqueId();
                String key = "indirect-surroundings";
                Buffer buffer = player.getBuffer(hackType);
                int bufferTime = 20, bufferThreshold;

                if (lastBlock != null) {
                    int x = block.getX(), y = block.getY(), z = block.getZ(),
                            previousX = lastBlock.getX(), previousY = lastBlock.getY(), previousZ = lastBlock.getZ();

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
                        cancel(prevention, null, 0, false, 0.0, 0);
                    } else {
                        cancel(prevention, null, seconds);
                    }
                }
                lastBlock = block;
            }
        }
    }

    @Override
    public boolean handle(Object object) {
        if (object instanceof PlayerInteractEvent) {
            PlayerInteractEvent event = (PlayerInteractEvent) object;

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                SpartanBlock b = new SpartanBlock(player, event.getClickedBlock());

                if (player.canRunCheck(hackType, false)) {
                    player.getTimer(hackType).set("delay=" + Objects.hash(b.getWorld().getName(), b.getType(), b.getX(), b.getY(), b.getZ()));
                }
            }
        } else if (canDo()) {
            SpartanBlock b = new SpartanBlock(player, ((BlockBreakEvent) object).getBlock());

            if (!TreeFeller.canCancel(this.player, b)) {
                int seconds = 20; // 1 second in ticks
                checkDelay(b, seconds);
                checkBreaksPerSecond(seconds, b);
                checkIndirectSurroundingsPerSecond(seconds, b);
            }
        }
        return false;
    }

    @Override
    public boolean run() {
        return false;
    }

    @Override
    protected boolean canDo() {
        if (player.canRunCheck(hackType, true)) {
            GameMode gameMode = player.getGameMode();
            return (gameMode == GameMode.SURVIVAL
                    || gameMode == GameMode.ADVENTURE
                    || gameMode == GameMode.CREATIVE)
                    && !MineBomb.canUse(player)
                    && !Vehicles.has(player, "drill")
                    && !SuperPickaxe.canCancel(player)
                    && !AureliumSkills.canCancel(player)
                    && !mcMMO.hasAbility(player)
                    && !Compatibility.CompatibilityType.CraftBook.isFunctional();
        } else {
            return false;
        }
    }
}
