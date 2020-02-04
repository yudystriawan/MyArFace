package com.example.myarface.ar;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.media.CamcorderProfile;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.myarface.R;
import com.example.myarface.record.VideoRecorder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.AugmentedFaceNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class AugmentedFacesActivity extends AppCompatActivity {

    private static final String TAG = AugmentedFacesActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private FaceArFragment arFragment;
    private ModelRenderable renderable;
    //    private Texture texture;
    private VideoRecorder videoRecorder;

    private final HashMap<AugmentedFace, AugmentedFaceNode> faceNodeHashMap = new HashMap<>();

    private FloatingActionButton recordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isDeviceSupported(this)) {
            return;
        }

        setContentView(R.layout.activity_augmented_face);

        initUI();

        initModelRenderable();

        initVideoRecorder();

    }

    private void initUI() {
        recordButton = findViewById(R.id.record);
        recordButton.setOnClickListener(this::toggleRecording);
        recordButton.setEnabled(true);
        recordButton.setImageResource(R.drawable.round_videocam);
    }

    private void initModelRenderable() {

        arFragment = (FaceArFragment) getSupportFragmentManager().findFragmentById(R.id.face_fragment);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            ModelRenderable.builder()
                    .setSource(this, R.raw.fox_face)
                    .build()
                    .thenAccept(modelRenderable -> {
                        renderable = modelRenderable;
                        modelRenderable.setShadowCaster(false);
                        modelRenderable.setShadowReceiver(false);
                    });


            ArSceneView arSceneView = arFragment.getArSceneView();
            arSceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);

            Scene scene = arSceneView.getScene();
            scene.addOnUpdateListener(
                    (FrameTime frametime) -> {
                        if (renderable == null) {
                            return;
                        }

                        Collection<AugmentedFace> faces =
                                arSceneView.getSession().getAllTrackables(AugmentedFace.class);

                        for (AugmentedFace face : faces) {
                            if (!faceNodeHashMap.containsKey(face)) {
                                AugmentedFaceNode faceNode = new AugmentedFaceNode(face);
                                faceNode.setParent(scene);
                                faceNode.setFaceRegionsRenderable(renderable);
                                faceNodeHashMap.put(face, faceNode);
                            }
                        }

                        Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iterator = faceNodeHashMap.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iterator.next();
                            AugmentedFace face = entry.getKey();

                            if (face.getTrackingState() == TrackingState.STOPPED) {
                                AugmentedFaceNode faceNode = entry.getValue();
                                faceNode.setParent(null);
                                iterator.remove();
                            }
                        }

                    }
            );
        } else {
            Toast.makeText(this, "This Device is not supported", Toast.LENGTH_LONG).show();
        }


    }

    private void initVideoRecorder() {
        int orientation = getResources().getConfiguration().orientation;

        videoRecorder = new VideoRecorder();
        videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_2160P, orientation);
        videoRecorder.setSceneView(arFragment.getArSceneView());

    }

    private void toggleRecording(View unusedView) {
        if (!arFragment.hasWritePermission()) {
            Log.e(TAG, "Video recording requires the WRITE_EXTERNAL_STORAGE permission");
            Toast.makeText(
                    this,
                    "Video recording requires the WRITE_EXTERNAL_STORAGE permission",
                    Toast.LENGTH_LONG)
                    .show();
            arFragment.launchPermissionSettings();
            return;
        }

        if (!arFragment.hasRecordAudioPermission()) {
            Log.e(TAG, "Video recording requires the RECORD_AUDIO permission");
            Toast.makeText(
                    this,
                    "Video recording requires the RECORD_AUDIO permission",
                    Toast.LENGTH_LONG)
                    .show();
            arFragment.launchPermissionSettings();
            return;
        }

        boolean recording = videoRecorder.onToggleRecord();

        if (recording) {
            recordButton.setImageResource(R.drawable.round_stop);
        } else {
            recordButton.setImageResource(R.drawable.round_videocam);
            String videoPath = videoRecorder.getVideoPath().getAbsolutePath();

            Toast.makeText(this, "Video saved: " + videoPath, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Video saved: " + videoPath);

            // Send  notification of updated content.
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.TITLE, "Sceneform Video");
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.DATA, videoPath);
            getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        }
    }

    private static boolean isDeviceSupported(final Activity activity) {

        if (ArCoreApk.getInstance().checkAvailability(activity) == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Log.e(TAG, "Augmented Faces requires ARCore.");
            Toast.makeText(activity, "Augmented Faces requires ARCore.", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }

        String glEsVersionString = ((ActivityManager) Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE)))
                .getDeviceConfigurationInfo()
                .getGlEsVersion();

        double glEsVersion = Double.parseDouble(glEsVersionString);

        if (glEsVersion < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OPENGL ES 3.0 or later.");
            Toast.makeText(activity, "Sceneform requires OPENGL ES 3.0 or later.", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }

        return true;

    }

    @Override
    protected void onPause() {
        if (videoRecorder.isRecording()){
            toggleRecording(null);
        }
        super.onPause();
    }
}
