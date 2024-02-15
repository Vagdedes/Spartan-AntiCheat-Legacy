package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.functionality.performance.FalsePositiveDetection;
import com.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerViolation {

    private final Enums.HackType hackType;
    private final long time;
    private final String information, detection;
    private final int level, similarityIdentity;
    final int divisionIdentity;
    private final boolean isOption;
    private final Collection<Number> numbers;

    public PlayerViolation(Enums.HackType hackType, long time, String information, int level) {
        this.hackType = hackType;
        this.time = time;
        this.information = information;
        this.level = level;

        InformationAnalysis informationAnalysis = new InformationAnalysis(hackType, information);
        this.detection = informationAnalysis.detection;
        this.numbers = informationAnalysis.getNumbers();
        this.isOption = informationAnalysis.isOption(hackType);
        this.similarityIdentity = FalsePositiveDetection.getSimplifiedNumber(hackType, detection, numbers);
        this.divisionIdentity = (information.hashCode() * SpartanBukkit.hashCodeMultiplier) + level;
    }

    public Enums.HackType getHackType() {
        return hackType;
    }

    public long getTime() {
        return time;
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
