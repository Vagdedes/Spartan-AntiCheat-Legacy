package com.vagdedes.spartan.abstraction.pattern.implementation;

import com.vagdedes.spartan.abstraction.pattern.Pattern;
import com.vagdedes.spartan.abstraction.pattern.PatternFamily;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;

public class MovementPatterns extends PatternFamily {

    public final Pattern horizontal, vertical, all, fall;

    public MovementPatterns() {
        super(1_000L, 4);
        this.addPatterns(
                new Pattern[]{
                        this.horizontal = new Pattern(
                                "movement/distance/horizontal",
                                new short[]{
                                        (short) GroundUtils.maxHeightLength
                                },
                                true
                        ),
                        this.vertical = new Pattern(
                                "movement/distance/vertical",
                                new short[]{
                                        (short) GroundUtils.maxHeightLength
                                },
                                true
                        ),
                        this.all = new Pattern(
                                "movement/distance/all",
                                new short[]{
                                        (short) GroundUtils.maxHeightLength
                                },
                                true
                        ),
                        this.fall = new Pattern(
                                "movement/distance/fall",
                                new short[]{
                                        (short) GroundUtils.maxHeightLength
                                },
                                true
                        )
                }
        );
    }

    public void learn(SpartanPlayer player,
                      long situation,
                      double dis, double hor, double ver, double fall) {
        this.all.learn(
                player,
                situation,
                dis
        );
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
