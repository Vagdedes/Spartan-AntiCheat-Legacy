package me.vagdedes.spartan.compatibility.manual.building.veinminer;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.handlers.identifiers.CheckProtection;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.server.ReflectionUtils;
import org.bukkit.entity.Player;

public class VeinMiner {

    public static void reload() {
        if (ReflectionUtils.classExists("wtf.choco.veinminer.api.event.PlayerVeinMineEvent")) {
            Register.enable(new VeinMinerNew(), VeinMinerNew.class);
        } else if (ReflectionUtils.classExists("me.choco.veinminer.api.PlayerVeinMineEvent")) {
            Register.enable(new VeinMinerOld(), VeinMinerOld.class);
        }
    }

    static void cancel(Player player) {
        CheckProtection.evadeCommonFalsePositives(player, Compatibility.CompatibilityType.VeinMiner,
                new Enums.HackType[]{
                        Enums.HackType.NoSwing,
                        Enums.HackType.FastBreak,
                        Enums.HackType.GhostHand,
                        Enums.HackType.BlockReach
                }, 30);
    }
}
