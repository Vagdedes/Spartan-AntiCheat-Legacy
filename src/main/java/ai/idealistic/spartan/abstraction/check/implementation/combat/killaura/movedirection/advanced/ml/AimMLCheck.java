package ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced.ml;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced.HeuristicComponent;
import ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced.ml.modules.v5.RNN1Module;
import ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced.ml.modules.v5.RNN2Module;
import ai.idealistic.spartan.abstraction.event.RotationEvent;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import kireiko.dev.millennium.ml.ClientML;
import kireiko.dev.millennium.ml.FactoryML;
import kireiko.dev.millennium.ml.data.ObjectML;
import kireiko.dev.millennium.ml.data.ResultML;
import kireiko.dev.millennium.ml.data.module.FlagType;
import kireiko.dev.millennium.ml.data.module.ModuleML;
import kireiko.dev.millennium.ml.data.module.ModuleResultML;
import kireiko.dev.millennium.ml.logic.MillenniumScheduler;
import kireiko.dev.millennium.types.Vec2f;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AimMLCheck implements HeuristicComponent {

    private static final double UNUSUAL_VL = 1.0; // 0.25
    private static final double STRANGE_VL = 1.0; // 0.5
    private static final double SUSPECTED_VL = 1.0;
    private static final int LEGACY_MODEL_COUNT = 7;
    private static final int LEGACY_ROTATION_BUFFER_SIZE = 600;
    private static final int RNN_ROTATION_BUFFER_SIZE = 150;
    private static final int ATTACK_TIMEOUT_MS = 3_000;

    private final CheckDetection detection;
    private final PlayerProtocol profile;
    private final List<Vec2f> rawRotations;
    private final List<Vec2f> rnnRotations;
    private long lastSampleTime;
    private double rnn1Buffer;
    private double rnn2Buffer;

    public AimMLCheck(final CheckDetection detection, final PlayerProtocol protocol) {
        this.detection = detection;
        this.profile = protocol;
        this.rawRotations = new CopyOnWriteArrayList<>();
        this.rnnRotations = new CopyOnWriteArrayList<>();
        this.lastSampleTime = 0L;
        this.rnn1Buffer = 0.0;
        this.rnn2Buffer = 0.0;
    }

    @Override
    public void process(final RotationEvent rotationUpdate) {
        long now = System.currentTimeMillis();

        if (this.lastSampleTime > 0L
                && now - this.lastSampleTime > ATTACK_TIMEOUT_MS) {
            this.resetSampleState();
        }
        this.detection.call(
                () -> {
                    if (profile.isCinematic()) {
                        return;
                    }
                    Vec2f delta = rotationUpdate.getDelta();

                    if (delta.getX() == 0 && delta.getY() == 0) {
                        return;
                    }
                    this.lastSampleTime = now;
                    this.rawRotations.add(delta);
                    this.rnnRotations.add(delta);

                    if (this.rnnRotations.size() >= RNN_ROTATION_BUFFER_SIZE) {
                        this.checkRNN();
                    }
                    if (this.rawRotations.size() >= LEGACY_ROTATION_BUFFER_SIZE) {
                        this.checkLegacy();
                    }
                },
                this::resetSampleState
        );
    }

    private synchronized void resetSampleState() {
        this.rawRotations.clear();
        this.rnnRotations.clear();
        this.rnn1Buffer = 0.0;
        this.rnn2Buffer = 0.0;
    }

    private void handleFlag(ModuleResultML finalModuleResult, Set<String> modelsThatFlagged) {
        if (finalModuleResult.getType() == FlagType.NORMAL) {
            return;
        }

        final FlagType type = finalModuleResult.getType();
        double vl;
        switch (type) {
            case UNUSUAL:
                vl = UNUSUAL_VL;
                break;
            case STRANGE:
                vl = STRANGE_VL;
                break;
            case SUSPECTED:
                vl = SUSPECTED_VL;
                break;
            default:
                vl = 0f;
        }

        String info = finalModuleResult.getInfo();
        String reason = "Result: " + type + " " + Arrays.toString(modelsThatFlagged.toArray());
        if (info != null && !info.isEmpty()) {
            reason += " (" + info + ")";
        }
        final String cancelReason = reason;
        detection.call(() -> detection.cancel(vl, "ML (" + cancelReason + ")", profile.getFromLocation()));
    }

    private synchronized void handleRnn1Flag(ModuleResultML moduleResultML) {
        final FlagType type = moduleResultML.getType();
        final float delta = RNN1Module.getBufferDelta(type);

        this.rnn1Buffer = Math.max(0.0, this.rnn1Buffer + delta);
        if (type == FlagType.NORMAL || this.rnn1Buffer < RNN1Module.FLAG_BUFFER) {
            return;
        }

        String reason = "Result: " + type + " [" + RNN1Module.MODULE_NAME + "] [buf: "
                + String.format(Locale.ROOT, "%.2f", this.rnn1Buffer) + "]";
        detection.call(() -> detection.cancel(delta, "ML (" + reason + ")", profile.getFromLocation()));
        if (this.rnn1Buffer > RNN1Module.FLAG_BUFFER) {
            this.rnn1Buffer = 4.0;
        }
    }

    private synchronized void handleRnn2Flag(ModuleResultML moduleResultML) {
        final FlagType type = moduleResultML.getType();
        final float delta = RNN2Module.getBufferDelta(type);

        this.rnn2Buffer = Math.max(0.0, this.rnn2Buffer + delta);
        if (type == FlagType.NORMAL || this.rnn2Buffer < RNN2Module.FLAG_BUFFER) {
            return;
        }

        String reason = "Result: " + type + " [" + RNN2Module.MODULE_NAME + "] [buf: "
                + String.format(Locale.ROOT, "%.2f", this.rnn2Buffer) + "]";
        detection.call(() -> detection.cancel(delta, "ML (" + reason + ")", profile.getFromLocation()));
        this.rnn2Buffer = 2.2;
    }

    private void checkResult(List<ObjectML> objectML, int fromIndex, int toIndex) {
        MillenniumScheduler.run(() -> {
            final Set<String> modelsThatFlagged = new HashSet<>();
            ModuleResultML finalModuleResult = new ModuleResultML(0, FlagType.NORMAL, null);

            for (int i = fromIndex; i < toIndex && i < ClientML.MODEL_LIST.size(); i++) {
                ModuleML moduleML = ClientML.MODEL_LIST.get(i);
                if (FactoryML.getModel(i) == null) {
                    continue;
                }

                final ResultML resultML = FactoryML.getModel(i).checkData(objectML);
                final ModuleResultML moduleResultML = moduleML.getResult(resultML);

                if (RNN1Module.MODULE_NAME.equals(moduleML.getName())) {
                    handleRnn1Flag(moduleResultML);
                    continue;
                }
                if (RNN2Module.MODULE_NAME.equals(moduleML.getName())) {
                    handleRnn2Flag(moduleResultML);
                    continue;
                }

                if (moduleResultML.getType() != FlagType.NORMAL) {
                    modelsThatFlagged.add(moduleML.getName());
                }

                if (finalModuleResult.getInfo() == null) {
                    finalModuleResult = moduleResultML;
                } else {
                    final int finalLevel = finalModuleResult.getType().getLevel();
                    final int tempLevel = moduleResultML.getType().getLevel();
                    final boolean isLevelSmaller = finalLevel < tempLevel;
                    final boolean isPrioritySmaller = finalLevel == tempLevel && finalModuleResult.getPriority() < moduleResultML.getPriority();
                    if (isLevelSmaller || isPrioritySmaller) {
                        finalModuleResult = moduleResultML;
                    }
                }
            }

            //profile.bukkit().sendMessage("check result debug: " + finalModuleResult.getInfo());
            handleFlag(finalModuleResult, modelsThatFlagged);
        });
    }

    private List<ObjectML> createObjectStack(List<Vec2f> rotations) {
        final List<ObjectML> objectMLStack = new ArrayList<>();

        ObjectML yaw = new ObjectML(new ArrayList<>());
        ObjectML pitch = new ObjectML(new ArrayList<>());
        for (Vec2f rot : rotations) {
            yaw.getValues().add((double) rot.getX());
            pitch.getValues().add((double) rot.getY());
        }
        objectMLStack.add(yaw);
        objectMLStack.add(pitch);
        return objectMLStack;
    }

    private synchronized void checkLegacy() {
        checkResult(createObjectStack(this.rawRotations), 0, LEGACY_MODEL_COUNT);
        this.rawRotations.clear();
    }

    private synchronized void checkRNN() {
        checkResult(createObjectStack(this.rnnRotations), LEGACY_MODEL_COUNT, ClientML.MODEL_LIST.size());
        this.rnnRotations.clear();
    }
}
