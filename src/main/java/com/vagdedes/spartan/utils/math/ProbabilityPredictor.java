package com.vagdedes.spartan.utils.math;

import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternGeneralization;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternProfile;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternValue;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.java.HashUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class ProbabilityPredictor {

    public static ProbabilityPredictor build(SpartanPlayer player,
                                             long situation,
                                             PatternGeneralization patternGeneralization,
                                             PatternProfile patternProfile,
                                             int count) {
        return new ProbabilityPredictor(
                patternGeneralization.getAll(player, situation),
                patternGeneralization.getSpecific(patternProfile, situation, count)
        );
    }

    // Separator

    public final ProbabilityPie pie;

    public ProbabilityPredictor(Collection<Collection<PatternValue>> totalPatterns,
                                Collection<PatternValue> currentPattern) {
        int currentPatternSize = currentPattern.size();
        this.pie = new ProbabilityPie();

        if (!totalPatterns.isEmpty() // No general data to compare
                && currentPatternSize > 0) { // No individual data to compare
            long currentPatternHash = HashUtils.collection(currentPattern);

            for (Collection<PatternValue> allPatterns : totalPatterns) {
                int allPatternsSize = allPatterns.size();

                if (currentPatternSize <= allPatternsSize) { // No enough general data to compare
                    boolean next = false;
                    Iterator<PatternValue> iterator = allPatterns.iterator();
                    LinkedList<PatternValue> list = new LinkedList<>();

                    for (int i = 0; i < currentPatternSize; i++) {
                        list.add(iterator.next());
                    }

                    while (iterator.hasNext()) {
                        PatternValue pattern = iterator.next();
                        list.add(pattern);
                        list.removeFirst();

                        if (next) {
                            next = false;
                            this.pie.add(pattern.pattern);
                        }
                        if (currentPatternHash == HashUtils.collection(list)) {
                            next = true;
                        }
                    }
                }
            }
        }
    }
}