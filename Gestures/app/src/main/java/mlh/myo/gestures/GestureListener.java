package mlh.myo.gestures;

import android.widget.TextView;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.XDirection;

public class GestureListener extends AbstractDeviceListener {
    private TextView targetText;

    private Arm currentArm = Arm.UNKNOWN;
    private XDirection xDirection = XDirection.UNKNOWN;

    public GestureListener(TextView targetText) {
        this.targetText = targetText;
    }

    @Override
    public void onConnect(Myo myo, long timestamp) {
        targetText.setText(R.string.connected);
    }

    @Override
    public void onArmRecognized(Myo myo, long timestamp, Arm arm, XDirection xdir ) {
        targetText.setText("Arm Recognized");
    }

    @Override
    public void onArmLost(Myo myo, long timestamp) {
        targetText.setText("Arm Lost!");
    }

    @Override
    public void onPose(Myo myo, long timestamp, Pose pose) {

    }
}
