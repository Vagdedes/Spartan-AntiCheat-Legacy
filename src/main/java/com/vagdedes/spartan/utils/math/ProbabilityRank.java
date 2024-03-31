package com.vagdedes.spartan.utils.math;

import java.util.*;

public class ProbabilityRank {

    private final List<Double> numbers;
    private boolean sorted;

    public ProbabilityRank() {
        this(-1);
    }

    public ProbabilityRank(int capacity) {
        this.numbers = capacity >= 0 ? new ArrayList<>(capacity) : new ArrayList<>();
        this.sorted = false;
    }

    public ProbabilityRank clear() {
        this.numbers.clear();
        this.sorted = false;
        return this;
    }

    public ProbabilityRank addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.numbers.add(number.doubleValue());
        }
        this.sorted = false;
        return this;
    }

    public ProbabilityRank addMultiple(Map<? extends Number, Integer> map) {
        for (Map.Entry<? extends Number, Integer> entry : map.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                this.numbers.add(entry.getKey().doubleValue());
            }
        }
        this.sorted = false;
        return this;
    }

    public ProbabilityRank add(Number number) {
        this.numbers.add(number.doubleValue());
        this.sorted = false;
        return this;
    }

    public ProbabilityRank remove(Number number) {
        this.numbers.remove(number.doubleValue());
        return this;
    }

    // Separator

    public double getProbability(Number number) {
        if (!this.sorted) {
            Collections.sort(this.numbers);
            this.sorted = true;
        }
        Iterator<Double> iterator = this.numbers.iterator();
        int start = -1, end = 0, position = 0;
        double find = number.doubleValue();

        while (iterator.hasNext()) {
            double next = iterator.next();

            if (next == find) {
                end = position;

                if (start == -1) {
                    start = position;
                }
            } else if (start != -1) {
                break;
            } else if (next > find) {
                return position / (double) this.numbers.size();
            }
            position++;
        }
        return start == -1
                ? 1.0
                : ((start == end ? start : (start + end) / 2.0)) / this.numbers.size();
    }

    // Separator

    public boolean hasData() {
        return !this.numbers.isEmpty();
    }

    public int getTotal() {
        return this.numbers.size();
    }
}
