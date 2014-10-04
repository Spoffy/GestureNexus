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

    private enum State {
        CALIBRATE,
        USE
    }
    private State state = State.USE;

    private DecimalFormat formatter = new DecimalFormat("###0");

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

    @Override
    public void onPose(Myo myo, long timestamp, Pose pose) {
        switch (pose) {
            case WAVE_IN:
                if (this.state == State.CALIBRATE) {
                    this.yawAdjust = this.yaw;
                    this.pitchAdjust = this.pitch;
                    this.rollAdjust = this.roll;
                    parent.GetStatusText().setText("Calibrated");
                }
            case FIST:
                if (this.state == State.USE) {
                    if(this.isVertical()) {
                        this.state = State.CALIBRATE;
                        parent.GetStatusText().setText("Calibrating");
                        break;
                    }
                    parent.GetStatusText().setText("FIST FOUND");
//                  startCall("07930562453");
                }
                break;
            default:
                parent.GetStatusText().setText("Other gesture");
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

        //Log.w("GestureNexus", "" + formatter.format(this.getYaw()) + " " + formatter.format(this.getPitch()) + " " + formatter.format(this.getRoll()));
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
        boolean isVert = this.InRange(this.getPitch(), this.angleTolerance);
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

    private boolean InRange(double value, double tolerance) {
        if (tolerance < 0 || tolerance > 180 ) throw new RuntimeException("Tolerance for Inrange should be < 180 and > 0");
        double lowerBound = clampAngle(value - tolerance);
        double upperBound = clampAngle(value + tolerance);
        value = clampAngle(value);
        Log.w("GestureNexus", "" + formatter.format(lowerBound) + " " + formatter.format(value) + " " + formatter.format(upperBound));
        return (value > lowerBound && value < upperBound) || (upperBound < lowerBound && (value < upperBound || value > lowerBound) );
    }

    private void startCall(String number) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:07930562453"));
            parent.startActivity(callIntent);
        } catch (ActivityNotFoundException e) {
            Log.e("helloandroid dialing example", "Call failed", e);
        }
    }
}
