package com.vagdedes.spartan.abstraction.math;

public interface AbstractPie {

    void clear();

    Float removeLeastSignificant();

    double getRatio(Number number);

    double getRatio(Number number, double defaultValue);

    int getCount(Number number);

    int getCount(Number number, int defaultValue);

    boolean isEmpty();

    int getSlices();

    int getTotal();
}

