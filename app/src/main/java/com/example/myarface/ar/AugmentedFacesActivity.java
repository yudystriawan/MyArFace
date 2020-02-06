package com.example.myarface.ar;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.CamcorderProfile;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myarface.R;
import com.example.myarface.adapter.ListFilterAdapter;
import com.example.myarface.model.Filter;
import com.example.myarface.record.VideoRecorder;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.AugmentedFaceNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class AugmentedFacesActivity extends AppCompatActivity {

    private static final String TAG = AugmentedFacesActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    public static String EXTRA_FILTER = "extra_filter";

    private FaceArFragment arFragment;
    private ModelRenderable renderable;
    private VideoRecorder videoRecorder;

    private final HashMap<AugmentedFace, AugmentedFaceNode> faceNodeHashMap = new HashMap<>();

    private ImageView imgButtonRecord;
    private ImageView imgButtonFilter;
    private ImageView testing;
    private RecyclerView rvFilters;
    private ConstraintLayout layoutFilters;

    private float dX, dY;
    private int lastAction;

//    private ExpandableRelativeLayout layoutFilters;

    private boolean isLayoutFilterExpanded = false;

    private ArrayList<Filter> list = new ArrayList<>();

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

        initRecycleView();


    }

    private void initUI() {
        imgButtonRecord = findViewById(R.id.img_button_record);
        imgButtonRecord.setOnClickListener(this::toggleRecording);
        imgButtonRecord.setEnabled(true);
        imgButtonRecord.setImageResource(R.drawable.round_videocam);

        imgButtonFilter = findViewById(R.id.img_button_filter);
        imgButtonFilter.setOnClickListener(this::toggleLayoutFilter);
        imgButtonFilter.setEnabled(true);

        layoutFilters = findViewById(R.id.layout_filters);

        rvFilters = findViewById(R.id.rv_filters);
        rvFilters.setHasFixedSize(true);
    }

    private void initModelRenderable() {

        arFragment = (FaceArFragment) getSupportFragmentManager().findFragmentById(R.id.face_fragment);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            int filterResource = getIntent().getIntExtra(EXTRA_FILTER, 0);

            if (filterResource != 0) {
                loadModelRenderable(this, filterResource);
            }

            ArSceneView arSceneView = arFragment.getArSceneView();
            arSceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
            arSceneView.setMinimumWidth(900);
            arSceneView.setMinimumHeight(900);

            Scene scene = arSceneView.getScene();
            scene.addOnUpdateListener(frameTime -> {
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
            });

            scene.setOnTouchListener((hitTestResult, motionEvent) -> {
                Log.d(TAG, "onTouch: entering...");
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "ACTION_DOWN triggered");
                        dX = arSceneView.getX() - motionEvent.getRawX();
                        dY = arSceneView.getY() - motionEvent.getRawY();
                        lastAction = MotionEvent.ACTION_DOWN;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d(TAG, "ACTION_MOVE triggered");
                        arSceneView.setX(motionEvent.getRawX() + dX);
                        arSceneView.setY(motionEvent.getRawY() + dY);
                        lastAction = MotionEvent.ACTION_MOVE;
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "ACTION_UP triggered");
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            Toast.makeText(AugmentedFacesActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                return true;
            });

        } else {
            Toast.makeText(this, "This Device is not supported", Toast.LENGTH_LONG).show();
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadModelRenderable(Context context, int resource) {
        ModelRenderable.builder()
                .setSource(context, resource)
                .build()
                .thenAccept(modelRenderable -> {
                    renderable = modelRenderable;
                    modelRenderable.setShadowCaster(false);
                    modelRenderable.setShadowReceiver(false);
                });
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
            imgButtonRecord.setImageResource(R.drawable.round_stop);
        } else {
            imgButtonRecord.setImageResource(R.drawable.round_videocam);
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

    private void toggleLayoutFilter(View unusedView) {
        if (layoutFilters.getVisibility() == View.VISIBLE) {
            layoutFilters.setVisibility(View.GONE);
            isLayoutFilterExpanded = false;
        } else {
            layoutFilters.setVisibility(View.VISIBLE);
            isLayoutFilterExpanded = true;
        }
    }

    private void initRecycleView() {
        list.addAll(getListFilters());
        showRecycleList();
    }

    private void showRecycleList() {
        rvFilters.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        ListFilterAdapter filterAdapter = new ListFilterAdapter(list);
        rvFilters.setAdapter(filterAdapter);

        filterAdapter.setOnItemClickCallback(new ListFilterAdapter.OnItemClickCallback() {
            @Override
            public void onItemClicked(Filter data) {
                showSelectedFilter(data);
            }
        });
    }

    private void showSelectedFilter(Filter filter) {
        Toast.makeText(this, "Kamu memilih " + filter.getName(), Toast.LENGTH_SHORT).show();

        getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        Intent intent = getIntent().putExtra(EXTRA_FILTER, filter.getResource());
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);

    }

    private ArrayList<Filter> getListFilters() {
        ArrayList<Filter> arrayList = new ArrayList<>();
        arrayList.add(new Filter(0, "No Filter", null));
//        arrayList.add(new Filter(R.raw.eyeglass2, "eyeglass", null));
//        arrayList.add(new Filter(R.raw.red_nose, "red_nose", null));
//        arrayList.add(new Filter(R.raw.gatto, "gatto", null));
//        arrayList.add(new Filter(R.raw.fox_face, "fox_sample", null));
//        arrayList.add(new Filter(R.raw.horn_left, "horn_left", null));
//        arrayList.add(new Filter(R.raw.hair, "hair_sample", null));
        return arrayList;
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
        if (videoRecorder.isRecording()) {
            toggleRecording(null);
        }
        super.onPause();
    }

}
