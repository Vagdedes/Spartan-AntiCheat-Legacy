package ai.idealistic.vacan.functionality.connection;

import ai.idealistic.vacan.Register;
import ai.idealistic.vacan.abstraction.check.Check;
import ai.idealistic.vacan.abstraction.check.CheckEnums;
import ai.idealistic.vacan.functionality.server.PluginBase;
import ai.idealistic.vacan.utils.java.ReflectionUtils;

import java.io.File;

public class PluginAddons {

    private static final boolean[] ownedChecks = new boolean[Check.DataType.values().length];

    public static void refresh() {
        if (IDs.builtByBit) {
            PluginBase.dataThread.executeIfUnknownThreadElseHere(() -> {
                File[] files = new File(Register.plugin.getDataFolder() + "/addons").listFiles();

                if (files != null
                        && files.length > 0) {
                    for (CheckEnums.HackType hackType : CheckEnums.HackType.values()) {
                        if (ownedChecks[hackType.ordinal()]) {
                            continue;
                        }
                        for (File file : files) {
                            if (file.isFile()
                                    && file.getName().endsWith(".jar")) {
                                String clazz = Register.mainPackage + ".addon." + hackType.name().toLowerCase();
                                Object object = ReflectionUtils.getFieldFromJar(
                                        file.getAbsolutePath(),
                                        clazz,
                                        "user"
                                );

                                if (object != null
                                        && object.toString().equals(IDs.user)) {
                                    object = ReflectionUtils.getFieldFromJar(
                                            file.getAbsolutePath(),
                                            clazz,
                                            "resource"
                                    );

                                    if (object != null
                                            && !object.toString().equals(IDs.resource)) {
                                        ownedChecks[hackType.ordinal()] = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            });
        } else {
            for (CheckEnums.HackType hackType : CheckEnums.HackType.values()) {
                ownedChecks[hackType.ordinal()] = true;
            }
        }
    }

    // Detections

    public static boolean ownsCheck(CheckEnums.HackType hackType) {
        return ownedChecks[hackType.ordinal()];
    }

}
