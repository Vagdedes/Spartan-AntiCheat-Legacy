package me.vagdedes.spartan.handlers.stability;

import me.vagdedes.spartan.compatibility.own.UltimateStatistics;
import me.vagdedes.spartan.objects.profiling.ViolationStatistics;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class CancelViolation {

    public static final int extra = 1;
    private static final String masterKey = "cancel-violation";
    private static final Map<Integer, Integer> memory = new LinkedHashMap<>(Enums.hackTypes().length * ResearchEngine.DataType.values().length);
    private static final int cooldown = 3;

    static void clear() {
        memory.clear();
    }

    static void refresh(Enums.HackType[] hackTypes) {
        for (Enums.HackType hackType : hackTypes) {
            Check check = hackType.getCheck();

            for (ResearchEngine.DataType dataType : ResearchEngine.getDynamicUsableDataTypes(true)) {
                int key = getKey(hackType, dataType);

                if (check.hasCancelViolation()) {
                    memory.put(key, check.getCancelViolation());
                } else if (!ViolationStatistics.has(hackType, ResearchEngine.profileRequirement)) {
                    memory.put(key, check.getDefaultCancelViolation());
                } else {
                    int defaultCancelViolation = check.getDefaultCancelViolation(),
                            falsePositives = ResearchEngine.getTotalFalsePositives(hackType, dataType);
                    memory.put(key,
                            defaultCancelViolation
                                    + (falsePositives == 0 ? 0 : AlgebraUtils.integerCeil(falsePositives / ((double) defaultCancelViolation)))
                    );
                }
            }
        }
    }

    private static int getKey(Enums.HackType hackType, ResearchEngine.DataType dataType) {
        return hackType.ordinal() * (dataType.ordinal() + 1); // Plus one because it starts from zero
    }

    // Separator

    public static boolean isForced(SpartanPlayer player, Enums.HackType hackType) {
        return !player.getCooldowns().canDo(masterKey + hackType.getStringOrdinal())
                || UltimateStatistics.isSuspected(player.getUniqueId());
    }

    public static void force(SpartanPlayer player, Enums.HackType hackType) {
        Check check = hackType.getCheck();

        if (check.isUsingDefaultCancelViolation(get(hackType, player.getDataType()), 1)) {
            player.getCooldowns().add(masterKey + check.getStringOrdinal(), cooldown);
        }
    }

    public static int get(Enums.HackType hackType, ResearchEngine.DataType dataType) {
        return TestServer.isIdentified() ? Check.minimumDefaultCancelViolation :
                memory.getOrDefault(getKey(hackType, dataType), hackType.getCheck().getDefaultCancelViolation());
    }
}
