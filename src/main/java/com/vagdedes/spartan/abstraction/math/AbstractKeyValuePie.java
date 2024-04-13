package com.vagdedes.spartan.abstraction.math;

public interface AbstractKeyValuePie extends AbstractKeyPie {

    double getSpecificContribution(Number number, double defaultValue);

    double getSpecificContribution(Number number, int ignoreHash, double defaultValue);

    double getSpecificRatio(Number number, int hash, double defaultValue);

    double getSpecificDistance(Number number, int hash, double defaultValue);

    double getSpecificWaveProbability(Number number, boolean absolute, double defaultValue);
}

