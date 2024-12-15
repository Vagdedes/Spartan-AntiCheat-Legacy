package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class ProfileContinuity {

    private final PlayerProfile profile;
    private final Map<Check.DataType, Map<Long, Long>> continuity;

    public ProfileContinuity(PlayerProfile profile) {
        this.profile = profile;
        this.continuity = Collections.synchronizedMap(
                new LinkedHashMap<>(Check.DataType.values().length)
        );
    }

    public void setActiveTime(long moment, long length, boolean log) {
        if (log) {
            AntiCheatLogs.rawLogInfo(
                    moment,
                    this.profile.name + PlayerProfile.onlineFor + length,
                    false,
                    true,
                    true
            );
        }
        synchronized (this.continuity) {
            this.continuity.computeIfAbsent(
                    profile.getLastDataType(),
                    k -> new TreeMap<>()
            ).put(moment, length);
        }
    }

    public boolean wasOnline(SpartanProtocol protocol, long current, long previous) {
        if (protocol != null
                && current >= protocol.getActiveCreationTime()
                && previous >= protocol.getActiveCreationTime()) {
            return !protocol.spartan.isAFK();
        } else {
            synchronized (this.continuity) {
                Map<Long, Long> data = this.continuity.get(profile.getLastDataType());

                if (data != null) {
                    for (Map.Entry<Long, Long> entry : data.entrySet()) {
                        long connectedTime = entry.getValue(),
                                disconnectedMoment = entry.getKey();

                        if (previous >= (disconnectedMoment - connectedTime)
                                && current <= disconnectedMoment) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    public long getOnlineTime(SpartanProtocol protocol) {
        synchronized (this.continuity) {
            Map<Long, Long> data = this.continuity.get(this.profile.getLastDataType());

            if (data != null) {
                long sum = 0L;

                for (long value : data.values()) {
                    sum += value;
                }
                return protocol == null || protocol.spartan.isAFK()
                        ? sum
                        : protocol.getActiveTimePlayed() + sum;
            } else {
                return protocol == null || protocol.spartan.isAFK()
                        ? 0L
                        : protocol.getActiveTimePlayed();
            }
        }
    }

}
