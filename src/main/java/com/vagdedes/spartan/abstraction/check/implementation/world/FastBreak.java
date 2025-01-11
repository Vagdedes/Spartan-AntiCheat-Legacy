package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import com.vagdedes.spartan.compatibility.manual.building.MineBomb;
import com.vagdedes.spartan.compatibility.manual.building.SuperPickaxe;
import com.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.inventory.MaterialUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FastBreak extends CheckRunner {

    private final CheckDetection
            delay,
            breaksPerSecond,
            indirectSurroundingsPerSecond;
    private Block lastBlock = null;
    private final Map<Integer, Long> interactTime;
    private final Buffer.IndividualBuffer
            breaksPerSecondBuffer,
            individualSurroundingsPerSecondBuffer;

    public FastBreak(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.delay = new ImplementedProbabilityDetection(this, null, null, "delay", true);
        this.breaksPerSecond = new ImplementedProbabilityDetection(this, null, null, null, true);
        this.indirectSurroundingsPerSecond = new ImplementedProbabilityDetection(this, null, null, null, true);
        this.interactTime = new ConcurrentHashMap<>(2);
        this.breaksPerSecondBuffer = new Buffer.IndividualBuffer();
        this.individualSurroundingsPerSecondBuffer = new Buffer.IndividualBuffer();
    }

    private void checkDelay(Block block, int seconds) {
        if (!BlockUtils.isSensitive(block.getType())) {
            GameMode gameMode = this.protocol.bukkit().getGameMode();

            if (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE) {
                long time = interactTime.getOrDefault(
                        Objects.hash(block.getWorld().getName(), block.getType(), block.getX(), block.getY(), block.getZ()),
                        0L
                );

                if (time > 0L) {
                    time = System.currentTimeMillis() - time;

                    if (time > 0L) { // Make sure the plugin knows when the player started breaking the block
                        ItemStack itemStack = this.protocol.spartan.getItemInHand();
                        long originalLimit = MaterialUtils.getBlockBreakTime(this.protocol, itemStack, block.getType()),
                                limit = originalLimit;

                        if (limit > -1L) { // Make sure the limit can be calculated on this specific server
                            limit /= 2;

                            if (limit > (TPS.tickTime * 2L) && time < limit) {
                                boolean emptyItem = itemStack == null || itemStack.getType() == Material.AIR;
                                Collection<PotionEffect> potionEffects = this.protocol.spartan.getActivePotionEffects();
                                Set<Map.Entry<Enchantment, Integer>> enchantments = !emptyItem
                                        ? itemStack.getEnchantments().entrySet()
                                        : new HashSet<>();
                                StringBuilder enchantmentsString = new StringBuilder(),
                                        effectsString = new StringBuilder();

                                if (!enchantments.isEmpty()) {
                                    for (Map.Entry<Enchantment, Integer> enchantment : enchantments) {
                                        String name = enchantment.getKey().getName().toLowerCase().replace("_", "/");
                                        enchantmentsString.append(name).append("-").append(enchantment.getValue()).append(", ");
                                    }
                                    enchantmentsString = new StringBuilder(enchantmentsString.substring(0, enchantmentsString.length() - 2));
                                }
                                if (!potionEffects.isEmpty()) {
                                    for (PotionEffect effect : potionEffects) {
                                        String name = effect.getType().getName().toLowerCase().replace("_", "/");
                                        effectsString.append(name).append("-").append(effect.getAmplifier() + 1).append(", ");
                                    }
                                    effectsString = new StringBuilder(effectsString.substring(0, effectsString.length() - 2));
                                }
                                String prevention = "type: delay"
                                        + ", time: " + time
                                        + ", original-limit: " + originalLimit
                                        + ", limit: " + limit
                                        + ", difference: " + (limit - time)
                                        + ", block: " + BlockUtils.materialToString(block.getType())
                                        + ", item: " + (emptyItem ? "none" : BlockUtils.materialToString(itemStack.getType()))
                                        + ", enchantments: " + (!enchantments.isEmpty() ? "none" : "[" + enchantmentsString + "]")
                                        + ", effects: " + (!potionEffects.isEmpty() ? "none" : "[" + effectsString + "]");
                                this.delay.cancel(
                                        prevention,
                                        null,
                                        seconds
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkBreaksPerSecond(int seconds, Block block) {
        GameMode gameMode = this.protocol.bukkit().getGameMode();

        if (gameMode == GameMode.CREATIVE) {
            int limit = hackType.getCheck().getNumericalOption("max_breaks_per_second", 40);

            if (limit > 0) {
                int buffer = breaksPerSecondBuffer.count(1, 20);

                if (buffer >= limit) {
                    this.breaksPerSecond.cancel(
                            "type: breaks-per-second"
                                    + ", type: " + BlockUtils.blockToString(block)
                                    + ", limit: " + limit,
                            null,
                            seconds
                    );
                }
            }
        }
    }

    private void checkIndirectSurroundingsPerSecond(int seconds, Block block) {
        GameMode gameMode = this.protocol.bukkit().getGameMode();

        if ((gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE)
                && !BlockUtils.isSensitive(this.protocol, block.getType())) {
            int bufferTicks = AlgebraUtils.integerCeil(TPS.maximum),
                    limit = hackType.getCheck().getNumericalOption(
                            "indirect_surroundings_per_second",
                            AlgebraUtils.integerRound(bufferTicks * 0.75)
                    );

            if (limit > 0) {
                double bufferThreshold;

                if (lastBlock != null) {
                    if (!(block.getX() == lastBlock.getX() && block.getY() == lastBlock.getY() && block.getZ() == lastBlock.getZ() // Same
                            || block.getX() == (lastBlock.getX() + 1) && block.getY() == lastBlock.getY() && block.getZ() == lastBlock.getZ() // Left
                            || block.getX() == (lastBlock.getX() - 1) && block.getY() == lastBlock.getY() && block.getZ() == lastBlock.getZ() // Right
                            || block.getX() == lastBlock.getX() && block.getY() == lastBlock.getY() && block.getZ() == (lastBlock.getZ() + 1) // Front
                            || block.getX() == lastBlock.getX() && block.getY() == lastBlock.getY() && block.getZ() == (lastBlock.getZ() - 1) // Back
                            || block.getX() == (lastBlock.getX() + 1) && block.getY() == lastBlock.getY() && block.getZ() == (lastBlock.getZ() + 1) // Left-Diagonal
                            || block.getX() == (lastBlock.getX() - 1) && block.getY() == lastBlock.getY() && block.getZ() == (lastBlock.getZ() - 1) // Right-Diagonal
                            || block.getX() == (lastBlock.getX() + 1) && block.getY() == lastBlock.getY() && block.getZ() == (lastBlock.getZ() - 1) // Front-Diagonal
                            || block.getX() == (lastBlock.getX() - 1) && block.getY() == lastBlock.getY() && block.getZ() == (lastBlock.getZ() + 1) // Back-Diagonal
                            || block.getX() == lastBlock.getX() && block.getY() == (lastBlock.getY() + 1) && block.getZ() == lastBlock.getZ() // Up
                            || block.getX() == lastBlock.getX() && block.getY() == (lastBlock.getY() - 1) && block.getZ() == lastBlock.getZ())) { // Down
                        bufferThreshold = individualSurroundingsPerSecondBuffer.count(1, bufferTicks);
                    } else {
                        return;
                    }
                } else {
                    bufferThreshold = individualSurroundingsPerSecondBuffer.count(1, bufferTicks);
                }

                if (bufferThreshold >= limit) {
                    String prevention = "type: indirect-surroundings-per-second"
                            + ", type: " + BlockUtils.blockToString(block)
                            + ", limit: " + limit;
                    this.indirectSurroundingsPerSecond.cancel(
                            prevention,
                            null,
                            seconds
                    );
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
                Block b = event.getClickedBlock();

                if (interactTime.size() == TPS.maximum) {
                    Iterator<Integer> iterator = interactTime.keySet().iterator();
                    iterator.next();
                    iterator.remove();
                }
                interactTime.put(Objects.hash(b.getWorld().getName(), b.getType(), b.getX(), b.getY(), b.getZ()), System.currentTimeMillis());
            }
        } else {
            Block b = ((BlockBreakEvent) object).getBlock();

            if (!TreeFeller.canCancel(b)) {
                int seconds = AlgebraUtils.integerCeil(TPS.maximum);
                checkDelay(b, seconds);
                checkBreaksPerSecond(seconds, b);
                checkIndirectSurroundingsPerSecond(seconds, b);
            }
        }
    }

    @Override
    protected boolean canRun() {
        return !MineBomb.isUsing(this.protocol.spartan)
                && !Vehicles.has(this.protocol, Vehicles.DRILL)
                && !SuperPickaxe.isUsing(this.protocol.spartan)
                && !AureliumSkills.isUsing(this.protocol)
                && !Compatibility.CompatibilityType.CRAFT_BOOK.isFunctional()
                && Attributes.getAmount(this.protocol, Attributes.PLAYER_BLOCK_BREAK_SPEED) == 0.0;
    }

}
