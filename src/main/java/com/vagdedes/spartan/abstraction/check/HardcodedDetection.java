package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;

public abstract class HardcodedDetection extends CheckDetection {

    public HardcodedDetection(CheckRunner executor, Check.DataType forcedDataType, String name, boolean def) {
        super(executor, forcedDataType, name, def);
    }

    public final void setHackingRatio(double ratio) {
        if (this.protocol != null) {
            this.setProbability(
                    this.protocol.spartan.dataType,
                    PlayerEvidence.probabilityToCertainty(ratio)
            );
        }
    }

    @Override
    public final void clearProbability(Check.DataType dataType) {
    }

    @Override
    protected final boolean hasSufficientData(Check.DataType dataType) {
        return this.getProbability(dataType) != PlayerEvidence.emptyProbability;
    }

    @Override
    final double getDataCompletion(Check.DataType dataType) {
        return this.hasSufficientData(dataType) ? 1.0 : 0.0;
    }

}
