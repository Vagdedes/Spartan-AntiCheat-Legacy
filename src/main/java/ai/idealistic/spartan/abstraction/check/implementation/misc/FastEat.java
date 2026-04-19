package ai.idealistic.spartan.abstraction.check.implementation.misc;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckEnums.HackType;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.abstraction.world.ServerBlock;
import ai.idealistic.spartan.compatibility.manual.vanilla.Attributes;
import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.utils.minecraft.inventory.MaterialUtils;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;

public class FastEat extends CheckRunner {

    private final CheckDetection interact, eat;
    private static final Material cake = MaterialUtils.get("cake");
    private static final long
            delay = 1_000L,
            driedKelp = 550L;

    private long delayTime, eatTime, lastConsumeEvent = 0L;
    private double vlEat = 0, vlInteract = 0; // Added buffers to absorb network desyncs

    public FastEat(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.interact = new ImplementedDetection(this, null, null, "interact", true);
        this.eat = new ImplementedDetection(this, null, null, "eat", true);
    }

    private long getDelay(ItemStack itemStack) {
        if (itemStack != null) {
            Material type = itemStack.getType();

            // EXTENSION: Catch fast-drinking for Potions, Milk, and Honey
            if (type.name().contains("POTION") || type.name().contains("MILK_BUCKET") || type.name().contains("HONEY_BOTTLE")) {
                return delay;
            }
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && type == Material.DRIED_KELP) {
                return driedKelp;
            }
            if (type.isEdible()) {
                return delay;
            }
        }
        return 0L;
    }

    // EXTENSION: Scans both hands to prevent Offhand Bypasses
    private long getBestDelay() {
        long bestDelay = 0L;
        ItemStack mainHand = this.protocol.getItemInHand();
        bestDelay = Math.max(bestDelay, getDelay(mainHand));

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            PlayerInventory inventory = this.protocol.getInventory();
            ItemStack offHand = inventory.getItemInOffHand();
            bestDelay = Math.max(bestDelay, getDelay(offHand));
        }

        return bestDelay;
    }

    private void checkDelay(ItemStack consumed) {
        this.eat.call(() -> {
            long time = System.currentTimeMillis() - delayTime;
            this.delayTime = System.currentTimeMillis();
            long calculatedDelay = consumed != null ? getDelay(consumed) : getBestDelay();

            if (calculatedDelay > 0L && time <= calculatedDelay) {
                if (++vlEat > 1) { // Buffer integration
                    this.eat.cancel(
                            "eat, ms: " + time + " (consecutive)"
                    );
                }
            } else {
                vlEat = Math.max(0, vlEat - 0.25);
            }
        });
    }

    private void checkEating(ItemStack consumed) {
        this.interact.call(() -> {
            long time = System.currentTimeMillis() - eatTime;
            long calculatedDelay = consumed != null ? getDelay(consumed) : getBestDelay();

            if (calculatedDelay > 0L && time <= calculatedDelay) {
                if (++vlInteract > 1) { // Buffer integration
                    this.interact.cancel(
                            "interact, ms: " + time + ", delay: " + calculatedDelay
                    );
                }
            } else {
                vlInteract = Math.max(0, vlInteract - 0.5);
            }
        });
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        // EXTENSION: Hook PlayerItemConsumeEvent to fix the 20/20 Hunger Gapple Bypass
        if (object instanceof PlayerItemConsumeEvent) {
            PlayerItemConsumeEvent e = (PlayerItemConsumeEvent) object;
            lastConsumeEvent = System.currentTimeMillis();

            checkDelay(e.getItem());
            checkEating(e.getItem());

            if (this.prevent()) {
                e.setCancelled(true);
            }

        } else if (object instanceof FoodLevelChangeEvent) {
            FoodLevelChangeEvent e = (FoodLevelChangeEvent) object;
            int lvl = e.getFoodLevel();

            // Ignore if we just processed a ConsumeEvent to prevent double-flagging
            if (lvl > this.protocol.bukkit().getFoodLevel() && (System.currentTimeMillis() - lastConsumeEvent > 50L)) {
                checkDelay(null);
                checkEating(null);

                if (this.prevent()) {
                    e.setCancelled(true);
                }
            }

        } else if (object instanceof PlayerInteractEvent) {
            PlayerInteractEvent e = (PlayerInteractEvent) object;
            Action action = e.getAction();

            if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                Block block = e.getClickedBlock();

                if (block != null && action == Action.RIGHT_CLICK_BLOCK) {
                    PluginBase.runTask(block, () -> {
                        if (new ServerBlock(block).getType() == cake) {
                            this.protocol.getRunner(hackType).addDisableCause(null, null, 10);
                        }
                    });
                }

                // EXTENSION: Fixes the Floor-Staring interact bypass
                if (getBestDelay() > 0L) {
                    this.eatTime = System.currentTimeMillis();
                }
            }
        }
    }

    @Override
    protected boolean canRun() {
        return this.protocol.getWorld().getDifficulty() != Difficulty.PEACEFUL
                && !this.protocol.hasPotionEffect(PotionEffectType.SATURATION, 0)
                && Attributes.getAmount(this.protocol, Attributes.GENERIC_MAX_ABSORPTION) == Double.MIN_VALUE;
    }
}