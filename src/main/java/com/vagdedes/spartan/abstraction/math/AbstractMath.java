package com.vagdedes.spartan.abstraction.math;

public interface AbstractMath {

    void clear();

    Integer removeLeastSignificant();

    boolean isEmpty();

    int getParts();

    int getTotal();

    int getCount(Number number);

    // Separator

    double getGeneralContribution();

    double getUniqueContribution();

    double getGeneralContribution(Number ignoreNumber);

    double getRatio(Number number, double defaultValue);

    double getDistance(Number number, double defaultValue);

    double getSlopeProbability(Number number, double defaultValue);

    double getCurveProbability(Number number, double defaultValue);

}

