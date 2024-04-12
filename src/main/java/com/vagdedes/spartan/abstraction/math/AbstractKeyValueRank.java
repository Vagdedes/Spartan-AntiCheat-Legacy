package com.vagdedes.spartan.abstraction.math;

public interface AbstractKeyValueRank extends AbstractRank, AbstractKeyValuePie {

    double getSpecificPosition(Number number, int hash, double defaultValue);

}
