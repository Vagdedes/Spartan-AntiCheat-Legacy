package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternComparison;

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
    public final PatternComparison
            probabilityRank,
            significanceRank,
            contributionRank;


    protected PatternGeneralization(String key, int generalization) {
        this.key = key;
        this.generalization = generalization;
        this.probabilityRank = new PatternComparison();
        this.significanceRank = new PatternComparison();
        this.contributionRank = new PatternComparison();
    }

    public double[] probabilityRankAndProbability(double value) {
        synchronized (this.probabilityRank) {
            double rank = this.probabilityRank.getPosition(value, -1.0);

            if (rank != -1.0) {
                return new double[]{
                        rank,
                        this.probabilityRank.pie.getProbability(value)
                };
            } else {
                return null;
            }
        }
    }

    public double[] significanceRankAndProbability(double value) {
        synchronized (this.significanceRank) {
            double rank = this.significanceRank.getPosition(value, -1.0);

            if (rank != -1.0) {
                return new double[]{
                        rank,
                        this.significanceRank.pie.getProbability(value)
                };
            } else {
                return null;
            }
        }
    }

    public double[] contributionRankAndProbability(double value) {
        synchronized (this.contributionRank) {
            double rank = this.contributionRank.getPosition(value, -1.0);

            if (rank != -1.0) {
                return new double[]{
                        rank,
                        this.contributionRank.pie.getProbability(value)
                };
            } else {
                return null;
            }
        }
    }

}
