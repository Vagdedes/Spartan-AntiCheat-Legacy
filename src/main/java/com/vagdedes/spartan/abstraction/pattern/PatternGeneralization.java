package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.math.implementation.ProbabilityRankWithPie;

public abstract class PatternGeneralization {

    public static String path(String key) {
        return Register.plugin.getDataFolder()
                + "/learning"
                + "/" + key;
    }

    public static final String
            profileOption = ".profile",
            patternOption = ".pattern",
            situationOption = ".situation";
    public static final String[] options = {profileOption, patternOption, situationOption};

    // Separator

    protected final String key;
    public final int generalization;
    public final ProbabilityRankWithPie
            probabilityRank,
            significanceRank,
            contributionRank;


    protected PatternGeneralization(String key, int generalization) {
        this.key = key;
        this.generalization = generalization;
        this.probabilityRank = new ProbabilityRankWithPie();
        this.significanceRank = new ProbabilityRankWithPie();
        this.contributionRank = new ProbabilityRankWithPie();
    }

    public double getProbabilityRank(Number number) {
        synchronized (probabilityRank) {
            return probabilityRank.getProbability(number, -1.0);
        }
    }

    public double getProbabilityProbability(Number number) {
        synchronized (probabilityRank) {
            return probabilityRank.pie.getProbability(number);
        }
    }

    public double getSignificanceRank(Number number) {
        synchronized (significanceRank) {
            return significanceRank.getProbability(number, -1.0);
        }
    }

    public double getSignificanceProbability(Number number) {
        synchronized (significanceRank) {
            return significanceRank.pie.getProbability(number);
        }
    }

    public double getContributionRank(Number number) {
        synchronized (contributionRank) {
            return contributionRank.getProbability(number, -1.0);
        }
    }

    public double getContributionProbability(Number number) {
        synchronized (contributionRank) {
            return contributionRank.pie.getProbability(number);
        }
    }
}
