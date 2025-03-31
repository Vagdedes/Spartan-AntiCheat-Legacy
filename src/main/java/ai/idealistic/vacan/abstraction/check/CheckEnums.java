package ai.idealistic.vacan.abstraction.check;

import ai.idealistic.vacan.abstraction.check.implementation.misc.ImpossibleInventory;
import ai.idealistic.vacan.abstraction.check.implementation.misc.InventoryClicks;
import ai.idealistic.vacan.utils.minecraft.inventory.MaterialUtils;
import lombok.Getter;
import org.bukkit.Material;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CheckEnums {

    public enum HackType {
        HIT_REACH(
                HackCategoryType.COMBAT,
                ai.idealistic.vacan.abstraction.check.implementation.combat.HitReach.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to hit entities",
                        "from an abnormally long distance"
                }
        ),
        CRITICALS(
                HackCategoryType.COMBAT,
                ai.idealistic.vacan.abstraction.check.implementation.combat.Criticals.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to critical damage",
                        "an entity without properly moving."
                }
        ),
        FAST_BOW(
                HackCategoryType.COMBAT,
                ai.idealistic.vacan.abstraction.check.implementation.combat.FastBow.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to shoot arrows",
                        "in abnormally fast rates."
                }
        ),
        FAST_CLICKS(
                HackCategoryType.COMBAT,
                ai.idealistic.vacan.abstraction.check.implementation.combat.FastClicks.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to click abnormally fast",
                        "or have an irregular clicking consistency."
                }
        ),
        VELOCITY(
                HackCategoryType.COMBAT,
                ai.idealistic.vacan.abstraction.check.implementation.combat.Velocity.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to receive abnormal",
                        "amounts of knockback, or none at all."
                }
        ),
        KILL_AURA(
                HackCategoryType.COMBAT,
                ai.idealistic.vacan.abstraction.check.implementation.combat.killaura.KillAura.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to have an 'apparent'",
                        "combat advantage against any entity."
                }
        ),
        IRREGULAR_MOVEMENTS(
                HackCategoryType.MOVEMENT,
                ai.idealistic.vacan.abstraction.check.implementation.movement.irregularmovements.IrregularMovements.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to move abnormally,",
                        "such as stepping blocks or climbing walls."
                }
        ),
        MORE_PACKETS(
                HackCategoryType.MOVEMENT,
                ai.idealistic.vacan.abstraction.check.implementation.movement.MorePackets.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to send abnormally",
                        "high amounts of movement packets."
                }
        ),
        X_RAY(
                HackCategoryType.WORLD,
                ai.idealistic.vacan.abstraction.check.implementation.world.XRay.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to see through blocks",
                        "in order to find rare ores, such as diamonds,",
                        "gold, and even emerald. (Logs must be enabled)"
                }
        ),
        IMPOSSIBLE_ACTIONS(
                HackCategoryType.WORLD,
                ai.idealistic.vacan.abstraction.check.implementation.world.impossibleactions.ImpossibleActions.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to execute actions",
                        "in abnormal cases, such as when sleeping."
                }
        ),
        FAST_BREAK(
                HackCategoryType.WORLD,
                ai.idealistic.vacan.abstraction.check.implementation.world.FastBreak.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to break one or multiple",
                        "blocks irregularly fast."
                }
        ),
        FAST_PLACE(
                HackCategoryType.WORLD,
                ai.idealistic.vacan.abstraction.check.implementation.world.FastPlace.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to place blocks",
                        "in abnormally fast rates."
                }
        ),
        GHOST_HAND(
                HackCategoryType.WORLD,
                ai.idealistic.vacan.abstraction.check.implementation.world.GhostHand.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to interact or break",
                        "blocks through walls of blocks."
                }
        ),
        BLOCK_REACH(
                HackCategoryType.WORLD,
                ai.idealistic.vacan.abstraction.check.implementation.world.BlockReach.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to build or break",
                        "blocks within an abnormally long distance."
                }
        ),
        NO_SWING(
                HackCategoryType.MISCELLANEOUS,
                ai.idealistic.vacan.abstraction.check.implementation.misc.NoSwing.class,
                new String[]{
                        "This check will prevent client modules",
                        "that manipulate packets and prevent",
                        "interaction animations from being shown."
                }
        ),
        AUTO_RESPAWN(
                HackCategoryType.MISCELLANEOUS,
                ai.idealistic.vacan.abstraction.check.implementation.misc.AutoRespawn.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to respawn faster",
                        "than what is physically expected."
                }
        ),
        INVENTORY_CLICKS(
                HackCategoryType.MISCELLANEOUS,
                InventoryClicks.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to interact with an",
                        "amount of items, in abnormally fast rates."
                }
        ),
        FAST_HEAL(
                HackCategoryType.MISCELLANEOUS,
                ai.idealistic.vacan.abstraction.check.implementation.misc.FastHeal.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to heal faster",
                        "than what is physically allowed."
                }
        ),
        IMPOSSIBLE_INVENTORY(
                HackCategoryType.MISCELLANEOUS,
                ImpossibleInventory.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to interact with",
                        "an inventory in abnormal cases, such",
                        "as when sprinting or walking."
                }
        ),
        FAST_EAT(
                HackCategoryType.MISCELLANEOUS,
                ai.idealistic.vacan.abstraction.check.implementation.misc.FastEat.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to consume an amount",
                        "of food in an abnormal amount of time."
                }
        );

        @Getter
        private Check check;
        public final HackCategoryType category;
        public final Class<?> executor;
        public final String[] description;
        private final Map<String, Long> detections;

        HackType(HackCategoryType category, Class<?> executor, String[] description) {
            this.category = category;
            this.executor = executor;
            this.description = description;
            this.detections = new ConcurrentHashMap<>();
            this.check = new Check(this);
        }

        public void resetCheck() {
            if (this.check != null) {
                this.check = new Check(this);
            }
        }

        public void addDetection(String detection, long averageTime) {
            this.detections.put(detection, averageTime);
        }

        public void removeDetection(String detection) {
            this.detections.remove(detection);
        }

        public long getDefaultAverageTime(String detection) {
            return this.detections.get(detection);
        }

        public Collection<String> getDetections() {
            return this.detections.keySet();
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase().replace("_", "-");
        }
    }

    public enum HackCategoryType {
        COMBAT(Material.IRON_SWORD),
        MOVEMENT(Material.FEATHER),
        WORLD(Material.DIAMOND_PICKAXE),
        MISCELLANEOUS(MaterialUtils.get("crafting_table"));

        public final Material material;

        HackCategoryType(Material material) {
            this.material = material;
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase().replace("_", "-");
        }

    }

}
