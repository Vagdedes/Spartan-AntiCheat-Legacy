package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.java.OverflowList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinearPatternProfile extends PatternProfile {

    final List<Float> patterns;
    final List<Integer> situations;

    LinearPatternProfile(PlayerProfile profile) {
        super(profile);
        this.patterns = Collections.synchronizedList(new ArrayList<>(4));
        this.situations = Collections.synchronizedList(new OverflowList<>(2, (int) TPS.maximum * 2));
    }

}
