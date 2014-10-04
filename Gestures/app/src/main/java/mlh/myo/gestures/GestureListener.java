package mlh.myo.gestures;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;

import java.text.DecimalFormat;

public class GestureListener extends AbstractDeviceListener {
    private GestureLauncher parent;

    private Arm currentArm = Arm.UNKNOWN;
    private XDirection xDirection = XDirection.UNKNOWN;

    private double pitchAdjust = 0;
    private double yawAdjust = 0;
    private double rollAdjust = 0;
    private double pitch = 0;
    private double roll = 0;
    private double yaw = 0;

    private double angleTolerance = 20;

    private long lastPinkyTouch = 0;

    private enum State {
        CALIBRATE,
        USE,
        DIALING
    }
    private State state = State.USE;
    private State lastState = State.USE;

    private enum Quadrant {
        UNKNOWN,
        RIGHT,
        MIDDLE,
        LEFT,
        TOP,
        BOTTOM
    }

    private DecimalFormat formatter = new DecimalFormat("###0.####");

    public GestureListener(GestureLauncher parent) {
        this.parent = parent;
    }

    @Override
    public void onConnect(Myo myo, long timestamp) {
        parent.GetStatusText().setText(R.string.connected);
    }

    @Override
    public void onArmRecognized(Myo myo, long timestamp, Arm arm, XDirection xdir ) {
        this.xDirection = xdir;

//        Log.w("GestureNexus", "" + formatter.format(yaw) + " " + formatter.format(pitch) + " " + formatter.format(roll));
        parent.GetStatusText().setText("Arm Recognized");
    }

    @Override
    public void onArmLost(Myo myo, long timestamp) {
        parent.GetStatusText().setText("Arm Lost!");
    }

    private void switchState(State newState) {
        this.lastState = this.state;
        this.state =  newState;
    }

    @Override
    public void onPose(Myo myo, long timestamp, Pose pose) {
        switch (pose) {
            case WAVE_IN:
                if (this.state == State.CALIBRATE) {
                    this.yawAdjust = this.yaw;
                    this.pitchAdjust = this.pitch;
                    this.rollAdjust = this.roll;
                    parent.GetStatusText().setText("Calibrated");
                    switchState(State.USE);
                }
                break;
            case FIST:
                if (this.state == State.USE) {
                    if(this.isVertical()) {
                        switchState(State.CALIBRATE);
                        parent.GetStatusText().setText("Calibrating");
                        break;
                    }
                    parent.GetStatusText().setText("FIST FOUND");
//                  startCall("07930562453");
                }
                if(this.state == State.DIALING) {
                    parent.addDigit(this.calcNumber());
                }
                break;
            case THUMB_TO_PINKY:
                if (System.currentTimeMillis() < lastPinkyTouch + 1000) {
                    if(this.state == State.USE) {
                        switchState(State.DIALING);
                        parent.GetStatusText().setText("DIALING");
                    } else if(this.state == State.DIALING) {
                        parent.startCall();
                        //switchState(State.USE);
                        parent.resetNumber();
                        parent.GetStatusText().setText("CANCELLED DIAL");
                    }
                }
                this.lastPinkyTouch = System.currentTimeMillis();
                break;
            case FINGERS_SPREAD:
                if(this.state == State.DIALING) {
                    parent.startCall();
                }
                break;
            default:
                break;
        }
        this.debugPose(pose);
    }

    private void debugPose(Pose pose) {
        switch (pose) {
            case UNKNOWN:
                parent.GetDebugText().setText(R.string.hello_world);
                break;
            case REST:
                parent.GetDebugText().setText("REST");
                break;
            case FIST:
                parent.GetDebugText().setText("Fist");
                break;
            case WAVE_IN:
                parent.GetDebugText().setText("Wave in");
                break;
            case WAVE_OUT:
                parent.GetDebugText().setText("Wave out");
                break;
            case FINGERS_SPREAD:
                parent.GetDebugText().setText("Fingers spread");
                break;
            case THUMB_TO_PINKY:
                parent.GetDebugText().setText("Thumb to pinky");
                break;
        }
    }

