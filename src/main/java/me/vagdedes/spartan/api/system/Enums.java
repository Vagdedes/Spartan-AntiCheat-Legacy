package me.vagdedes.spartan.api.system;

import ai.idealistic.vacan.abstraction.check.CheckEnums;
import lombok.Getter;

public class Enums {

    // API Use
    // Should have used capital letters but won't change them now so to not break the dozen APIs who use these enums
    public enum HackType {
        XRay(CheckEnums.HackType.X_RAY),
        Exploits(CheckEnums.HackType.EXPLOITS),
        NoSwing(CheckEnums.HackType.NO_SWING),
        IrregularMovements(CheckEnums.HackType.IRREGULAR_MOVEMENTS),
        ImpossibleActions(CheckEnums.HackType.IMPOSSIBLE_ACTIONS),
        AutoRespawn(CheckEnums.HackType.AUTO_RESPAWN),
        InventoryClicks(CheckEnums.HackType.INVENTORY_CLICKS),
        Criticals(CheckEnums.HackType.CRITICALS),
        GhostHand(CheckEnums.HackType.GHOST_HAND),
        BlockReach(CheckEnums.HackType.BLOCK_REACH),
        FastBow(CheckEnums.HackType.FAST_BOW),
        FastClicks(CheckEnums.HackType.FAST_CLICKS),
        FastHeal(CheckEnums.HackType.FAST_HEAL),
        ImpossibleInventory(CheckEnums.HackType.IMPOSSIBLE_INVENTORY),
        HitReach(CheckEnums.HackType.HIT_REACH),
        FastBreak(CheckEnums.HackType.FAST_BREAK),
        FastPlace(CheckEnums.HackType.FAST_PLACE),
        MorePackets(CheckEnums.HackType.MORE_PACKETS),
        FastEat(CheckEnums.HackType.FAST_EAT),
        Velocity(CheckEnums.HackType.VELOCITY),
        KillAura(CheckEnums.HackType.KILL_AURA),
        ;

        @Getter
        private final CheckEnums.HackType hackType;

        HackType(CheckEnums.HackType hackType) {
            this.hackType = hackType;
        }
    }

    // API Use
    public enum Permission {
        CONDITION(CheckEnums.Permission.CONDITION),
        WAVE(CheckEnums.Permission.WAVE),
        RECONNECT(CheckEnums.Permission.RECONNECT),
        ADMIN(CheckEnums.Permission.ADMIN),
        RELOAD(CheckEnums.Permission.RELOAD),
        KICK(CheckEnums.Permission.KICK),
        BYPASS(CheckEnums.Permission.BYPASS),
        MANAGE(CheckEnums.Permission.MANAGE),
        INFO(CheckEnums.Permission.INFO),
        CHAT_PROTECTION(CheckEnums.Permission.CHAT_PROTECTION),
        WARN(CheckEnums.Permission.WARN),
        USE_BYPASS(CheckEnums.Permission.USE_BYPASS),
        NOTIFICATIONS(CheckEnums.Permission.NOTIFICATIONS),
        BEDROCK(CheckEnums.Permission.BEDROCK);

        @Getter
        private final CheckEnums.Permission permission;

        Permission(CheckEnums.Permission permission) {
            this.permission = permission;
        }
    }

    // API Use
    public enum ToggleAction {
        ENABLE(ai.idealistic.vacan.api.ToggleAction.ENABLE),
        DISABLE(ai.idealistic.vacan.api.ToggleAction.DISABLE);

        @Getter
        private final ai.idealistic.vacan.api.ToggleAction toggleAction;

        ToggleAction(ai.idealistic.vacan.api.ToggleAction toggleAction) {
            this.toggleAction = toggleAction;
        }
    }

}
