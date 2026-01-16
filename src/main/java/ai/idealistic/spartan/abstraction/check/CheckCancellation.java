package ai.idealistic.spartan.abstraction.check;

import ai.idealistic.spartan.compatibility.Compatibility;
import ai.idealistic.spartan.functionality.server.TPS;
import lombok.Getter;

public class CheckCancellation {

    @Getter
    private final String reason, pointer;
    private long expiration;

    CheckCancellation(Compatibility.CompatibilityType compatibilityType) {
        this(compatibilityType.toString(), null, 0);
    }

    CheckCancellation(String reason, String pointer, int ticks) {
        this.reason = reason;
        this.pointer = pointer;
        this.expiration = ticks < 0
                ? -System.currentTimeMillis() - (Math.abs(ticks) * TPS.tickTime)
                : ticks == 0
                ? Long.MAX_VALUE
                : System.currentTimeMillis() + (ticks * TPS.tickTime);
    }

    boolean hasExpired() {
        if (expiration < 0) {
            expiration = 0;
            return false;
        } else {
            return expiration < System.currentTimeMillis();
        }
    }

    boolean pointerMatches(String info) {
        return this.pointer == null || info.contains(this.pointer);
    }
}
