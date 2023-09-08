package me.vagdedes.spartan.system;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.objects.system.CancelCause;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.objects.system.LiveViolation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Enums {

    public enum HackType {
        XRay, Exploits, EntityMove, NoSwing, IrregularMovements, ImpossibleActions, ItemDrops, AutoRespawn, InventoryClicks,
        NoSlowdown, Criticals, GhostHand, BlockReach, FastBow, FastClicks, FastHeal, ImpossibleInventory, HitReach, FastBreak,
        Speed, FastPlace, MorePackets, NoFall, FastEat, Velocity, KillAura;

        private Check check;

        HackType() {
            this.check = null;
        }

        public Check getCheck() {
            return check != null ? check : (check = new Check(this));
        }

        public void resetCheck(boolean clearViolations) {
            if (this.check != null) {
                Map<UUID, CancelCause> disabledUsers = this.check.copyDisabledUsers();
                Map<UUID, CancelCause> silentUsers = this.check.copySilentUsers();
                Map<UUID, LiveViolation> violations = clearViolations ? new ConcurrentHashMap<>(Config.getMaxPlayers()) : this.check.copyViolations();
                Map<Integer, String> problematicDetections = this.check.copyProblematicDetections();
                Map<Integer, Integer> maxCancelledViolations = this.check.copyMaxCancelledViolations();
                this.check.clearCache();
                this.check = new Check(this, disabledUsers, silentUsers, violations, problematicDetections, maxCancelledViolations);
            }
        }
    }

    public enum MiningOre {
        AncientDebris, Diamond, Emerald, Gold;

        private final String string;

        MiningOre() {
            string = this.ordinal() == 0 ? "ancient-debris" : this.toString().toLowerCase();
        }

        public String getString() {
            return string;
        }
    }

    public enum PunishmentCategory {
        minimum, unsafe, normal, safe, maximum;

        private final String string;
        private final double multiplier;

        PunishmentCategory() {
            switch (this.ordinal()) {
                case 0:
                    string = "unlikely";
                    multiplier = 1.0;
                    break;
                case 1:
                    string = "potential";
                    multiplier = 2.0;
                    break;
                case 2:
                    string = "certain";
                    multiplier = 3.5;
                    break;
                case 3:
                    string = "definite";
                    multiplier = 5.5;
                    break;
                default:
                    string = "absolute";
                    multiplier = 8.0;
                    break;
            }
        }

        public String getString() {
            return string;
        }

        public double getMultiplier() {
            return multiplier;
        }
    }

    public enum Permission {
        condition, report, staff_chat, wave, reconnect, admin, reload, kick, bypass, manage, info,
        chat_protection, warn, use_bypass, ban, unban, notifications;

        private final String key;

        Permission() {
            key = Register.plugin.getName().toLowerCase() + "." + this.name().toLowerCase();
        }

        public String getKey() {
            return key;
        }
    }

    public enum ToggleAction {
        Enable, Disable
    }

    public enum CheckType {
        Combat, Movement, Exploits, Player, World, Inventory
    }

    // Separator

    public static HackType[] hackTypes() {
        return HackType.values();
    }

    public static final int
            hackTypeLength = hackTypes().length,
            miningOreLength = MiningOre.values().length;
}
