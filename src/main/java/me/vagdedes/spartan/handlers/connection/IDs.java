package me.vagdedes.spartan.handlers.connection;

import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.features.synchronicity.cloud.CloudFeature;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.Objects;

public class IDs {

    private static final String
            user = "%%__USER__%%",
            defaultLocalID = "0";
    private static String
            nonce = "%%__NONCE__%%",
            localID = defaultLocalID;

    public static final boolean
            hasUserIDByDefault = !user.startsWith("%%__");
    static boolean preview = !Piracy.enabled && !hasUserIDByDefault;

    private static int platform = 0;

    static {
        if (!nonce.startsWith("%%__") && !AlgebraUtils.validInteger(nonce)) {
            nonce = String.valueOf(Objects.hash(nonce));
        }
    }

    static void setUserID(int id) {
        localID = Integer.toString(id);
        preview = false;
        Config.refreshVariables(false);
    }

    // Platforms

    public static String user() {
        return !Piracy.enabled ? localID : user;
    }

    public static String nonce() {
        return !Piracy.enabled ? (CloudFeature.hasToken() ? Integer.toString(CloudFeature.getRawToken().hashCode()) : localID) : nonce;
    }

    static String resource() {
        return !Piracy.enabled ? localID : "%%__RESOURCE__%%";
    }

    static String site() {
        return "https://vagdedes.com/minecraft/cloud/verification/?id=&nonce=";
    }

    // Platforms

    public static boolean isValid() {
        if (Piracy.enabled) {
            return true;
        }
        String user = user();
        return !user.equals(defaultLocalID) && user.equals(resource());
    }

    public static void setPlatform(int id) {
        platform = id;
    }

    public static boolean isBuiltByBit() {
        return platform == 2 || "%%__FILEHASH__%%".length() != 16;
    }

    public static boolean isSongoda() {
        return platform == 4 || nonce().length() == 36;
    }

    public static boolean isPolymart() {
        return platform == 3 || "%%__POLYMART__%%".length() == 1;
    }

    public static boolean isPreview() {
        return preview && Piracy.isSupplied();
    }

    public static boolean isSpigotMC(boolean dismissOthers) {
        return platform == 1 || Piracy.enabled && !CloudFeature.hasToken() && (!dismissOthers || !isBuiltByBit() && !isSongoda() && !isPolymart());
    }

    public static String getPlatform(boolean notNull) {
        return IDs.isBuiltByBit() ? "BuiltByBit" : IDs.isPolymart() ? "Polymart" : IDs.isSongoda() ? "Songoda" : notNull || isSpigotMC(false) ? "SpigotMC" : null;
    }

    public static String hide(String id) {
        double version = Double.parseDouble(API.getVersion().substring(6));
        double number = AlgebraUtils.cut(Integer.parseInt(id) / version, 6);
        return String.valueOf(number).replace("-", "*").replace(".", "-");
    }
}
