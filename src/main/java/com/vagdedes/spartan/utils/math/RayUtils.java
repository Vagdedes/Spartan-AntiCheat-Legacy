package com.vagdedes.spartan.utils.math;

import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.RayLine;
import org.bukkit.World;
import org.bukkit.block.Block;

public class RayUtils {

    public static double scaleVal(double value, double scale) {
        double scale2 = Math.pow(10, scale);
        return Math.ceil(value * scale2) / scale2;
    }

    public static boolean validRayLines(RayLine a, RayLine b, double radi) {
        double angleA = Math.atan2(a.z(), a.x());
        double angleB = Math.atan2(b.z(), b.x());

        double angleDiff = Math.abs(angleA - angleB);

        if (angleDiff > Math.PI) {
            angleDiff = 2 * Math.PI - angleDiff;
        }

        return angleDiff <= Math.toRadians(radi);
    }

    public static double calculateRayLines(RayLine a, RayLine b) {
        double angleA = Math.atan2(a.z(), a.x());
        double angleB = Math.atan2(b.z(), b.x());

        double angleDiff = Math.abs(angleA - angleB);

        if (angleDiff > Math.PI) {
            angleDiff = 2 * Math.PI - angleDiff;
        }

        return Math.toDegrees(angleDiff);
    }

    public static double calculateRayLine(RayLine a) {
        return Math.atan2(a.z(), a.x());
    }

    public static float masterCast(float num) {
        return sinusoidOnlyMin(castTo360(num));
    }

    public static float castTo360(float num) {
        float value = Math.abs((num + 360) % 360 - 180);
        // int wrapShitMath = (int) Math.floor(value / 360);
        return value;
    }

    public static float castTo360WithLimit(float num) {
        float value = Math.abs((num + 360) % 360 - 180);
        int wrapShitMath = (int) Math.floor(value / 360);
        return value - (wrapShitMath * 360);
    }

    public static float sinusoidOnlyMin(float num) {
        int wrapShitMath = (int) Math.floor(num / 180);
        return num - (wrapShitMath * 180);
    }

    public static boolean onBlock(SpartanPlayer player, SpartanLocation location, String material) {
        World world = player.getWorld();
        org.bukkit.util.Vector playerLocation = location.toVector();
        org.bukkit.util.Vector min = playerLocation.clone().add(new org.bukkit.util.Vector(-0.3, -0.5, -0.3));
        org.bukkit.util.Vector max = playerLocation.clone().add(new org.bukkit.util.Vector(0.3, 0.1, 0.3));
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType().toString().toLowerCase().contains(material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean onSolidBlock(SpartanPlayer player, SpartanLocation location) {
        World world = player.getWorld();
        org.bukkit.util.Vector playerLocation = location.toVector();
        org.bukkit.util.Vector min = playerLocation.clone().add(new org.bukkit.util.Vector(-0.3, -0.5, -0.3));
        org.bukkit.util.Vector max = playerLocation.clone().add(new org.bukkit.util.Vector(0.3, 0.1, 0.3));
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType().isSolid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static double getOnlyScale(double value) {
        return value - Math.floor(value);
    }
}
