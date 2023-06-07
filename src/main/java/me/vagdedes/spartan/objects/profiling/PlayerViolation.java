package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.features.performance.FalsePositiveDetection;
import me.vagdedes.spartan.system.Enums;

import java.sql.Timestamp;
import java.util.Objects;

public class PlayerViolation {

    private final Enums.HackType hackType;
    private final long time;
    private final String information, date;
    private final int level, identity, falsePositiveIdentity;
    private final boolean lowTPS, isOption;
    private final String[] detection;
    private final Double[] decimals;
    private final Integer[] integers;

    public PlayerViolation(Enums.HackType hackType, long time, String information, int level, boolean lowTPS) {
        this.hackType = hackType;
        this.time = time;
        this.date = new Timestamp(time).toString().substring(0, 10);
        this.information = information;
        this.level = level;
        this.lowTPS = lowTPS;
        this.identity = Objects.hash(information, level, lowTPS);

        InformationAnalysis informationAnalysis = new InformationAnalysis(hackType, information);
        this.decimals = informationAnalysis.getDecimals();
        this.integers = informationAnalysis.getIntegers();
        this.falsePositiveIdentity = FalsePositiveDetection.getSimplifiedNumber(information, decimals, integers);
        this.detection = new String[]{
                informationAnalysis.getDetection(false),
                informationAnalysis.getDetection(true)
        };
        this.isOption = informationAnalysis.isOption();
    }

    public Enums.HackType getHackType() {
        return hackType;
    }

    public long getTime() {
        return time;
    }

    public String getDate() {
        return date;
    }

    public String getInformation() {
        return information;
    }

    public int getLevel() {
        return level;
    }

    public boolean isLowTPS() {
        return lowTPS;
    }

    // Dynamic

    public boolean isDetectionEnabled() {
        return !isOption || hackType.getCheck().getBooleanOption(detection[0], false);
    }

    public String getDetection(boolean details) {
        return detection[details ? 1 : 0];
    }

    public int getFalsePositiveIdentity() {
        return falsePositiveIdentity;
    }

    public Double[] getDecimals() {
        return decimals;
    }

    public Integer[] getIntegers() {
        return integers;
    }

    // Local

    int getIdentity() {
        return identity;
    }
}
