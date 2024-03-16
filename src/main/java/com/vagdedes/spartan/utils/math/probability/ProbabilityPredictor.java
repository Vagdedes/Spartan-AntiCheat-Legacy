package com.vagdedes.spartan.utils.math.probability;

import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternValue;
import com.vagdedes.spartan.utils.java.HashHelper;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class ProbabilityPredictor {

    public final ProbabilityPie pie;
    public final double patternSignificance;
    public final int generalization, count;
    public final Collection<PatternValue> currentPattern;

    public ProbabilityPredictor(Collection<Collection<PatternValue>> totalPatterns,
                                Collection<PatternValue> currentPattern,
                                int generalization) {
        this.generalization = generalization;
        this.count = currentPattern.size();
        this.currentPattern = currentPattern;

        if (!totalPatterns.isEmpty() && this.count > 0) {
            long currentPatternHash = HashHelper.collection(currentPattern);
            int totalPatternSize = 0;
            ProbabilityPie pie = new ProbabilityPie();

            for (Collection<PatternValue> allPatterns : totalPatterns) {
                int allPatternsSize = allPatterns.size();

                if (this.count <= allPatternsSize) {
                    totalPatternSize += allPatternsSize;
                    boolean next = false;
                    Iterator<PatternValue> iterator = allPatterns.iterator();
                    LinkedList<PatternValue> list = new LinkedList<>();

                    for (int i = 0; i < this.count; i++) {
                        list.add(iterator.next());
                    }

                    while (iterator.hasNext()) {
                        PatternValue pattern = iterator.next();
                        list.add(pattern);
                        list.removeFirst();

                        if (next) {
                            next = false;
                            pie.add(pattern.pattern);
                        }
                        if (currentPatternHash == HashHelper.collection(list)) {
                            next = true;
                        }
                    }
                }
            }
            if (totalPatternSize > 0) {
                this.pie = pie;
                this.patternSignificance = (this.pie.getTotal() * this.generalization) / (double) totalPatternSize;
            } else {
                this.pie = null;
                this.patternSignificance = 0.0;
            }
        } else {
            this.pie = null;
            this.patternSignificance = 0.0;
        }
    }
}