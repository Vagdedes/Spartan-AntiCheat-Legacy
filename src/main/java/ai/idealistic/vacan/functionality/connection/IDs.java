package ai.idealistic.vacan.functionality.connection;

import ai.idealistic.vacan.utils.math.AlgebraUtils;

import java.util.Objects;

public class IDs {

    public static final String resource = "%%__RESOURCE__%%";
    public static final boolean
            enabled = AlgebraUtils.validInteger(resource),
            builtByBit = "%%__BUILTBYBIT__%%".length() != 18,
            polymart = "%%__POLYMART__%%".length() == 1,
            spigot = !builtByBit && !polymart;
    public static final String
            user = "%%__USER__%%",
            file = "%%__NONCE__%%";
    private static String
            userModified = null,
            fileModified = null;

    static void set(int user, int nonce) {
        IDs.userModified = Integer.toString(user);
        IDs.fileModified = Integer.toString(nonce);
    }

    public static String user() {
        return userModified != null ? userModified : user;
    }

    public static String file() {
        if (IDs.enabled) {
            String f = fileModified != null ? fileModified : file;

            if (!f.startsWith("%%__")
                    && !AlgebraUtils.validInteger(f)) {
                f = String.valueOf(Objects.hash(f));
            }
            return f;
        } else {
            return user();
        }
    }

    static String platform() {
        return IDs.builtByBit ? "BuiltByBit" : IDs.polymart ? "Polymart" : "SpigotMC";
    }

    public static boolean canAdvertise() {
        return !IDs.enabled || IDs.builtByBit || IDs.polymart;
    }

}
