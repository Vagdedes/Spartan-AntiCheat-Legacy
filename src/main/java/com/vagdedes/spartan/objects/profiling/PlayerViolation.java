package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.functionality.performance.FalsePositiveDetection;
import com.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerViolation {

    private final PlayerProfile profile;
    private final Enums.HackType hackType;
    private final long time;
    private final String information, date, detection;
    private final int level, similarityIdentity, playerIdentity;
    final int divisionIdentity;
    private final boolean isOption;
    private final Collection<Number> numbers;

    public PlayerViolation(String name, Enums.HackType hackType, long time, String information, int level) {
        this.playerIdentity = name.hashCode();
        this.hackType = hackType;
        this.time = time;
        this.date = new Timestamp(time).toString().substring(0, 10);
        this.information = information;
        this.level = level;

        InformationAnalysis informationAnalysis = new InformationAnalysis(hackType, information);
        this.detection = informationAnalysis.detection;
        this.numbers = informationAnalysis.getNumbers();
        this.isOption = informationAnalysis.isOption(hackType);
        this.similarityIdentity = FalsePositiveDetection.getSimplifiedNumber(hackType, detection, numbers);
        this.divisionIdentity = (information.hashCode() * SpartanBukkit.hashCodeMultiplier) + level;
    }

    public int getPlayerIdentity() {
        return playerIdentity;
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

    public boolean isDetectionEnabled() {
        return !isOption || hackType.getCheck().getBooleanOption(detection, null);
    }

    public String getDetection() {
        return detection;
    }

    public List<Number> getNumbersList() {
        return new ArrayList<>(numbers);
    }

    public int getSimilarityIdentity() {
        return similarityIdentity;
    }
}
