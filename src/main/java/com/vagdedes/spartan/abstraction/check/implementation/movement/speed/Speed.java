package com.vagdedes.spartan.abstraction.check.implementation.movement.speed;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanBlock;
import com.vagdedes.spartan.abstraction.player.SpartanLocation;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.player.SpartanPotionEffect;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.listeners.protocol.modules.PlayerStayEvent;
import com.vagdedes.spartan.utils.math.RayLine;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.World;
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

import java.util.LinkedList;

import static com.vagdedes.spartan.abstraction.check.implementation.movement.speed.DataProcessor.keepSprint;

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


    private RayLine jumpRayLine;
    private Entity target;
    private boolean
            onDamage,
            ignoreBasicChecks,
            collideMinecraftShitMathFactor,
            newRayRequest,
            needToCheckAura,
            isJump;
    private float jumpRotation;
    private LinkedList<Aura> auraAnalysis;
    private int
            antiAbuseProcessor,
            collideMinecraftShitMathFactorValue,
            wrappedOnAir,
            wrappedOnFly,
            oldWrappedOnFly,
            verticalVl,
            ticksOnGround,
            sprintVl;
    private double yDiff,
            onGroundMinecraftShitMathFactor,
            oldYaw,
            multiply;

    public Speed(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
    }

    @Override
    protected void runInternal(boolean cancelled) {
        SpartanLocation to = player.movement.getEventToLocation(),
                from = player.movement.getEventFromLocation();
        boolean onGround = player.isOnGround(false);
        SpartanPotionEffect speedEffect = player.getPotionEffect(PotionEffectType.SPEED, 0);
        SpartanPotionEffect jumpEffect = player.getPotionEffect(PotionEffectType.JUMP, 0);
        SpartanPotionEffect glideEffect = PlayerUtils.slowFall
                ? player.getPotionEffect(PotionEffectType.SLOW_FALLING, 0)
                : null;
        SpartanPotionEffect levitationEffect = PlayerUtils.levitation
                ? player.getPotionEffect(PotionEffectType.LEVITATION, 0)
                : null;
        SpartanPotionEffect dolphinEffect = PlayerUtils.dolphinsGrace
                ? player.getPotionEffect(PotionEffectType.DOLPHINS_GRACE, 0)
                : null;
        double speedModify = getMaxSpeedAmplifier();
        PlayerInventory inventory = player.getInventory();
        Entity vehicle = player.getVehicle();

        String flag = null;
        double dx = to.getX() - from.getX(),
                dz = to.getZ() - from.getZ();
        int oldAir = -1;

        if (newRayRequest) {
            newRayRequest = false;
            jumpRayLine = new RayLine(dx, dz);
        }
        double vertical = RayUtils.scaleVal(to.getY() - from.getY(), 2),
                horizontal = RayUtils.scaleVal(Math.sqrt(dx * dx + dz * dz), 2);
        float speedMultiply = (player.getWalkSpeed() * 5) < 1 ? 1 : (player.getWalkSpeed() * 5);
        if (onGround && wrappedOnFly > 0) oldAir = wrappedOnFly;
        wrappedOnAir = onGround ? 0 : wrappedOnAir + 1;
        wrappedOnFly = onGround ? 0 : wrappedOnFly + 1;
        int onAir = wrappedOnAir,
                onAirID = wrappedOnAir - 1,
                onFly = wrappedOnFly,
                onFlyID = wrappedOnFly - 1;


        double predictedVertical = (onAir > 76) ? FLY_SPEED[76] : (onAir < 0) ? FLY_SPEED[0] : FLY_SPEED[onAir];
        double verticalDiff = (predictedVertical < vertical) ? 0 : predictedVertical - vertical;
        boolean solidBlockOnTop = RayUtils.onSolidBlock(player, to.clone().add(0, 2, 0)),
                inLiquid = RayUtils.onBlock(player, to, BlockUtils.liquid),
                onBlock = RayUtils.onBlock(player, to, BlockUtils.solid, BlockUtils.semi_solid);
        double multiplexSpecify = solidBlockOnTop ? 2.65 : 1;

        // 2
        //player.getInstance().sendMessage(onGround + " " + onAir + " fly: " + vertical + " | " + predictedVertical);

        float yaw = to.getYaw(),
                yR = (float) Math.toRadians(yaw);
        RayLine a = new RayLine(-Math.sin(yR), Math.cos(yR));
        RayLine b = new RayLine(dx, dz);

        //1
        //Register.plugin.getLogger().info(yaw + " " + horizontal + " "
        //                + onAirID + " " + onGround + " " + Move.groundManager.get(player.getInstance()));

        double wrappedYaw = Math.toDegrees(RayUtils.calculateRayLine(a));
        double deltaYaw = RayUtils.scaleVal(Math.abs(wrappedYaw - oldYaw), 2);


        if (RayUtils.onBlock(player, to, BlockUtils.ice)
                || RayUtils.onBlock(player, to.clone().subtract(0, 1, 0), BlockUtils.ice)) {
            if (multiply < 0.6 * multiplexSpecify) multiply = 0.6 * multiplexSpecify;
        }
        if (RayUtils.onBlock(player, to, BlockUtils.slime_block)
                || RayUtils.onBlock(player, to.clone().subtract(0, 1, 0), BlockUtils.slime_block)) {
            if (multiply < 0.15) multiply = 0.15;
        }
        if (RayUtils.onBlock(player, to, BlockUtils.honey_block)) {
            if (multiply < 0.15) multiply = 0.15;
        }
        if (RayUtils.onBlock(player, to, BlockUtils.climbableOriginal)) {
            if (multiply < 0.3) multiply = 0.3;
        }
        if (RayUtils.onBlock(player, to, BlockUtils.stairs)) {
            if (multiply < 0.35) multiply = 0.35;
        }
        if (player.movement.isSwimming()) {
            newRayRequest = true;
        }
        if (inventory != null
                && MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)
                && inventory.getBoots() != null) {
            int e = inventory.getBoots().getEnchantmentLevel(Enchantment.SOUL_SPEED);
            double v = (e + 1) * 0.4;
            if (e > 0) {
                if (multiply < v) multiply = v;
            }
        }
        if (dolphinEffect != null) if (multiply < 5) multiply = 5D;
        if (inLiquid) {
            yDiff = 0D;
            wrappedOnFly = wrappedOnFly + 1;
        }
        if (solidBlockOnTop) {
            if (multiply < 0.07) multiply = 0.07;
            yDiff = 0D;
            wrappedOnFly = wrappedOnFly + 1;
        } else if (!onBlock) {
            if (multiply < 0.08 && !onGround && vertical != -0.09
                    && isJump && horizontal > 0.1 && vehicle == null) {
                yDiff = yDiff + verticalDiff;
                if (yDiff > 0.05 && onFly < 21) {
                    verticalVl = verticalVl + 4;
                    if (verticalVl > 36) {
                        antiAbuseProcessor = antiAbuseProcessor + 45;
                        if (hackType.getCheck().getBooleanOption("check_flying", true)
                                && antiAbuseProcessor > 200) {
                            flag = "type: flying, speed: " + vertical
                                    + ", speed-require: " + predictedVertical + ", air-tick: " + onAir;
                            verticalVl = verticalVl - 3;
                            if (wrappedOnFly > 1) wrappedOnFly = wrappedOnFly - 1;
                            yDiff = 0D;
                        }
                    }
                } else if (verticalVl > 0) verticalVl = verticalVl - 1;
            }
            if (oldAir < 3 && oldAir != -1 && isJump && vehicle == null
                    && !RayUtils.onBlock(player, to, BlockUtils.climbableOriginal)) {
                antiAbuseProcessor = antiAbuseProcessor + 25;

                if (hackType.getCheck().getBooleanOption("check_landing", true)
                        && antiAbuseProcessor > 200) {
                    flag = "type: landing, air: " + oldAir;
                }
            }
        } else {
            yDiff = 0D;
            wrappedOnFly = wrappedOnFly + 1;
        }
        if (slimeBlockCause(player, to)) {
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
        if (vehicle != null) {
            if (multiply < 9) multiply = 9D;
            ignoreBasicChecks = true;
            newRayRequest = true;
        }
        if (!onGround) {
            // player.sendMessage("nigger " + horizontal);
            // player.sendMessage("My tick check: " + onAir + " Vagdedes tick check: " + player.movement.getTicksOnAir());
            double limit = (onAirID > 12 ? BASIC_SPEED[12] : BASIC_SPEED[onAirID]) * speedMultiply,
                    limitNonSprint = RayUtils.scaleVal(0.19 * speedMultiply, 2);
            limit += ((speedEffect != null) ? (speedEffect.bukkitEffect.getAmplifier() + 1) * 0.2D : 0);
            limit += (collideMinecraftShitMathFactor ? 0.05 : 0);
            limit = RayUtils.scaleVal(limit * (1 + multiply), 2);
            double wrapLimit = !player.movement.isSprinting() ? RayUtils.scaleVal(limit / 1.2, 2) : limit;
            ticksOnGround = 0;
            if (horizontal > limit) {
                antiAbuseProcessor = antiAbuseProcessor + 75;
                if (hackType.getCheck().getBooleanOption("check_simulation_air_limit", true)
                        && antiAbuseProcessor > 200) {
                    flag = "type: simulation-air-limit, speed: " + horizontal
                            + ", speed-limit: " + limit + ", air-tick: " + onAir;
                }
            }
            if (horizontal > wrapLimit && keepSprint.containsKey(player.uuid) && !keepSprint.get(player.uuid).isTimedOut()) {
                antiAbuseProcessor = antiAbuseProcessor + 25;
                if (hackType.getCheck().getBooleanOption("check_keepsprint_silent", true)
                        && antiAbuseProcessor > 200) {
                    flag = "type: keepsprint-silent, speed: " + horizontal
                            + ", speed-limit: " + wrapLimit + ", air-tick: " + onAir;
                }
            }
            if (onAir == 1) {
                int diff = Math.abs(to.getBlockY() - collideMinecraftShitMathFactorValue);
                if (diff > 0) collideMinecraftShitMathFactor = true;
                collideMinecraftShitMathFactorValue = to.getBlockY();
                onGroundMinecraftShitMathFactor = 1.6;
                jumpRayLine = new RayLine(dx, dz);
                jumpRotation = to.getYaw();
                yDiff = 0D;
                keepSprint.put(player.uuid, new KeepSprint(0, false, 0, System.currentTimeMillis()));
                isJump = (vertical >= -0.04);

                // player.sendMessage(String.valueOf(horizontal));
                double diffRayLines = RayUtils.calculateRayLines(a, b);
                double wrappedLimit = limit / 1.6 + ((diffRayLines >= 90) ? 0 : (limit / 2.4) * (1 - diffRayLines / 90));
                if (hackType.getCheck().getBooleanOption("check_simulation_air_direction_basic", true)
                        && !player.trackers.has(Trackers.TrackerType.VEHICLE, "exit")
                        && horizontal > RayUtils.scaleVal(wrappedLimit, 2)
                        && speedModify < 1.0
                        && !ignoreBasicChecks) {
                    flag = "type: simulation-air-direction(basic), speed: " + horizontal
                            + ", speed-limit: " + RayUtils.scaleVal(wrappedLimit, 2)
                            + ", diff: " + RayUtils.scaleVal(diffRayLines, 2);
                }
            } else if (jumpRayLine != null) {
                boolean validPsychic = RayUtils.validRayLines(new RayLine(dx, dz), jumpRayLine, 35),
                        muchThanAllowedStrafe = horizontal > limit / 1.4,
                        muchThanAllowedRotation = horizontal > limit / 1.5;
                if (!player.movement.isSprinting()) {
                    keepSprint.get(player.uuid).sprint();
                }

                if (keepSprint.get(player.uuid).hits() > 0 && !keepSprint.get(player.uuid).sprinted()
                        && onAir > 2 && muchThanAllowedRotation) {
                    keepSprint.get(player.uuid).newFlag();
                    if (keepSprint.get(player.uuid).flags() > 2) {
                        antiAbuseProcessor = antiAbuseProcessor + 210;
                        if (hackType.getCheck().getBooleanOption("check_keepsprint", true)
                                && antiAbuseProcessor > 200) {
                            flag = "type: keepsprint, hits: " + keepSprint.get(player.uuid).hits();
                        }
                    }
                }

                if (!validPsychic && muchThanAllowedStrafe) {
                    antiAbuseProcessor = antiAbuseProcessor + 50;
                    if (hackType.getCheck().getBooleanOption("check_simulation_air_physics", true)
                            && antiAbuseProcessor > 200) {
                        flag = "type: simulation-air-physics, speed: " + horizontal
                                + ", speed-limit: " + RayUtils.scaleVal(limit / 1.4, 2) + ", air-tick: " + onAir;
                    }
                }

                if (horizontal > limit / 1.2 && !RayUtils.validRayLines(a, b, 90)
                        && speedModify < 1.0 && !onDamage) {
                    antiAbuseProcessor = antiAbuseProcessor + 60;
                    if (hackType.getCheck().getBooleanOption("check_simulation_air_direction_blatant", true)
                            && antiAbuseProcessor > 200) {
                        flag = "type: simulation-air-direction(blatant), speed: " + horizontal
                                + ", speed-limit: " + RayUtils.scaleVal(limit / 1.2, 2);
                    }
                }
                if (horizontal >= limitNonSprint && deltaYaw > 4 && horizontal < limitNonSprint * 1.15
                        && !player.movement.isSprinting() && !ignoreBasicChecks
                        && speedModify < 1.0) {
                    double calcDiff = RayUtils.scaleVal(RayUtils.calculateRayLines(a, b), 2);
                    if (calcDiff > 10 && !allowedAngle(calcDiff)) {
                        antiAbuseProcessor = antiAbuseProcessor + 60;
                        if (hackType.getCheck().getBooleanOption("check_simulation_air_direction_profound", true)
                                && antiAbuseProcessor > 200) {
                            flag = "type: simulation-air-direction(profound), speed: " + horizontal
                                    + ", speed-limit: " + limitNonSprint
                                    + ", angle: " + calcDiff;
                        }
                    }
                }

            }
        } else {
            ticksOnGround++;

            if (ticksOnGround == 1
                    && oldWrappedOnFly < 16 && vertical < -0.23 && !solidBlockOnTop
                    && !ignoreBasicChecks && jumpEffect == null) {
                antiAbuseProcessor = antiAbuseProcessor + 75;
                if (hackType.getCheck().getBooleanOption("check_landing_end", true)
                        && antiAbuseProcessor > 200) {
                    flag = "type: landing-end, speed: " + vertical
                            + ", air: " + oldWrappedOnFly;
                }
            }
            collideMinecraftShitMathFactor = false;
            double limit = 0.3D * (speedMultiply * 1.2) * ((speedModify > 0) ? 2 : 1);

            if (keepSprint.containsKey(player.uuid) && !keepSprint.get(player.uuid).isTimedOut() && !player.movement.isSprinting()) {
                sprintVl = sprintVl + 2;

                if (sprintVl > 4) {
                    limit = RayUtils.scaleVal(limit / 1.15, 2);
                }
            }
            if (sprintVl > 0) sprintVl = sprintVl - 1;

            limit = RayUtils.scaleVal(limit * (1 + multiply), 2);

            if (onGroundMinecraftShitMathFactor > 0) {
                onGroundMinecraftShitMathFactor = onGroundMinecraftShitMathFactor / 1.33;
                limit = RayUtils.scaleVal(limit * (1 + onGroundMinecraftShitMathFactor), 2);
                if (onGroundMinecraftShitMathFactor < 0.01) onGroundMinecraftShitMathFactor = 0D;
            }
            if (horizontal > limit) {
                antiAbuseProcessor = antiAbuseProcessor + 110;
                if (hackType.getCheck().getBooleanOption("check_simulation_ground", true)
                        && antiAbuseProcessor > 200) {
                    flag = "type: simulation-ground, speed: " + horizontal
                            + ", speed-limit: " + limit;
                }
            }
            if (horizontal > limit / 1.35
                    && !RayUtils.validRayLines(a, b, 88)
                    && speedModify < 1.0) {
                antiAbuseProcessor = antiAbuseProcessor + 60;
                if (hackType.getCheck().getBooleanOption("check_simulation_ground_direction_basic", true)
                        && antiAbuseProcessor > 200) {
                    flag = "type: simulation-ground-direction(basic), speed: " + horizontal
                            + ", speed-limit: " + RayUtils.scaleVal(limit / 1.35, 2);
                }
            }
            if (horizontal >= limit / 1.7
                    && !player.movement.isSprinting()
                    && speedModify < 1.0) {
                double calcDiff = RayUtils.scaleVal(RayUtils.calculateRayLines(a, b), 2);
                if (calcDiff > 10 && !allowedAngle(calcDiff)) {
                    antiAbuseProcessor = antiAbuseProcessor + 60;
                    if (hackType.getCheck().getBooleanOption("check_simulation_ground_direction_profound", true)
                            && antiAbuseProcessor > 200) {
                        flag = "type: simulation-ground-direction(profound), speed: " + horizontal
                                + ", speed-limit: " + RayUtils.scaleVal(limit / 1.7, 2)
                                + ", angle: " + calcDiff;
                    }
                }
            }
            if (horizontal > limit / 1.1
                    && !RayUtils.validRayLines(a, b, 25)
                    && speedModify < 1.0) {
                antiAbuseProcessor = antiAbuseProcessor + 90;
                if (hackType.getCheck().getBooleanOption("check_simulation_ground_direction_blatant", true)
                        && antiAbuseProcessor > 200) {
                    flag = "type: simulation-ground-direction(blatant), speed: " + horizontal
                            + ", speed-limit: " + RayUtils.scaleVal(limit / 1.35, 2);
                }
            }

            onDamage = false;
            multiply = multiply / 2;
            if (multiply < 0.04) multiply = 0D;
        }
        if (flag == null && antiAbuseProcessor > 0)
            antiAbuseProcessor = antiAbuseProcessor - 8;
        if (flag != null) cancel(flag, horizontal, to);
        ignoreBasicChecks = false;
        needToCheckAura = (multiply < 2);
        oldWrappedOnFly = wrappedOnFly;
        oldYaw = wrappedYaw;
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerStayEvent) {
            if (((PlayerStayEvent) object).isOnGround()) {
                wrappedOnAir = 0;
                wrappedOnFly = 0;
            }
        } else if (object instanceof PlayerVelocityEvent) {
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
                needToCheckAura = false;
                // player.sendMessage(String.valueOf(horizontal));
            }
        } else if (object instanceof EntityDamageByEntityEvent) {
            if (SpartanBukkit.packetsEnabled()) return;
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) object;
            Entity damager = e.getDamager();
            DataProcessor.target.put(e.getEntity().getUniqueId(), damager);
            DataProcessor.target.put(damager.getUniqueId(), e.getEntity());

            if (damager instanceof Player && e.getEntity() instanceof Player) {
                keepSprint.get(damager.getUniqueId()).newHit();
                keepSprint.get(damager.getUniqueId()).setTimedOut(System.currentTimeMillis());
            }

            needToCheckAura = false;
            double multi = 0.4;
            newRayRequest = true;
            wrappedOnAir = 0;
            wrappedOnFly = 0;

            if (damager instanceof TNTPrimed) {
                multi += 5;
            }

            if (damager instanceof Arrow) {
                multi += (((Arrow) damager).getKnockbackStrength() + 1) * 3;
            }
            if (damager instanceof Player) {
                LivingEntity livingEntity = (LivingEntity) damager;
                EntityEquipment equipment = livingEntity.getEquipment();
                ItemStack item = equipment.getItemInHand();
                int enchant = item.getEnchantmentLevel(Enchantment.KNOCKBACK);
                multi += enchant * 1.8;
            }
            multiply = multiply + multi;
        } else if (object instanceof PlayerJoinEvent) {
            SpartanLocation to = player.movement.getLocation();
            wrappedOnAir = 0;
            wrappedOnFly = 0;
            multiply = 0D;
            antiAbuseProcessor = 0;
            jumpRayLine = new RayLine(0, 0);
            jumpRotation = to.getYaw();
            collideMinecraftShitMathFactorValue = to.getBlockY();
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
            auraAnalysis = new LinkedList<>();
            needToCheckAura = false;
            target = null;
            ticksOnGround = 0;
            oldWrappedOnFly = 0;
            oldYaw = 0;
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

    private boolean slimeBlockCause(SpartanPlayer player, SpartanLocation location) {
        boolean slime = false,
                piston = false;
        World world = player.getWorld();

        for (int x = location.getBlockX() - 1; x <= location.getBlockX() + 1; x++) {
            for (int y2 = location.getBlockY() - 1; y2 <= location.getBlockY() + 1; y2++) {
                for (int z = location.getBlockZ() - 1; z <= location.getBlockZ() + 1; z++) {
                    SpartanBlock block = new SpartanLocation(world, null, x, y2, z, 0.0f, 0.0f).getBlock();

                    if (BlockUtils.areSlimeBlocks(block.material)
                            || BlockUtils.arePistons(block.material)) {
                        piston = true;
                    }
                    if (BlockUtils.areSlimeBlocks(block.material)) {
                        slime = true;
                    }
                }
            }
        }
        return piston && slime;
    }

    private double getMaxSpeedAmplifier() {
        return Attributes.getAmount(player, Attributes.GENERIC_MOVEMENT_SPEED);
    }

    private void checkAuraAnalysis(SpartanLocation to) {
        if (auraAnalysis.isEmpty()) return;
        String flag = null;
        float yawDiff = 0,
                yawDiffFromFirst = Math.abs(auraAnalysis.getLast().yaw() - auraAnalysis.getFirst().yaw());
        double rayDiff = 0,
                raySimDiff = 0,
                angle = Math.toDegrees(RayUtils.calculateRayLine(auraAnalysis.get(0).rayLine()));
        float yaw = auraAnalysis.get(0).yaw();
        double raySim = RayUtils.calculateRayLine(auraAnalysis.get(0).simRayLine());
        boolean oldInTarget = false;
        int tooFactRotationCause = 0;

        for (Aura a : auraAnalysis) {
            double angleLocal = Math.toDegrees(RayUtils.calculateRayLine(a.rayLine()));
            float yawLocal = a.yaw();

            rayDiff += Math.abs(angleLocal - angle);
            yawDiff += Math.abs(yawLocal - yaw);
            double raySimDiffAdd = Math.abs(RayUtils.calculateRayLine(a.simRayLine()) - raySim);
            raySimDiff += raySimDiffAdd;
            yaw = yawLocal;
            angle = angleLocal;
            raySim = RayUtils.calculateRayLine(a.simRayLine());

            oldInTarget = a.isLookAtTarget();
        }
        auraAnalysis.clear();

        yawDiff = (float) RayUtils.scaleVal(RayUtils.set360Limit(yawDiff), 2);
        yawDiffFromFirst = (float) RayUtils.scaleVal(yawDiffFromFirst, 2);
        rayDiff = RayUtils.scaleVal(RayUtils.set360Limit((float) rayDiff), 2);
        raySimDiff = RayUtils.scaleVal(Math.toDegrees(raySimDiff), 2);

        // player.sendMessage(raySimDiff + " " + rayDiff + " " + yawDiff + " " + yawDiffFromFirst);

        /*
                if (rayDiff * 6.5 < raySimDiff && rayDiff < 16 && raySimDiff < 75 && rayDiff > 0.5 && yawDiffFromFirst > 9.8) {
            for (int i = 0; i < 5; i++) {
                flag = "type: aura(direction), final-rayDiff: "
                                + rayDiff + ", final-raySimDiff: " + raySimDiff + ", count: " + (i + 1);
                cancel(flag, player.movement.getLocation(), 9);
            }
        }
         */


    }

    private boolean allowedAngle(double angle) {
        double a = angle / 45;
        int wrapGoodMath = (int) Math.floor(a);
        double b = a - wrapGoodMath;
        return (b < 0.3 || b > 0.7);
    }

    @Override
    protected boolean canRun() {
        // Check will no longer run when someone uses server flying
        // Do not change as there is a protection for it via the IrregularMovements check
        // The flying method should still be considered in the code, in case anything ever changes
        return MovementProcessing.canCheck(player, true, true, false, true);
    }

}
