package me.vagdedes.spartan.checks.world;

import me.vagdedes.spartan.compatibility.manual.abilities.mcmmo.mcMMO;
import me.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import me.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import org.bukkit.GameMode;
import org.bukkit.event.block.Action;

public class GhostHand {

    private static final HackType check = HackType.GhostHand;

    public static void runInteract(SpartanPlayer p, SpartanBlock b, Action action) {
        if (b == null
                || action != Action.RIGHT_CLICK_BLOCK
                || !canDo(p, b)
                || !check.getCheck().getBooleanOption("check_player_interactions", true)
                || p.getHandlers().has(Handlers.HandlerType.BlockBreak)
                || !BlockUtils.isChangeable(b.getLocation()) && !BlockUtils.areEggs(p.getItemInHand().getType())) {
            return;
        }
        SpartanBlock itb = p.getIllegalTargetBlock(b);

        if (itb != null
                && (!check.getCheck().getBooleanOption("check_fully_solid_blocks_only", true)
                || BlockUtils.isFullSolid(itb.getType()))) {
            new HackPrevention(p, check,
                    "type: interact, block: " + BlockUtils.blockToString(b) + ", real-block: " + BlockUtils.blockToString(itb),
                    null, 0, false, 0.0,
                    p.getCooldowns(check).canDo("break-protection"));
        }
    }

    public static void runBreak(SpartanPlayer p, SpartanBlock b, boolean cancelled) {
        if (cancelled) {
            p.getCooldowns(check).add("break-protection", 2);
        } else if (canDo(p, b)
                && check.getCheck().getBooleanOption("check_block_breaking", true)
                && !BlockUtils.isSensitive(p, b.getType())) {
            SpartanBlock itb = p.getIllegalTargetBlock(b);

            if (itb != null
                    && (!check.getCheck().getBooleanOption("check_fully_solid_blocks_only", true)
                    || BlockUtils.isFullSolid(itb.getType()))) {
                new HackPrevention(p, check,
                        "type: break, block: " + BlockUtils.blockToString(b) + ", real-block: " + BlockUtils.blockToString(itb),
                        10);
            }
        }
    }

    private static boolean canDo(SpartanPlayer p, SpartanBlock b) {
        if (p.canRunCheck(check, true)
                && !p.wasFlying()
                && b.getWorld() == p.getWorld()
                && !BlockUtils.areWires(b.getType())
                && !Vehicles.has(p, "drill")
                && !PrinterMode.canCancel(p)
                && !mcMMO.hasAbility(p)) {
            GameMode gameMode = p.getGameMode();
            return gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE;
        }
        return false;
    }
}
