package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPotionEffect;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.SuperPickaxe;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.MaterialUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class FastBreak extends CheckExecutor {

    private SpartanBlock lastBlock = null;
    private final Map<Integer, Long> interactTime;

    public FastBreak(SpartanPlayer player) {
        super(HackType.FastBreak, player);
        this.interactTime = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    private void checkDelay(SpartanBlock block, int seconds) {
        if (hackType.getCheck().getBooleanOption("check_delay", true)) {
            GameMode gameMode = player.getGameMode();

            if (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE) {
                long time = interactTime.getOrDefault(Objects.hash(block.world.getName(), block.material, block.x, block.y, block.z), 0L);

                if (time > 0L) {
                    time = System.currentTimeMillis() - time;
                }
                if (time > 0L) { // Make sure the plugin knows when the player started breaking the block
                    ItemStack itemStack = player.getItemInHand();
                    long originalLimit = MaterialUtils.getBlockBreakTime(player, itemStack, block.material),
                            limit = originalLimit;

                    if (limit > -1L) { // Make sure the limit can be calculated on this specific server
                        limit -= 250L; // Subtract ms for software/connection inconsistencies

                        if (player.bedrockPlayer) {
                            limit = AlgebraUtils.integerRound(limit * 0.8);
                        }

                        if (limit > 100L && time < limit) { // Do not check blocks that take equal or less than 1/10th of a second to break
                            boolean emptyItem = itemStack == null || itemStack.getType() == Material.AIR;
                            Collection<SpartanPotionEffect> potionEffects = player.getStoredPotionEffects();
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
                                for (SpartanPotionEffect effect : potionEffects) {
                                    if (effect.isActive()) {
                                        String name = effect.bukkitEffect.getType().getName().toLowerCase().replace("_", "/");
                                        effectsString.append(name).append("-").append(effect.bukkitEffect.getAmplifier() + 1).append(", ");
                                    }
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
                                    + ", block: " + BlockUtils.materialToString(block.material)
                                    + ", item: " + (emptyItem ? "none" : BlockUtils.materialToString(itemStack.getType()))
                                    + ", enchantments: " + (!hasEnchantments ? "none" : "[" + enchantmentsString + "]")
                                    + ", effects: " + (!hasEffects ? "none" : "[" + effectsString + "]");
                            cancel(prevention, 1.0, null, seconds);
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
                Buffer buffer = this.getBuffer();
                String key = "breaks-per-second";

                if (buffer.count(key, 20) == limit) {
                    buffer.remove(key);
                    cancel(
                            "type: breaks-per-second"
                                    + ", type: " + BlockUtils.blockToString(block)
                                    + ", limit: " + limit,
                            1.0,
                            null,
                            seconds
                    );
                }
            }
        }
    }

    private void checkIndirectSurroundingsPerSecond(int seconds, SpartanBlock block) {
        GameMode gameMode = player.getGameMode();

        if (gameMode != GameMode.CREATIVE) {
            int limit = hackType.getCheck().getNumericalOption("indirect_surroundings_per_second", 15);

            if (limit > 0) {
                String key = "indirect-surroundings";
                Buffer buffer = this.getBuffer();
                int bufferTime = 20;
                double bufferThreshold;

                if (lastBlock != null) {
                    if (!(block.x == lastBlock.x && block.y == lastBlock.y && block.z == lastBlock.z // Same
                            || block.x == (lastBlock.x + 1) && block.y == lastBlock.y && block.z == lastBlock.z // Left
                            || block.x == (lastBlock.x - 1) && block.y == lastBlock.y && block.z == lastBlock.z // Right
                            || block.x == lastBlock.x && block.y == lastBlock.y && block.z == (lastBlock.z + 1) // Front
                            || block.x == lastBlock.x && block.y == lastBlock.y && block.z == (lastBlock.z - 1) // Back
                            || block.x == (lastBlock.x + 1) && block.y == lastBlock.y && block.z == (lastBlock.z + 1) // Left-Diagonal
                            || block.x == (lastBlock.x - 1) && block.y == lastBlock.y && block.z == (lastBlock.z - 1) // Right-Diagonal
                            || block.x == (lastBlock.x + 1) && block.y == lastBlock.y && block.z == (lastBlock.z - 1) // Front-Diagonal
                            || block.x == (lastBlock.x - 1) && block.y == lastBlock.y && block.z == (lastBlock.z + 1) // Back-Diagonal
                            || block.x == lastBlock.x && block.y == (lastBlock.y + 1) && block.z == lastBlock.z // Up
                            || block.x == lastBlock.x && block.y == (lastBlock.y - 1) && block.z == lastBlock.z)) { // Down
                        bufferThreshold = buffer.count(key, bufferTime);
                    } else {
                        return;
                    }
                } else {
                    bufferThreshold = buffer.count(key, bufferTime);
                }

                if (bufferThreshold == limit) {
                    buffer.remove(key);
                    String prevention = "type: indirect-surroundings-per-second"
                            + ", type: " + BlockUtils.blockToString(block)
                            + ", limit: " + limit;
                    cancel(prevention, 1.0, null, seconds);
                }
                lastBlock = block;
            }
        }
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerInteractEvent) {
            PlayerInteractEvent event = (PlayerInteractEvent) object;

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                SpartanBlock b = new SpartanBlock(player, event.getClickedBlock());

                if (interactTime.size() == TPS.maximum) {
                    Iterator<Integer> iterator = interactTime.keySet().iterator();
                    iterator.next();
                    iterator.remove();
                }
                interactTime.put(Objects.hash(b.world.getName(), b.material, b.x, b.y, b.z), System.currentTimeMillis());
            }
        } else {
            SpartanBlock b = new SpartanBlock(player, ((BlockBreakEvent) object).getBlock());

            if (!TreeFeller.canCancel(this.player, b)) {
                int seconds = 20; // 1 second in ticks
                checkDelay(b, seconds);
                checkBreaksPerSecond(seconds, b);
                checkIndirectSurroundingsPerSecond(seconds, b);
            }
        }
    }

    @Override
    protected boolean canRun() {
        return !MineBomb.isUsing(player)
                && !Vehicles.has(player, "drill")
                && !SuperPickaxe.canCancel(player)
                && !AureliumSkills.isUsing(player)
                && !mcMMO.hasGeneralAbility(player)
                && !Compatibility.CompatibilityType.CRAFT_BOOK.isFunctional();
    }
}
