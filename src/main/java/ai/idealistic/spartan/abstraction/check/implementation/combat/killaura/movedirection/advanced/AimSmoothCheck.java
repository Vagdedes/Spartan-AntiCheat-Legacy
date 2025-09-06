package ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.event.RotationEvent;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import kireiko.dev.millennium.math.Statistics;
import kireiko.dev.millennium.types.Vec2f;

import java.util.*;

public final class AimSmoothCheck implements HeuristicComponent {

    private final CheckDetection detection;
    private final PlayerProtocol profile;
    private final List<Double> stack = new ArrayList<>();
    public AimSmoothCheck(final CheckDetection detection, final PlayerProtocol protocol) {
        this.detection = detection;
        this.profile = protocol;
    }

    @Override
    public void process(final RotationEvent event) {
        if (event.getAbsDelta().getY() == 0 && event.getAbsDelta().getY() == 0) return;
        final Vec2f delta = event.getDelta();
        double angle = getAngleInDegrees(delta) % 90;
        { // check logic
            if ((event.getAbsDelta().getY() > 1.5 && event.getAbsDelta().getX() > 0.32) || event.getAbsDelta().getX() > 1.5)
                stack.add(angle);
            if (stack.size() >= 20) {
                {
                    List<Float> jiff = Statistics.getJiffDelta(stack, 1);
                    //profile.getPlayer().sendMessage("d: " + jiff);
                    float prev = 999;
                    float prePrev = 999;
                    for (float f : jiff) {
                        if (f == 0.0 && prev == 0.0 && prePrev == 0) {
                            detection.cancel(
                                            "[Smooth] Invalid smoothing " + Arrays.toString(jiff.toArray()),
                                            this.profile.getFromLocation()
                            );
                            break;
                        }
                        prePrev = prev;
                        prev = f;
                    }
                }
                stack.clear();
            }
        }
    }

    private static double getAngleInDegrees(Vec2f delta) {
        double angleInRadians = Math.atan2(delta.getX(), delta.getY());
        double angleInDegrees = Math.toDegrees(angleInRadians);

        if (angleInDegrees < 0) {
            angleInDegrees += 360;
        }

        return angleInDegrees;
    }
}