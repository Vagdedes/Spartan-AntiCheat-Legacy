package ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced.ml;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced.HeuristicComponent;
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
import kireiko.dev.millennium.ml.logic.ModelML;
import kireiko.dev.millennium.types.Pair;
import kireiko.dev.millennium.types.Vec2f;

import java.util.*;

public final class AimMLCheck implements HeuristicComponent {

    private static final boolean TEST_MODE = false;
    private static final int STATUS = 0;
    private static final Pair<Integer, String> SAVE_DATA = new Pair<>(0, "plugins/Spartan/models/m_huge1.dat");

    private static final double UNUSUAL_VL = 1.0; // 0.25
    private static final double STRANGE_VL = 1.0; // 0.5
    private static final double SUSPECTED_VL = 1.0;
    private static final int ROTATION_BUFFER_SIZE = 600;
    private static final int ATTACK_TIMEOUT_MS = 2500;

    private final CheckDetection detection;
    private final PlayerProtocol profile;
    private final List<Vec2f> rawRotations;

    public AimMLCheck(final CheckDetection detection, final PlayerProtocol protocol) {
        this.detection = detection;
        this.profile = protocol;
        this.rawRotations = new ArrayList<>();
    }

    @Override
    public void process(final RotationEvent rotationUpdate) {
        if (profile.isCinematic()) return;
        if (rotationUpdate.getAbsDelta().getY() == 0 && rotationUpdate.getAbsDelta().getY() == 0) return;
        rawRotations.add(new Vec2f(rotationUpdate.getAbsDelta().getX(), rotationUpdate.getAbsDelta().getY()));
        if (this.rawRotations.size() >= ROTATION_BUFFER_SIZE) {
            this.check();
        }
    }

    private void checkResult(List<ObjectML> objectML) {
        MillenniumScheduler.run(() -> {
            final List<ResultML> results = new ArrayList<>();
            final Set<String> modelsThatFlagged = new HashSet<>();
            ModuleResultML finalModuleResult = new ModuleResultML(0, FlagType.NORMAL, null);

            for (int i = 0; i < ClientML.MODEL_LIST.size(); i++) {
                final ResultML resultML = FactoryML.getModel(i).checkData(objectML);
                ModuleML moduleML = ClientML.MODEL_LIST.get(i);
                final ModuleResultML moduleResultML = moduleML.getResult(resultML);

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
                results.add(resultML);
            }

            //profile.bukkit().sendMessage("t; " + finalModuleResult.getType());

            if (finalModuleResult.getType() != FlagType.NORMAL) {
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
                String reason = "Result: " + type + " " + Arrays.toString(modelsThatFlagged.toArray());
                detection.call(() -> detection.cancel(vl, "ML (" + reason + ")", profile.getFromLocation()));
            }
        });
    }

    private void check() {
        final List<ObjectML> objectMLStack = new ArrayList<>();

        ObjectML yaw = new ObjectML(new ArrayList<>());
        ObjectML pitch = new ObjectML(new ArrayList<>());
        for (Vec2f rot : this.rawRotations) {
            yaw.getValues().add((double) rot.getX());
            pitch.getValues().add((double) rot.getY());
        }
        objectMLStack.add(yaw);
        objectMLStack.add(pitch);

        if (STATUS == 0) {
            checkResult(objectMLStack);
        } else {
            ModelML modelMLLearn = (ModelML) FactoryML.getModel(SAVE_DATA.getX());
            for (int r = 0; r < 1; r++) {
                modelMLLearn.learnByData(objectMLStack, (STATUS == 2));
            }
            modelMLLearn.saveToFile(SAVE_DATA.getY());
        }
        this.rawRotations.clear();
    }
}