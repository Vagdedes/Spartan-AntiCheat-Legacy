package com.vagdedes.spartan.abstraction.pattern.implementation;

import com.vagdedes.spartan.abstraction.pattern.PatternFamily;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternStorage;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;

public class MovementPatterns extends PatternFamily {

    public PatternStorage horizontal, vertical, distance;

    public MovementPatterns(long storeFrequency) {
        super(storeFrequency, 3);
        this.addPatterns(
                new PatternStorage[]{
                        this.horizontal = new PatternStorage(
                                "movement/distance/horizontal",
                                new int[]{GroundUtils.maxHeightLength}
                        ),
                        this.vertical = new PatternStorage(
                                "movement/distance/vertical",
                                new int[]{GroundUtils.maxHeightLength}
                        ),
                        this.distance = new PatternStorage(
                                "movement/distance/all",
                                new int[]{GroundUtils.maxHeightLength}
                        )
                }
        );
    }
}
