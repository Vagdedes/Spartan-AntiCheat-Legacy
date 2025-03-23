package ai.idealistic.vacan.compatibility.manual.building;

import ai.idealistic.vacan.Register;
import ai.idealistic.vacan.abstraction.check.CheckEnums;
import ai.idealistic.vacan.compatibility.Compatibility;
import ai.idealistic.vacan.functionality.server.Config;
import ai.idealistic.vacan.functionality.server.PluginBase;
import ai.idealistic.vacan.functionality.server.TPS;
import ai.idealistic.vacan.utils.math.AlgebraUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import wtf.choco.veinminer.api.event.player.PlayerClientActivateVeinMinerEvent;
import wtf.choco.veinminer.api.event.player.PlayerVeinMineEvent;

public class VeinMiner implements Listener {

    public static void reload() {
        Register.enable(new VeinMiner());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(PlayerVeinMineEvent e) {
        if (Compatibility.CompatibilityType.VEIN_MINER.isFunctional()) {
            Config.compatibility.evadeFalsePositives(
                    PluginBase.getProtocol(e.getPlayer()),
                    Compatibility.CompatibilityType.VEIN_MINER,
                    new CheckEnums.HackType[]{
                            CheckEnums.HackType.NO_SWING,
                            CheckEnums.HackType.FAST_BREAK,
                            CheckEnums.HackType.GHOST_HAND,
                            CheckEnums.HackType.BLOCK_REACH
                    },
                    AlgebraUtils.integerRound(TPS.maximum)
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(PlayerClientActivateVeinMinerEvent e) {
        if (Compatibility.CompatibilityType.VEIN_MINER.isFunctional()) {
            Config.compatibility.evadeFalsePositives(
                    PluginBase.getProtocol(e.getPlayer()),
                    Compatibility.CompatibilityType.VEIN_MINER,
                    new CheckEnums.HackType[]{
                            CheckEnums.HackType.NO_SWING,
                            CheckEnums.HackType.FAST_BREAK,
                            CheckEnums.HackType.GHOST_HAND,
                            CheckEnums.HackType.BLOCK_REACH
                    },
                    AlgebraUtils.integerRound(TPS.maximum)
            );
        }
    }

}
