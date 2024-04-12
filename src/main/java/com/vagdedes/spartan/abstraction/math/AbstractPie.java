package com.vagdedes.spartan.abstraction.math;

public interface AbstractPie extends AbstractMath {

    double getContribution(Number number, double defaultValue);

    double getRatio(Number number, double defaultValue);

    double getDistance(Number number, double defaultValue);

}

