package com.vagdedes.spartan.abstraction.check.implementation.movement.speed;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPotionEffect;
import com.vagdedes.spartan.functionality.tracking.RayLine;
import com.vagdedes.spartan.utils.math.RayUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Speed extends CheckExecutor {

    /*
    Anticipate movements in advance = have good performance in the future
     */
    public static final double[] BASIC_SPEED = new double[]{
            0.63, // 0
            0.37,
            0.37,
            0.36,
            0.35,
            0.35,
            0.34,
            0.34,
            0.34,
            0.33,
            0.32,
            0.32,
            0.32 // 12
    };

    public static final double[] FLY_SPEED = new double[]{
            0.42, 0.34, 0.25, 0.17, 0.09, 0.01, -0.07, -0.15, -0.22, -0.3,
            -0.37, -0.44, -0.51, -0.58, -0.64, -0.71, -0.77, -0.84, -0.9,
            -0.96, -1.02, -1.08, -1.13, -1.24, -1.19, -1.3, -1.35, -1.4,
            -1.45, -1.5, -1.55, -1.6, -1.64, -1.69, -1.73, -1.78, -1.82,
            -1.86, -1.9, -1.94, -1.98, -2.02, -2.06, -2.1, -2.13, -2.17,
            -2.2, -2.24, -2.27, -2.3, -2.34, -2.37, -2.4, -2.43, -2.46,
            -2.49, -2.52, -2.54, -2.6, -2.57, -2.65, -2.62, -2.68, -2.7,
            -2.72, -2.75, -2.77, -2.79, -2.82, -2.84, -2.86, -2.88, -2.9,
            -2.92, -2.94, -2.96, -2.98, -3.0
    };


    private static final Map<UUID, KeepSprint> keepSprint = new ConcurrentHashMap<>();
    private RayLine jumpRayLine;
    private boolean
            onDamage,
            ignoreBasicChecks,
            collideMinecraftShitMathFactor,
            newRayRequest,
            isJump;
    private float jumpRotation;
    private int
            antiAbuseProcessor,
            collideMinecraftShitMathFactorValue,
            wrappedOnAir,
            wrappedOnFly,
            verticalVl,
            sprintVl;
    private double yDiff,
            onGroundMinecraftShitMathFactor,
            multiply;

    public Speed(SpartanPlayer player) {
        super(Enums.HackType.Speed, player);
    }

    @Override
    protected void runInternal(boolean cancelled) {
        String flag = null;
        SpartanLocation to = player.movement.getLocation(),
                from = player.movement.getEventFromLocation();
        boolean onGround = player.isOnGroundDefault();
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        int oldAir = -1;
        double speedModify = getMaxSpeedAmplifier();
        if (newRayRequest) {
            newRayRequest = false;
            jumpRayLine = new RayLine(dx, dz);
        }
        double vertical = RayUtils.scaleVal(to.getY() - from.getY(), 2);
        double horizontal = RayUtils.scaleVal(Math.sqrt(dx * dx + dz * dz), 2);
        float speedMultiply = (player.getWalkSpeed() * 5) < 1 ? 1 : (player.getWalkSpeed() * 5);
        SpartanPotionEffect speedEffect = player.getPotionEffect(PotionEffectType.SPEED, 1L);
        SpartanPotionEffect glideEffect = player.getPotionEffect(PotionEffectType.SLOW_FALLING, 1L);
        SpartanPotionEffect levitationEffect = player.getPotionEffect(PotionEffectType.LEVITATION, 1L);
        SpartanPotionEffect dolphinEffect = player.getPotionEffect(PotionEffectType.DOLPHINS_GRACE, 1L);
        if (player.isOnGround() && wrappedOnFly > 0) oldAir = wrappedOnFly;
        wrappedOnAir = onGround ? 0 : wrappedOnAir + 1;
        wrappedOnFly = onGround ? 0 : wrappedOnFly + 1;
        int onAir = wrappedOnAir;
        int onAirID = wrappedOnAir - 1;
        int onFly = wrappedOnFly;
        int onFlyID = wrappedOnFly - 1;
        double predictedVertical = (onAirID > 76) ? FLY_SPEED[76] : (onAirID < 0) ? FLY_SPEED[0] : FLY_SPEED[onAirID];
        double verticalDiff = (predictedVertical < vertical) ? 0 : predictedVertical - vertical;
        boolean solidBlockOnTop = RayUtils.onSolidBlock(player, player.movement.getLocation().add(0, 2, 0));
        boolean inLiquid = RayUtils.onBlock(player, player.movement.getLocation(), "water") || RayUtils.onBlock(player, player.movement.getLocation(), "lava");
        boolean onBlock = !RayUtils.onBlock(player, player.movement.getLocation(), "air");
        double multiplexSpecify = solidBlockOnTop ? 2.65 : 1;

        if (RayUtils.onBlock(player, player.movement.getLocation(), "ice")) {
            if (multiply < 0.6 * multiplexSpecify) multiply = 0.6 * multiplexSpecify;
        }
        if (RayUtils.onBlock(player, player.movement.getLocation(), "slime")) {
            if (multiply < 0.15) multiply = 0.15;
        }
        if (RayUtils.onBlock(player, player.movement.getLocation(), "honey")) {
            if (multiply < 0.15) multiply = 0.15;
        }
        if (RayUtils.onBlock(player, player.movement.getLocation(), "ladder")
                || RayUtils.onBlock(player, player.movement.getLocation(), "vine")) {
            if (multiply < 0.3) multiply = 0.3;
        }
        if (RayUtils.onBlock(player, player.movement.getLocation(), "stair")) {
            if (multiply < 0.35) multiply = 0.35;
        }
        if (player.movement.isSwimming()) {
            newRayRequest = true;
        }
        if (dolphinEffect != null) if (multiply < 5) multiply = 5D;
        if (solidBlockOnTop) {
            if (multiply < 0.07) multiply = 0.07;
            yDiff = 0D;
            wrappedOnFly = wrappedOnFly + 1;
        } else if (!onBlock) {
            if (multiply < 0.08 && !onGround && vertical != -0.09 && isJump) {
                yDiff = yDiff + verticalDiff;
                if (yDiff > 0.05) {
                    verticalVl = verticalVl + 4;
                    if (verticalVl > 36) {
                        antiAbuseProcessor = antiAbuseProcessor + 45;
                        if (antiAbuseProcessor > 200) {
                            flag = "type: flying, speed: " + vertical
                                    + ", speed-require: " + predictedVertical + ", air-tick: " + onFly;
                            verticalVl = verticalVl - 3;
                            if (wrappedOnFly > 1) wrappedOnFly = wrappedOnFly - 1;
                            yDiff = 0D;
                        }
                    }
                } else if (verticalVl > 0) verticalVl = verticalVl - 1;
            }
            if (oldAir < 3 && oldAir != -1 && isJump) {
                if (!RayUtils.onBlock(player, player.movement.getLocation(), "ladder")
                        && !RayUtils.onBlock(player, player.movement.getLocation(), "vine")) {
                    antiAbuseProcessor = antiAbuseProcessor + 25;
                    if (antiAbuseProcessor > 200) {
                        flag = "type: landing, air: " + oldAir;
                    }
                }
            }
        } else {
            yDiff = 0D;
            wrappedOnFly = wrappedOnFly + 1;
        }
        if (slimeBlockCause(player)) {
            if (multiply < 3) multiply = 3D;
            return;
        }
        if (speedEffect != null) {
            double increaceValue = (speedEffect.bukkitEffect.getAmplifier() + 1) * 0.1D;
            if (multiply < increaceValue) multiply = increaceValue;
        }
        if (glideEffect != null || levitationEffect != null) {
            wrappedOnAir = 1;
            wrappedOnFly = 1;
            if (multiply < 0.1) multiply = 0.1D;
            jumpRayLine = new RayLine(dx, dz);
        }
        if (player.movement.isFlying() || player.movement.isGliding()) {
            wrappedOnAir = 1;
            wrappedOnFly = 1;
            multiply = 8D;
            return;
        }
        if (speedModify > 0) {
            if (multiply < speedModify * 3) multiply = speedModify * 2.65;
        }
        for (int i = 0; i < player.getNearbyEntities(2, 2, 2).size(); i++) {
            if (multiply < 0.4) multiply = 0.4;
        }
        if (player.getVehicle() != null) {
            if (multiply < 9) multiply = 9D;
        }
        if (!onGround) {
            // player.sendMessage("nigger " + horizontal);
            // player.sendMessage("My tick check: " + onAir + " Vagdedes tick check: " + player.movement.getTicksOnAir());
            double limit = (onAirID > 12 ? BASIC_SPEED[12] : BASIC_SPEED[onAirID]) * speedMultiply;
            limit += ((speedEffect != null) ? (speedEffect.bukkitEffect.getAmplifier() + 1) * 0.2D : 0);
            limit += (collideMinecraftShitMathFactor ? 0.05 : 0);
            limit = RayUtils.scaleVal(limit * (1 + multiply), 2);
            double wrapLimit = !player.movement.isSprinting() ? RayUtils.scaleVal(limit / 1.2, 2) : limit;
            if (horizontal > limit) {
                antiAbuseProcessor = antiAbuseProcessor + 75;
                if (antiAbuseProcessor > 200) {
                    flag = "type: simulation-air-limit, speed: " + horizontal
                            + ", speed-limit: " + limit + ", air-tick: " + onAir;
                }
            }
            if (horizontal > wrapLimit && !keepSprint.get(player.uuid).isTimedOut()) {
                antiAbuseProcessor = antiAbuseProcessor + 25;
                if (antiAbuseProcessor > 200) {
                    flag = "type: keepsprint-silent, speed: " + horizontal
                            + ", speed-limit: " + wrapLimit + ", air-tick: " + onAir;
                }
            }
            if (onAir == 1) {
                int diff = Math.abs(player.movement.getLocation().getBlockY() - collideMinecraftShitMathFactorValue);
                if (diff > 0) collideMinecraftShitMathFactor = true;
                collideMinecraftShitMathFactorValue = player.movement.getLocation().getBlockY();
                onGroundMinecraftShitMathFactor = 1.6;
                jumpRayLine = new RayLine(dx, dz);
                jumpRotation = to.getYaw();
                yDiff = 0D;
                keepSprint.put(player.uuid, new KeepSprint(0, false, 0, System.currentTimeMillis()));
                isJump = (vertical > 0);
                {
                    float yaw = to.getYaw();
                    float yR = (float) Math.toRadians(yaw);
                    RayLine a = new RayLine(-Math.sin(yR), Math.cos(yR));
                    RayLine b = new RayLine(dx, dz);
                    // player.sendMessage(String.valueOf(horizontal));
                    double diffRayLines = RayUtils.calculateRayLines(a, b);
                    double wrappedLimit = limit / 1.6 + ((diffRayLines >= 90) ? 0 : (limit / 2.4) * (1 - diffRayLines / 90));
                    if (horizontal > RayUtils.scaleVal(wrappedLimit, 2) && speedModify < 1.0 && !ignoreBasicChecks) {
                        flag = "type: simulation-air-direction (basic), speed: " + horizontal
                                + ", speed-limit: " + RayUtils.scaleVal(wrappedLimit, 2)
                                + ", diff: " + RayUtils.scaleVal(diffRayLines, 2);
                    }
                }
            } else if (jumpRayLine != null) {
                boolean validPsychic = RayUtils.validRayLines(new RayLine(dx, dz), jumpRayLine, 35);
                boolean muchThanAllowedStrafe = horizontal > limit / 1.4;
                boolean muchThanAllowedRotation = horizontal > limit / 1.5;
                if (!player.movement.isSprinting()) {
                    keepSprint.get(player.uuid).sprint();
                }

                if (keepSprint.get(player.uuid).hits() > 0 && !keepSprint.get(player.uuid).sprinted()
                        && onAir > 2 && muchThanAllowedRotation) {
                    keepSprint.get(player.uuid).newFlag();
                    if (keepSprint.get(player.uuid).flags() > 2) {
                        antiAbuseProcessor = antiAbuseProcessor + 210;
                        if (antiAbuseProcessor > 200) {
                            flag = "type: keepsprint, hits: " + keepSprint.get(player.uuid).hits();
                        }
                    }
                }

                if (!validPsychic && muchThanAllowedStrafe) {
                    antiAbuseProcessor = antiAbuseProcessor + 50;
                    if (antiAbuseProcessor > 200) {
                        flag = "type: simulation-air-physics, speed: " + horizontal
                                + ", speed-limit: " + RayUtils.scaleVal(limit / 1.4, 2) + ", air-tick: " + onAir;
                    }
                }

                {
                    float yaw = to.getYaw();
                    float yR = (float) Math.toRadians(yaw);
                    RayLine a = new RayLine(-Math.sin(yR), Math.cos(yR));
                    RayLine b = new RayLine(dx, dz);
                    if (horizontal > limit / 1.35 && !RayUtils.validRayLines(a, b, 90)
                            && speedModify < 1.0 && !onDamage) {
                        antiAbuseProcessor = antiAbuseProcessor + 60;
                        if (antiAbuseProcessor > 200) {
                            flag = "type: simulation-air-direction (blatant), speed: " + horizontal
                                    + ", speed-limit: " + RayUtils.scaleVal(limit / 1.35, 2);
                        }
                    }
                }

            }
        } else {
            collideMinecraftShitMathFactor = false;
            double limit = 0.3D * (speedMultiply * 1.2) * ((speedModify > 0) ? 2 : 1);

            if (!keepSprint.get(player.uuid).isTimedOut() && !player.movement.isSprinting()) {
                sprintVl = sprintVl + 2;
                if (sprintVl > 4) {
                    limit = RayUtils.scaleVal(limit / 1.15, 2);
                }
            }
            if (sprintVl > 0) sprintVl = sprintVl - 1;

            limit = RayUtils.scaleVal(limit * (1 + multiply), 2);
            float yaw = to.getYaw();
            float yR = (float) Math.toRadians(yaw);
            RayLine a = new RayLine(-Math.sin(yR), Math.cos(yR));
            RayLine b = new RayLine(dx, dz);
            if (onGroundMinecraftShitMathFactor > 0) {
                onGroundMinecraftShitMathFactor = onGroundMinecraftShitMathFactor / 1.33;
                limit = RayUtils.scaleVal(limit * (1 + onGroundMinecraftShitMathFactor), 2);
                if (onGroundMinecraftShitMathFactor < 0.01) onGroundMinecraftShitMathFactor = 0D;
            }
            if (horizontal > limit) {
                antiAbuseProcessor = antiAbuseProcessor + 110;
                if (antiAbuseProcessor > 200) {
                    flag = "type: simulation-ground, speed: " + horizontal
                            + ", speed-limit: " + limit;
                }
            }
            if (horizontal > limit / 1.35 && !RayUtils.validRayLines(a, b, 88)
                    && speedModify < 1.0) {
                antiAbuseProcessor = antiAbuseProcessor + 60;
                if (antiAbuseProcessor > 200) {
                    flag = "type: simulation-ground-direction (basic), speed: " + horizontal
                            + ", speed-limit: " + RayUtils.scaleVal(limit / 1.35, 2);
                }
            }
            if (horizontal > limit / 1.1 && !RayUtils.validRayLines(a, b, 25)
                    && speedModify < 1.0) {
                antiAbuseProcessor = antiAbuseProcessor + 90;
                if (antiAbuseProcessor > 200) {
                    flag = "type: simulation-ground-direction (blatant), speed: " + horizontal
                            + ", speed-limit: " + RayUtils.scaleVal(limit / 1.35, 2);
                }
            }

            onDamage = false;
            multiply = multiply / 1.4;
            if (multiply < 0.04) multiply = 0D;
        }
        if (flag == null && antiAbuseProcessor > 0)
            antiAbuseProcessor = antiAbuseProcessor - 8;
        if (flag != null) cancel(flag, player.movement.getLocation(), 2);
        ignoreBasicChecks = false;
    }

    @Override
    protected void schedulerInternal() {

    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerVelocityEvent) {
            if (!cancelled) {
                PlayerVelocityEvent event = (PlayerVelocityEvent) object;
                RayLine line = new RayLine(event.getVelocity().getX(), event.getVelocity().getZ());
                Vector to = event.getVelocity();
                double dx = to.getX();
                double dz = to.getZ();
                double speedModify = getMaxSpeedAmplifier();
                newRayRequest = true;
                double horizontal = RayUtils.scaleVal(to.distance(new Vector(0, to.getY(), 0)), 2);
                if (horizontal > 0.05) {
                    horizontal += 0.2;
                    horizontal *= 1.5;
                }
                if (horizontal > multiply) multiply = horizontal;
                jumpRayLine = new RayLine(line.x(), line.z());
                wrappedOnAir = 0;
                wrappedOnFly = 0;
                ignoreBasicChecks = true;
                onDamage = true;
                // player.sendMessage(String.valueOf(horizontal));
            }
        } else if (object instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) object;
            Entity damager = e.getDamager();

            if (damager instanceof Player && e.getEntity() instanceof Player) {
                keepSprint.get(damager.getUniqueId()).newHit();
                keepSprint.get(damager.getUniqueId()).setTimedOut(System.currentTimeMillis());
            }

            double multi = 0.4;
            newRayRequest = true;
            wrappedOnAir = 0;
            wrappedOnFly = 0;
            if (damager instanceof TNTPrimed) {
                multi += 5;
            }
            if (damager instanceof Projectile) {
                if (damager instanceof Arrow) {
                    multi += (((Arrow) damager).getKnockbackStrength() + 1) * 3;
                }
            }
            if (damager instanceof Player) {
                LivingEntity livingEntity = (LivingEntity) damager;
                EntityEquipment equipment = livingEntity.getEquipment();
                ItemStack item = equipment.getItemInMainHand();
                int enchant = item.getEnchantmentLevel(Enchantment.KNOCKBACK);
                multi += enchant * 1.8;

            }
            multiply = multiply + multi;
        } else if (object instanceof PlayerJoinEvent) {
            wrappedOnAir = 0;
            wrappedOnFly = 0;
            multiply = 0D;
            antiAbuseProcessor = 0;
            jumpRayLine = new RayLine(0, 0);
            jumpRotation = player.movement.getLocation().getYaw();
            collideMinecraftShitMathFactorValue = player.movement.getLocation().getBlockY();
            collideMinecraftShitMathFactor = false;
            newRayRequest = true;
            onGroundMinecraftShitMathFactor = 1.6;
            keepSprint.put(player.uuid, new KeepSprint(0, false, 0, System.currentTimeMillis()));
            onDamage = false;
            yDiff = 0D;
            isJump = false;
            verticalVl = 0;
            ignoreBasicChecks = true;
            sprintVl = 0;
        } else if (object == SpeedAction.RIPTIDE) {
            newRayRequest = true;
            wrappedOnAir = 1;
            if (multiply < 9.5D) multiply = 9.5D;
        } else if (object == SpeedAction.RIPTIDE_UNSAFE) {
            newRayRequest = true;
            wrappedOnAir = 1;
            if (multiply < 36D) multiply = 36D;
        }
    }

    private boolean slimeBlockCause(SpartanPlayer player) {
        SpartanLocation location = player.movement.getLocation();
        boolean slime = false;
        boolean piston = false;
        for (int x = location.getBlockX() - 1; x <= location.getBlockX() + 1; x++) {
            for (int y2 = location.getBlockY() - 1; y2 <= location.getBlockY() + 1; y2++) {
                for (int z = location.getBlockZ() - 1; z <= location.getBlockZ() + 1; z++) {
                    if (player.getWorld().getBlockAt(x, y2, z).getType() == Material.SLIME_BLOCK
                            || player.getWorld().getBlockAt(x, y2, z).getType() == Material.PISTON) {
                        piston = true;
                    }
                    if (player.getWorld().getBlockAt(x, y2, z).getType().toString().toLowerCase().contains("slime")) {
                        slime = true;
                    }
                }
            }
        }
        return piston && slime;
    }

    private double getMaxSpeedAmplifier() {
        PlayerInventory inventory = player.getInventory();

        if (inventory != null) {
            double maxAmplifier = 0.0;
            ItemStack mainHandItem = inventory.getItemInMainHand();

            double mainHandAmplifier = getSpeedAmplifier(mainHandItem);
            maxAmplifier = Math.max(maxAmplifier, mainHandAmplifier);

            ItemStack offHandItem = inventory.getItemInOffHand();
            double offHandAmplifier = getSpeedAmplifier(offHandItem);
            maxAmplifier = Math.max(maxAmplifier, offHandAmplifier);

            for (ItemStack armor : inventory.getArmorContents()) {
                double armorAmplifier = getSpeedAmplifier(armor);
                maxAmplifier = Math.max(maxAmplifier, armorAmplifier);
            }
            return maxAmplifier;
        } else {
            return 0.0;
        }
    }


    private static double getSpeedAmplifier(ItemStack item) {
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasAttributeModifiers()) {
            return item.getItemMeta().getAttributeModifiers(Attribute.GENERIC_MOVEMENT_SPEED).stream()
                    .mapToDouble(AttributeModifier::getAmount)
                    .max().orElse(0.0);
        }
        return 0.0;
    }

    @Override
    protected boolean canDo() {
        return true;
    }
}
