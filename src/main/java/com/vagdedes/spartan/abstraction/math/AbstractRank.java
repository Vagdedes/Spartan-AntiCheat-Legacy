package com.vagdedes.spartan.abstraction.math;

public interface AbstractRank {

    void clear();

    Float removeLeastSignificant();

    double getPosition(Number number, double defaultValue);

    double getPosition(Number number);

    double getRatio(Number number, double defaultValue);

    double getRatio(Number number);

    int getCount(Number number);

    int getCount(Number number, int defaultValue);

    boolean isEmpty();

    int getParts();

    int getTotal();

}
