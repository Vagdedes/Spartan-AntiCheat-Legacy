package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;

import java.util.*;

public class PlayerEvidence {

    enum EvidenceType {
        Hacker, Suspected, Legitimate;

        private final int count;

        EvidenceType() {
            switch (this.ordinal()) {
                case 0:
                    count = Check.hackerCheckAmount;
                    break;
                case 1:
                    count = 1;
                    break;
                default:
                    count = 0;
                    break;
            }
        }
    }

    // Object

    private final Map<Enums.HackType, String> evidence, establishedEvidence;
    private EvidenceType type;

    PlayerEvidence() {
        this.type = EvidenceType.Legitimate;
        this.evidence = new LinkedHashMap<>(Enums.HackType.values().length);
        this.establishedEvidence = new LinkedHashMap<>(Enums.HackType.values().length);
    }

    PlayerEvidence(PlayerEvidence evidence) {
        this.type = evidence.type;
        this.evidence = new LinkedHashMap<>(evidence.evidence);
        this.establishedEvidence = new LinkedHashMap<>(evidence.establishedEvidence);
    }

    // Separator

    public Collection<Enums.HackType> getKnowledgeList() {
        return new HashSet<>(establishedEvidence.keySet());
    }

    public Set<Map.Entry<Enums.HackType, String>> getKnowledgeEntry() {
        return new HashSet<>(establishedEvidence.entrySet());
    }

    public String getKnowledge(Enums.HackType hackType, String color) {
        String knowledge = establishedEvidence.get(hackType);
        return color == null || knowledge == null ? knowledge : addColor(knowledge, color);
    }

    // Separator

    public String addColor(String knowledge, String color) {
        return knowledge.replace("§r", "§r" + color);
    }

    // Separator

    public int getCount() {
        return establishedEvidence.size();
    }

    public EvidenceType getType() {
        return type;
    }

    // Separator

    public boolean has() {
        return !establishedEvidence.isEmpty();
    }

    boolean has(EvidenceType type) {
        return this.type == type;
    }

    public boolean has(Enums.HackType hackType) {
        return establishedEvidence.containsKey(hackType);
    }

    // Separator

    void add(Enums.HackType hackType, String reason) {
        this.evidence.put(hackType, reason);
    }

    boolean addIfAbsent(Enums.HackType hackType, String reason) {
        return this.evidence.putIfAbsent(hackType, reason) == null;
    }

    public void remove(Enums.HackType hackType) {
        this.evidence.remove(hackType);
    }

    void clear() {
        evidence.clear();
        establishedEvidence.clear();
        this.type = EvidenceType.Legitimate;
    }

    void judge(PlayerEvidence evidence) {
        int count = this.evidence.size();

        if (evidence != null && evidence.has()) {
            for (Enums.HackType hackType : evidence.getKnowledgeList()) {
                if (!this.has(hackType)) {
                    count++;
                }
            }
        }
        this.type = EvidenceType.Legitimate;

        for (EvidenceType evidenceType : EvidenceType.values()) {
            if (count >= evidenceType.count
                    && this.type.count < evidenceType.count) {
                this.type = evidenceType;

                if (evidenceType == EvidenceType.Hacker) {
                    break;
                }
            }
        }
        this.establishedEvidence.clear();
        this.establishedEvidence.putAll(this.evidence);
    }

}
