package com.vagdedes.spartan.abstraction.pattern.implementation;

import com.vagdedes.spartan.abstraction.pattern.Pattern;
import com.vagdedes.spartan.abstraction.pattern.PatternFamily;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;

public class MovementPatterns extends PatternFamily {


    public final Pattern horizontal, vertical, fall;

    public MovementPatterns() {
        super(1_000L, 3);
        this.addPatterns(
                new Pattern[]{
                        this.vertical = new Pattern(
                                "movement/distance/vertical",
                                new short[]{
                                        (short) MovementProcessing.motionLearningLength
                                },
                                true
                        ),
                        this.horizontal = new Pattern(
                                "movement/distance/horizontal",
                                new short[]{
                                        (short) MovementProcessing.motionLearningLength
                                },
                                true
                        ),
                        this.fall = new Pattern(
                                "movement/distance/fall",
                                new short[]{
                                        0
                                },
                                true
                        )
                }
        );
    }

    public void learn(SpartanPlayer player,
                      int situation,
                      double hor, double ver, double fall) {
        this.horizontal.learn(
                player,
                situation,
                hor
        );
        this.vertical.learn(
                player,
                situation,
                ver
        );
        this.fall.learn(
                player,
                situation,
                fall
        );
        this.store();
    }
}
