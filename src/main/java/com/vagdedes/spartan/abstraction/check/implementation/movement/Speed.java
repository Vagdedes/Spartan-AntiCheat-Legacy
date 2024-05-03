package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.implementation.movement.speed.SprintSpeed;
import com.vagdedes.spartan.abstraction.check.implementation.movement.speed.WaterSpeed;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import me.vagdedes.spartan.system.Enums.HackType;

import java.util.HashSet;
import java.util.Set;

public class Speed extends CheckExecutor {

    public static final HackType check = HackType.Speed;
    private final SprintSpeed sprintDetection;
    private final WaterSpeed waterDetection;

    public Speed(SpartanPlayer player) {
        super(check, player, 2);
        this.addDetections(new DetectionExecutor[]{
                new SprintSpeed(this),
                new WaterSpeed(this)
        });
        sprintDetection = (SprintSpeed) detections[0];
        waterDetection = (WaterSpeed) detections[1];
    }

    public static final byte
            SPRINT = 0,
            WATER = 1;
    private static final double walking = 0.292;
    public static double ground = 0.325;
    private static final double incompatibility = 0.75;
    private static final double ground_block_speed = 0.46;
    private static final double ground_block_speed_extended = 0.57;
    private static final double ground_slime = 0.31;
    private static final double air_slime = 0.47;
    private static final double ground_jump_speed = 0.525;
    private static double air = 0.3675;
    private static final double air_decreased = 0.366;
    private static final double air_non_solid_min = 0.42;
    private static final double air_non_solid_max = 1.0;
    private static final double blocks = 0.7;
    private static final double blocks_slime = 0.85;
    private static final double blocks_ice = 1.55;
    public static final double ypos = 0.68;
    private static final double ypos_advanced = 0.71;
    private static final double upwards = 0.78;
    private static final double sneaking = 0.16;
    private static final double cobweb = 0.1;
    private static final double interactiveBush = 0.15;
    static final double ice = 0.69; // 0.59 previously
    private static final double selfHit = 1.6;
    private static final double soul_sand_ground = 0.2;
    private static final double solid = 1.5;
    private static final double bed = 0.62;
    private static final double dmg = 2.15;
    private static final double trident = 4.0;
    private static final double bow = 3.0;
    static final double piston = 1.6;
    private static final double pushExtreme = 6.0;
    private static final double pushFair = (pushExtreme / 4.0);
    private static final Set<Float> fallingDistance = new HashSet<>(6);

    static {
        float f = (float) PlayerUtils.airAcceleration;
        fallingDistance.add(0.0f);
        fallingDistance.add(f);
        fallingDistance.add(f * 3);
        fallingDistance.add(f * 6);
        fallingDistance.add(f * 10);
        fallingDistance.add(f * 14.5f);

        /*
        fallingDistance.add(0.0784f);
        fallingDistance.add(0.075444065f);

        fallingDistance.add(0.22777925f);
        fallingDistance.add(0.233632f);

        fallingDistance.add(0.45546773f);
        fallingDistance.add(0.46415937f);

        fallingDistance.add(0.7570025f);
        fallingDistance.add(0.7684762f);

        fallingDistance.add(1.1309065f);
        fallingDistance.add(1.1451067f);
        */
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        if (canDo()) {
            // todo
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true);
    }
}