    @Override
    public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
        this.yaw = Math.toDegrees(Quaternion.yaw(rotation));
        this.pitch = Math.toDegrees(Quaternion.pitch(rotation));
        this.roll = Math.toDegrees(Quaternion.roll(rotation));

        if (xDirection == XDirection.TOWARD_ELBOW) {
            this.roll *= -1;
            this.pitch *= -1;
        }

        if(this.state == State.USE) {
        //    Log.w("GestureNexus", "" + formatter.format(this.getYaw()) + " " + formatter.format(this.getPitch()) + " " + formatter.format(this.getRoll()));
        }
    }

    /*@Override
    public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
        if (accel.x() < 0.5) return;
        //Log.w("GestureNexus", "" + formatter.format(accel.x()) + " " + formatter.format(accel.y()) + " " + formatter.format(accel.z()));
        //Log.w("GestureNexus", "" + formatter.format(this.getYaw()) + " " + formatter.format(this.getPitch()) + " " + formatter.format(this.getRoll()));
        int number = this.calcNumber();
    }*/

    private int calcNumber() {
        Quadrant horizontalQuad = Quadrant.UNKNOWN;
        Quadrant verticalQuad = Quadrant.UNKNOWN;
        double yaw = this.getYaw();
        double pitch = this.getPitch();
        if (yaw >= -90 && yaw < -10) {
            horizontalQuad = Quadrant.RIGHT;
        } else if (yaw >= -20 && yaw <= 20) {
            horizontalQuad = Quadrant.MIDDLE;
        } else if (yaw > 10 && yaw <= 90) {
            horizontalQuad = Quadrant.LEFT;
        }

        if (pitch >= -90 && pitch < -10) {
            verticalQuad = Quadrant.TOP;
        } else if (pitch >= -20 && pitch <= 20) {
            verticalQuad = Quadrant.MIDDLE;
        } else if (pitch <= 90 && pitch > 10) {
            verticalQuad = Quadrant.BOTTOM;
        }

        if (this.InRange(this.getPitch(), 90, 20)) return 0;

        switch (verticalQuad) {
            case TOP:
                switch(horizontalQuad) {
                    case LEFT:
                        return 1;
                    case MIDDLE:
                        return 2;
                    case RIGHT:
                        return 3;
                }
                break;
            case MIDDLE:
                switch(horizontalQuad) {
                    case LEFT:
                        return 4;
                    case MIDDLE:
                        return 5;
                    case RIGHT:
                        return 6;
                }
                break;
            case BOTTOM:
                switch(horizontalQuad) {
                    case LEFT:
                        return 7;
                    case MIDDLE:
                        return 8;
                    case RIGHT:
                        return 9;
                }
                break;
        }

        Log.e("Quads", "H: " + horizontalQuad + "V: " + verticalQuad);
        return 0;
    }

    public double getPitch() {
        return this.clampAngle(this.pitch - this.pitchAdjust);
    }

    public double getRoll() {
        return this.clampAngle(this.roll - this.rollAdjust);
    }

    public double getYaw() {
        return this.clampAngle(this.yaw - this.yawAdjust);
    }

    private boolean isVertical() {
        boolean isVert = this.InRange(this.getPitch(), -90, this.angleTolerance);
        Log.w("GVert", isVert + "");
        return isVert;
    }

    private double clampAngle(double angle) {
        if(angle < -180) {
            angle = 360 + angle;
        }
        if(angle > 180) {
            angle = -360 + angle;
        }
        return angle;
    }

    private boolean InRange(double value, double point, double tolerance) {
        if (tolerance < 0 || tolerance > 180 ) throw new RuntimeException("Tolerance for Inrange should be < 180 and > 0");
        double lowerBound = clampAngle(point - tolerance);
        double upperBound = clampAngle(point + tolerance);
        point = clampAngle(point);
        value = clampAngle(value);
        Log.w("GestureNexus", "" + formatter.format(lowerBound) + " " + formatter.format(value) + " " + formatter.format(upperBound));
        return (value > lowerBound && value < upperBound) || (upperBound < lowerBound && (value < upperBound || value > lowerBound) );
    }
}
