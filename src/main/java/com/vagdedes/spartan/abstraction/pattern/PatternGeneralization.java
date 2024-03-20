package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternComparison;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public abstract class PatternGeneralization {

    public static String path(String key) {
        return Register.plugin.getDataFolder()
                + "/learning/"
                + key;
    }

    public static final String
            profileOption = ".profile",
            patternOption = ".pattern",
            situationOption = ".situation";
    public static final String[] options = {profileOption, patternOption, situationOption};

    public static Number generalize(Number number, int generalization) {
        if (generalization == 0) {
            return number;
        } else if (number instanceof Double) {
            return AlgebraUtils.cut(number.doubleValue(), generalization);
        } else if (number instanceof Float) {
            return AlgebraUtils.cut(number.floatValue(), generalization);
        } else if (number instanceof Long) {
            return AlgebraUtils.roundToNearest(number.longValue(), generalization);
        } else if (number instanceof Integer) {
            return AlgebraUtils.roundToNearest(number.intValue(), generalization);
        } else {
            return AlgebraUtils.roundToNearest(number.shortValue(), generalization);
        }
    }

    // Separator

    protected final String key;
    public final int generalization;
    public final PatternComparison
            probabilityRank,
            significanceRank,
            contributionRank;
    public final PatternComparison[] patternComparisons;


    protected PatternGeneralization(String key, int generalization) {
        this.key = key;
        this.generalization = generalization;
        this.probabilityRank = new PatternComparison("probability");
        this.significanceRank = new PatternComparison("significance");
        this.contributionRank = new PatternComparison("contribution");
        this.patternComparisons = new PatternComparison[]{
                this.probabilityRank,
                this.significanceRank,
                this.contributionRank
        };
    }

    public double[] probabilityRankAndProbability(double value) {
        synchronized (this.probabilityRank) {
            return this.rankAndProbability(this.probabilityRank, value);
        }
    }

    public double[] significanceRankAndProbability(double value) {
        synchronized (this.significanceRank) {
            return this.rankAndProbability(this.significanceRank, value);
        }
    }

    public double[] contributionRankAndProbability(double value) {
        synchronized (this.contributionRank) {
            return this.rankAndProbability(this.contributionRank, value);
        }
    }

    private double[] rankAndProbability(PatternComparison object, double value) {
        if (value == -1.0) {
            return null;
        } else {
            double rank = object.getPosition(value, -1.0);

            if (rank != -1.0) {
                return new double[]{rank, object.getProbability(value)};
            } else {
                return null;
            }
        }
    }

}
