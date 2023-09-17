package me.vagdedes.spartan.checks.combat;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.replicates.SpartanInventory;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class FastBow extends CheckExecutor {

    public FastBow(SpartanPlayer player) {
        super(HackType.FastBow, player);
    }

    @Override
    public boolean handle(Object object) {
        if (object instanceof EntityShootBowEvent) {
            if (canDo()
                    && player.getItemInHand().getType() == Material.BOW) {
                checkBowShots();
                checkBowForce(((EntityShootBowEvent) object).getForce());
            }
        } else if (canDo()
                && ((PlayerInteractEvent)object).getAction() == Action.RIGHT_CLICK_AIR) {
            SpartanInventory inventory = player.getInventory();
            ItemStack item = inventory.getItemInHand();

            if (item.getType() == Material.BOW && inventory.contains(Material.ARROW)) {
                player.getTimer(hackType).set("interact");
            }
        }
        return false;
    }

    private void checkBowForce(float force) {
        long time = player.getTimer(hackType).get("interact");

        if (!hackType.getCheck().getBooleanOption("check_bow_force", true) || force != 1.0f || time > 500L) {
            return;
        }
        Buffer buffer = player.getBuffer(hackType);
        String key = "bow-force";

        if (buffer.start(key, 20) >= 2) {
            buffer.remove(key);
            cancel("type: bow-force, ms: " + time);
        }
    }

    private void checkBowShots() {
        if (hackType.getCheck().getBooleanOption("check_bow_shots", true)) {
            String cancel = null;
            Buffer buffer = player.getBuffer(hackType);
            String key = "bow-shots=";
            int delay = AlgebraUtils.integerRound(LagLeniencies.getDelay(player));
            int[] details = new int[]{10, 6};

            if (buffer.start(key + "fast", details[0]) == Math.min(details[1] + delay, details[0])) { // Combat
                buffer.remove(key + "fast");
                cancel = "fast";
            } else {
                details = new int[]{20, 7};

                if (buffer.start(key + "medium", details[0]) == Math.min(details[1] + delay, details[0])) { // Combat
                    buffer.remove(key + "medium");
                    cancel = "medium";
                } else {
                    details = new int[]{30, 9};

                    if (buffer.start(key + "slow", details[0]) == Math.min(details[1] + delay, details[0])) { // Combat
                        buffer.remove(key + "slow");
                        cancel = "slow";
                    }
                }
            }

            if (cancel != null) {
                cancel("type: bow-shots(" + cancel + ")");
            }
        }
    }

    @Override
    public boolean run() {
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, false);
    }
}
