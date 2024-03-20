package com.vagdedes.spartan.abstraction.pattern.implementation;

import com.vagdedes.spartan.abstraction.pattern.PatternFamily;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.Pattern;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;

public class MovementPatterns extends PatternFamily {

    public Pattern horizontal, vertical, all, fall;

    public MovementPatterns(long storeFrequency) {
        super(storeFrequency, 4);
        this.addPatterns(
                new Pattern[]{
                        this.horizontal = new Pattern(
                                "movement/distance/horizontal",
                                new int[]{GroundUtils.maxHeightLength}
                        ),
                        this.vertical = new Pattern(
                                "movement/distance/vertical",
                                new int[]{GroundUtils.maxHeightLength}
                        ),
                        this.all = new Pattern(
                                "movement/distance/all",
                                new int[]{GroundUtils.maxHeightLength}
                        ),
                        this.fall = new Pattern(
                                "movement/distance/fall",
                                new int[]{}
                        )
                }
        );
    }
}
