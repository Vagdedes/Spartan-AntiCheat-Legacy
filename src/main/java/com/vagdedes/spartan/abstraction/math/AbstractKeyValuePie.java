package com.vagdedes.spartan.abstraction.math;

public interface AbstractKeyValuePie extends AbstractKeyPie {

    int getSpecificTotal();

    double getSpecificContribution(Number number, double defaultValue);

    double getSpecificContribution(Number number, int ignoreHash, double defaultValue);

    double getSpecificRatio(Number number, int hash, double defaultValue);

    double getSpecificDistance(Number number, int hash, double defaultValue);

    double getSpecificWaveProbability(Number number, int hash, boolean absolute, double defaultValue);
}

