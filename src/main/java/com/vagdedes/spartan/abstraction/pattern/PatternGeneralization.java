package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternRankWithPie;

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
    public final PatternRankWithPie
            probabilityRank,
            significanceRank,
            contributionRank;


    protected PatternGeneralization(String key, int generalization) {
        this.key = key;
        this.generalization = generalization;
        this.probabilityRank = new PatternRankWithPie();
        this.significanceRank = new PatternRankWithPie();
        this.contributionRank = new PatternRankWithPie();
    }

    public double probabilityRank(double value) {
        synchronized (probabilityRank) {
            return probabilityRank.getProbability(value, -1.0);
        }
    }

    public double probabilityProbability(double value) {
        synchronized (probabilityRank) {
            return probabilityRank.pie.getProbability(value);
        }
    }

    public double significanceRank(double value) {
        synchronized (significanceRank) {
            return significanceRank.getProbability(value, -1.0);
        }
    }

    public double significanceProbability(double value) {
        synchronized (significanceRank) {
            return significanceRank.pie.getProbability(value);
        }
    }

    public double contributionRank(double value) {
        synchronized (contributionRank) {
            return contributionRank.getProbability(value, -1.0);
        }
    }

    public double contributionProbability(double value) {
        synchronized (contributionRank) {
            return contributionRank.pie.getProbability(value);
        }
    }
}
