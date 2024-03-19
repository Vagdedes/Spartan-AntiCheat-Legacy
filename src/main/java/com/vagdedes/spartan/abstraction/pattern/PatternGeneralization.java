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

    public double[] probabilityRankAndProbability(double value) {
        synchronized (this.probabilityRank) {
            this.probabilityRank.maintain();
            double rank = this.probabilityRank.getProbability(value, -1.0);

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
            this.significanceRank.maintain();
            double rank = this.significanceRank.getProbability(value, -1.0);

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
            this.contributionRank.maintain();
            double rank = this.contributionRank.getProbability(value, -1.0);

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
