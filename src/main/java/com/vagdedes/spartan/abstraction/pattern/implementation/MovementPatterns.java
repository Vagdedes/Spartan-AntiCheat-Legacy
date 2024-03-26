package com.vagdedes.spartan.abstraction.pattern.implementation;

import com.vagdedes.spartan.abstraction.pattern.PatternFamily;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.Pattern;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternGeneralizationInitiator;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;

public class MovementPatterns extends PatternFamily {

    public Pattern horizontal, vertical, all, fall;

    public MovementPatterns() {
        super(1_000L, 4);
        this.addPatterns(
                new Pattern[]{
                        this.horizontal = new Pattern(
                                "movement/distance/horizontal",
                                0.05,
                                new PatternGeneralizationInitiator[]{
                                        new PatternGeneralizationInitiator(GroundUtils.maxHeightLength, 0.05)
                                }
                        ),
                        this.vertical = new Pattern(
                                "movement/distance/vertical",
                                0.05,
                                new PatternGeneralizationInitiator[]{
                                        new PatternGeneralizationInitiator(GroundUtils.maxHeightLength, 0.05)
                                }
                        ),
                        this.all = new Pattern(
                                "movement/distance/all",
                                0.05,
                                new PatternGeneralizationInitiator[]{
                                        new PatternGeneralizationInitiator(GroundUtils.maxHeightLength, 0.05)
                                }
                        ),
                        this.fall = new Pattern(
                                "movement/distance/fall",
                                0.05,
                                new PatternGeneralizationInitiator[]{
                                        new PatternGeneralizationInitiator(GroundUtils.maxHeightLength, 0.05)
                                }
                        )
                }
        );
    }
}
