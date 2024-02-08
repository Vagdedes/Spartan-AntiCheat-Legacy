package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import me.vagdedes.spartan.system.Enums;

import java.util.*;

public class PlayerEvidence {

    enum EvidenceType {
        Hacker(2),
        Suspected(1),
        Legitimate(0);

        private final int requirement;

        EvidenceType(int requirement) {
            this.requirement = requirement;
        }
    }

    // Object

    private EvidenceType type;
    final Map<Enums.HackType, String> live, historical; // Live object is used for synchronization

    PlayerEvidence() {
        this.type = EvidenceType.Legitimate;
        this.live = Collections.synchronizedMap(new LinkedHashMap<>(Enums.HackType.values().length));
        this.historical = new LinkedHashMap<>(Enums.HackType.values().length);
    }

    // Separator

    public EvidenceType getType() {
        return type;
    }

    // Separator

    void clear() {
        synchronized (this.live) {
            this.live.clear();
            this.historical.clear();
            this.type = EvidenceType.Legitimate;
        }
    }

    // Separator

    public Collection<Enums.HackType> getKnowledgeList() {
        synchronized (this.live) {
            return getRawKnowledgeList();
        }
    }

    private Collection<Enums.HackType> getRawKnowledgeList() {
        Collection<Enums.HackType> set = new HashSet<>(live.keySet());
        set.addAll(historical.keySet());
        return set;
    }

    // Separator

    public String getKnowledge(Enums.HackType hackType, String color) {
        synchronized (this.live) {
            String knowledge = historical.get(hackType);

            if (knowledge == null) {
                knowledge = live.get(hackType);
            }
            return knowledge == null ? knowledge : addColor(knowledge, color);
        }
    }

    private String addColor(String knowledge, String color) {
        return knowledge.replace("§r", "§r" + color);
    }

    // Separator

    boolean has(EvidenceType type) {
        return this.type == type;
    }

    public boolean has(Enums.HackType hackType) {
        synchronized (this.live) {
            return live.containsKey(hackType) || historical.containsKey(hackType);
        }
    }

    // Separator

    void judgeLocal() {
        int count = this.getRawKnowledgeList().size();
        EvidenceType type = this.type;

        for (EvidenceType evidenceType : EvidenceType.values()) {
            if (count >= evidenceType.requirement) {
                type = evidenceType;

                if (evidenceType != EvidenceType.Legitimate) {
                    break;
                }
            }
        }
        this.type = type;
    }

    public void judge() {
        synchronized (this.live) {
            judgeLocal();
        }
    }
}
