package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.objects.statistics.PatternStorage;
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

    private final UUID uuid;
    private EvidenceType type;
    final Map<Enums.HackType, String> live; // Live object is used for synchronization
    public final Map<Enums.HackType, String> historical; // Live object is used for synchronization

    PlayerEvidence(UUID uuid) {
        this.uuid = uuid;
        this.type = EvidenceType.Legitimate;
        this.live = Collections.synchronizedMap(new LinkedHashMap<>(Enums.HackType.values().length));
        this.historical = new LinkedHashMap<>(Enums.HackType.values().length);
    }

    // Separator

    public EvidenceType getType() {
        return type;
    }

    // Separator

    public void clear(boolean live, boolean historical, boolean judge) {
        synchronized (this.live) {
            if (live) {
                this.live.clear();
            }
            if (historical) {
                this.historical.clear();
            }
            if (judge) {
                judgeLocal();
            }
        }
    }

    public void remove(Enums.HackType hackType, boolean live, boolean historical, boolean judge) {
        synchronized (this.live) {
            if (live) {
                this.live.remove(hackType);
            }
            if (historical) {
                this.historical.remove(hackType);
            }
            if (judge) {
                judgeLocal();
            }
        }
    }

    public void add(Enums.HackType hackType, String info, boolean live, boolean historical, boolean judge) {
        synchronized (this.live) {
            if (live) {
                this.live.put(hackType, info);
            }
            if (historical) {
                this.historical.put(hackType, info);
            }
            if (judge) {
                judgeLocal();
            }
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
                    PatternStorage.block(this.uuid);
                    break;
                }
            }
        }
        PatternStorage.consider(this.uuid);
        this.type = type;
    }

    public void judge() {
        synchronized (this.live) {
            judgeLocal();
        }
    }
}
