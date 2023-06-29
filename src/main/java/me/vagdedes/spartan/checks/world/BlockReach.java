package me.vagdedes.spartan.checks.world;

import me.vagdedes.spartan.compatibility.manual.abilities.AureliumSkills;
import me.vagdedes.spartan.compatibility.manual.building.MineBomb;
import me.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import me.vagdedes.spartan.compatibility.manual.building.TreeFeller;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import org.bukkit.Material;
import org.bukkit.entity.Strider;

public class BlockReach {

    private static final HackType check = HackType.BlockReach;
    private static final double minimumLimit = 6.0;

    public static void runBreak(SpartanPlayer p, SpartanBlock b) {
        if (!canDo()) {
            return;
        }
        Material item;

        if (!canDo(p, b)
                || (item = p.getItemInHand().getType()) == Material.FLINT_AND_STEEL
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && item == Material.END_CRYSTAL) {
            return;
        }
        SpartanLocation to = p.getLocation();
        SpartanLocation bloc = b.getLocation();
        double distance = Math.min(to.distance(bloc), to.clone().add(0, p.getEyeHeight(), 0).distance(bloc)),
                limit = Math.max(
                        check.getCheck().getDecimalOption("overall_distance", 7.5),
                        p.wasFlying() ? MoveUtils.chunk : minimumLimit
                ),
                multiplier = getMultiplier(p, b, null);

        if (distance <= MoveUtils.chunk && distance >= (limit * multiplier)) {
            new HackPrevention(p, check, "type: break, distance: " + distance + ", limit: " + limit + ", multiplier: " + multiplier);
        }
    }

    public static void runInteract(SpartanPlayer p, SpartanBlock b) {
        if (!canDo()) {
            return;
        }
        SpartanLocation bloc;

        if (!canDo(p, b) || !BlockUtils.isChangeable(bloc = b.getLocation())) {
            return;
        }
        SpartanLocation to = p.getLocation();
        double distance = Math.min(to.distance(bloc), to.clone().add(0, p.getEyeHeight(), 0).distance(bloc)),
                limit = Math.max(
                        check.getCheck().getDecimalOption("overall_distance", 7.5),
                        p.wasFlying() ? MoveUtils.chunk : minimumLimit
                ),
                multiplier = getMultiplier(p, b, null);

        if (distance <= MoveUtils.chunk && distance >= (limit * multiplier)) {
            new HackPrevention(p, check, "type: interact, distance: " + distance + ", limit: " + limit + ", multiplier: " + multiplier);
        }
    }

    public static void runPlace(SpartanPlayer p, SpartanBlock b, SpartanBlock ba) {
        if (!canDo()) {
            return;
        }
        Material type;
        SpartanLocation baLoc;

        if (!canDo(p, b)
                || !BlockUtils.isFullSolid((baLoc = ba.getLocation()))
                || p.getItemInHand().getType() == (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.BONE_MEAL : Material.getMaterial("INK_SACK"))
                || BlockUtils.areBeds(type = b.getType()) || type == Material.BEDROCK) {
            return;
        }
        SpartanLocation to = p.getLocation(), bloc = b.getLocation();
        double ypos = bloc.getY() - to.getY(),
                distance = Math.min(to.distance(bloc), to.clone().add(0, p.getEyeHeight(), 0).distance(bloc)),
                ab_distance = to.distance(baLoc) + 0.4;
        boolean incompatibility = Compatibility.CompatibilityType.MajorIncompatibility.isFunctional();

        if (!p.isBedrockPlayer() && p.getVehicle() == null && !incompatibility && ypos <= 0.5
                && distance >= 1.4 && distance > ab_distance && distance <= MoveUtils.chunk) {
            new HackPrevention(p, check, "type: unusual, distance: " + distance + ", block-against-distance: " + ab_distance + ", vertical-distance: " + ypos);
        } else {
            double limit = Math.max(
                    check.getCheck().getDecimalOption("overall_distance", 7.5),
                    p.wasFlying() ? MoveUtils.chunk : minimumLimit
            ),
                    multiplier = getMultiplier(p, b, incompatibility);

            if (distance <= MoveUtils.chunk && distance >= (limit * multiplier)) {
                new HackPrevention(p, check, "type: place, distance: " + distance + ", limit: " + limit + ", multiplier: " + multiplier);
            }
        }
    }

    // Separator

    private static double getMultiplier(SpartanPlayer p, SpartanBlock b, Boolean incompatibility) {
        return p.isBedrockPlayer() || LagLeniencies.hasHighLatency(p, LagLeniencies.continentPing) || (incompatibility != null ? incompatibility : Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()) ? 2 : // Building
                MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && p.getVehicle() instanceof Strider ? 1.5 :
                        BlockUtils.isSensitive(p, b.getType()) ? Math.min(1 + LagLeniencies.getDelay(p), 1.5) : 1; // Building
    }

    private static boolean canDo() {
        return !Compatibility.CompatibilityType.mcMMO.isFunctional();
    }

    private static boolean canDo(SpartanPlayer p, SpartanBlock b) {
        return p.canRunCheck(check, true)
                && !BlockUtils.isScaffoldingBlock(b.getType())

                && !PrinterMode.canCancel(p)
                && !MineBomb.canUse(p)
                && !AureliumSkills.canCancel(p)
                && !TreeFeller.canCancel(p, b)
                && !MineBomb.canUse(p)
                && !Compatibility.CompatibilityType.CraftBook.isFunctional();
    }
}
