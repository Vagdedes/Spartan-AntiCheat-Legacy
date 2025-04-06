package ai.idealistic.vacan.functionality.connection;

import ai.idealistic.vacan.Register;
import ai.idealistic.vacan.abstraction.check.CheckEnums;
import ai.idealistic.vacan.functionality.server.PluginBase;
import ai.idealistic.vacan.utils.java.ReflectionUtils;
import ai.idealistic.vacan.utils.math.AlgebraUtils;
import org.bukkit.Bukkit;

import java.io.File;

public class PluginAddons {

    private static final boolean[] ownedChecks = new boolean[CheckEnums.HackType.values().length];

    public static void refresh() {
        if (IDs.builtByBit) {
            for (CheckEnums.HackType hackType : CheckEnums.HackType.values()) {
                if (hackType.category == CheckEnums.HackCategoryType.MISCELLANEOUS) {
                    ownedChecks[hackType.ordinal()] = true;
                }
            }
            PluginBase.dataThread.executeIfUnknownThreadElseHere(() -> {
                File directory = new File(Register.plugin.getDataFolder() + "/addons");

                if (directory.isFile()) {
                    directory.delete();
                }
                directory.mkdirs();
                File[] files = directory.listFiles();

                if (files != null
                        && files.length > 0) {
                    for (CheckEnums.HackType hackType : CheckEnums.HackType.values()) {
                        if (ownedChecks[hackType.ordinal()]) {
                            continue;
                        }
                        for (File file : files) {
                            if (file.isFile()
                                    && file.getName().endsWith(".jar")) {
                                Object object = ReflectionUtils.getFieldFromJar(
                                        file.getAbsolutePath(),
                                        Register.mainPackage + ".addon." + hackType.name(),
                                        "user"
                                );

                                if (object != null
                                        && (object.toString().equals(IDs.user)
                                        || !AlgebraUtils.validInteger(object.toString()))) {
                                    ownedChecks[hackType.ordinal()] = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        } else if (IDs.polymart) {
            String combat = "350",
                    movement = "3600",
                    world = "984";

            if (IDs.resource.equals(combat)) {
                for (CheckEnums.HackType hackType : CheckEnums.HackType.values()) {
                    if (hackType.category == CheckEnums.HackCategoryType.COMBAT
                            || hackType.category == CheckEnums.HackCategoryType.MISCELLANEOUS) {
                        ownedChecks[hackType.ordinal()] = true;
                    }
                }
            } else if (IDs.resource.equals(movement)) {
                for (CheckEnums.HackType hackType : CheckEnums.HackType.values()) {
                    if (hackType.category == CheckEnums.HackCategoryType.MOVEMENT
                            || hackType.category == CheckEnums.HackCategoryType.MISCELLANEOUS) {
                        ownedChecks[hackType.ordinal()] = true;
                    }
                }
            } else if (IDs.resource.equals(world)) {
                for (CheckEnums.HackType hackType : CheckEnums.HackType.values()) {
                    if (hackType.category == CheckEnums.HackCategoryType.WORLD
                            || hackType.category == CheckEnums.HackCategoryType.MISCELLANEOUS) {
                        ownedChecks[hackType.ordinal()] = true;
                    }
                }
            }
            PluginBase.dataThread.executeIfUnknownThreadElseHere(() -> {
                String pluginsFolder = Register.plugin.getDataFolder().toString().replace(
                        Register.pluginName,
                        ""
                );

                for (File file : new File(pluginsFolder).listFiles()) {
                    if (file.isFile()
                            && file.getName().endsWith(".jar")) {
                        String clazz = IDs.class.getCanonicalName();
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
                                if (object.toString().equals(combat)) {
                                    for (CheckEnums.HackType hackType : CheckEnums.HackType.values()) {
                                        if (hackType.category == CheckEnums.HackCategoryType.COMBAT) {
                                            ownedChecks[hackType.ordinal()] = true;
                                        }
                                    }
                                } else if (object.toString().equals(movement)) {
                                    for (CheckEnums.HackType hackType : CheckEnums.HackType.values()) {
                                        if (hackType.category == CheckEnums.HackCategoryType.MOVEMENT) {
                                            ownedChecks[hackType.ordinal()] = true;
                                        }
                                    }
                                } else if (object.toString().equals(world)) {
                                    for (CheckEnums.HackType hackType : CheckEnums.HackType.values()) {
                                        if (hackType.category == CheckEnums.HackCategoryType.WORLD) {
                                            ownedChecks[hackType.ordinal()] = true;
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            });
        } else if (IDs.spigot
                || Bukkit.getMotd().contains(Register.pluginName)) {
            for (CheckEnums.HackType hackType : CheckEnums.HackType.values()) {
                ownedChecks[hackType.ordinal()] = true;
            }
        }
    }

    public static boolean ownsCheck(CheckEnums.HackType hackType) {
        return ownedChecks[hackType.ordinal()];
    }

}
