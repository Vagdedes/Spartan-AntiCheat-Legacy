package com.vagdedes.spartan.utils.math;

import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternGeneralization;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternProfile;
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
                patternGeneralization.getAll(player, situation, count),
                patternGeneralization.getSpecific(patternProfile, situation, count)
        );
    }

    // Separator

    public final ProbabilityPie pie;

    private ProbabilityPredictor(Collection<Collection<Float>> totalPatterns,
                                 Collection<Float> currentPattern) {
        int currentPatternSize = currentPattern.size();
        this.pie = new ProbabilityPie();

        if (!totalPatterns.isEmpty() // No general data to compare
                && currentPatternSize > 0) { // No individual data to compare
            long currentPatternHash = HashUtils.collection(currentPattern);

            for (Collection<Float> allPatterns : totalPatterns) {
                boolean next = false;
                Iterator<Float> iterator = allPatterns.iterator();
                LinkedList<Float> list = new LinkedList<>();

                for (int i = 0; i < currentPatternSize; i++) {
                    list.add(iterator.next());
                }

                while (iterator.hasNext()) {
                    float pattern = iterator.next();
                    list.add(pattern);
                    list.removeFirst();

                    if (next) {
                        next = false;
                        this.pie.add(pattern);
                    }
                    if (currentPatternHash == HashUtils.collection(list)) {
                        next = true;
                    }
                }
            }
        }
    }

}