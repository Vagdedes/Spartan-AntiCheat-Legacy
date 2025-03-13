package me.vagdedes.spartan.api.system;

import ai.idealistic.vacan.abstraction.check.CheckEnums;
import ai.idealistic.vacan.utils.minecraft.inventory.MaterialUtils;
import lombok.Getter;
import org.bukkit.Material;

public class Enums {

    // API Use
    // Should have used capital letters but won't change them now so to not break the dozen APIs who use these enums
    public enum HackType {
        XRay(CheckEnums.HackType.XRay),
        Exploits(CheckEnums.HackType.Exploits),
        NoSwing(CheckEnums.HackType.NoSwing),
        IrregularMovements(CheckEnums.HackType.IrregularMovements),
        ImpossibleActions(CheckEnums.HackType.ImpossibleActions),
        AutoRespawn(CheckEnums.HackType.AutoRespawn),
        InventoryClicks(CheckEnums.HackType.InventoryClicks),
        Criticals(CheckEnums.HackType.Criticals),
        GhostHand(CheckEnums.HackType.GhostHand),
        BlockReach(CheckEnums.HackType.BlockReach),
        FastBow(CheckEnums.HackType.FastBow),
        FastClicks(CheckEnums.HackType.FastClicks),
        FastHeal(CheckEnums.HackType.FastHeal),
        ImpossibleInventory(CheckEnums.HackType.ImpossibleInventory),
        HitReach(CheckEnums.HackType.HitReach),
        FastBreak(CheckEnums.HackType.FastBreak),
        FastPlace(CheckEnums.HackType.FastPlace),
        MorePackets(CheckEnums.HackType.MorePackets),
        FastEat(CheckEnums.HackType.FastEat),
        Velocity(CheckEnums.HackType.Velocity),
        KillAura(CheckEnums.HackType.KillAura),
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
