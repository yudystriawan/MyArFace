package com.example.myarface.record;

import android.content.res.Configuration;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.example.myarface.ar.AugmentedFacesActivity;
import com.google.ar.sceneform.SceneView;

import java.io.File;
import java.io.IOException;

public class VideoRecorder {

    private static final String TAG = AugmentedFacesActivity.class.getSimpleName();
    private static final int DEFAULT_BITRATE = 10000000;
    private static final int DEFAULT_FRAMERATE = 30;

    private boolean recordingVideoFlag;

    private MediaRecorder mediaRecorder;
    private Size videoSize;

    private SceneView sceneView;
    private int videoCodec;
    private int audioCodec;
    private File videoDirectory;
    private String videoBaseName;
    private File videoPath;
    private Surface encoderSurface;

    private int width, height;

    private int bitRate = DEFAULT_BITRATE;
    private int frameRate = DEFAULT_FRAMERATE;

    private static final int[] FALLBACK_QUALITY_LEVELS = {
            CamcorderProfile.QUALITY_HIGH,
            CamcorderProfile.QUALITY_2160P,
            CamcorderProfile.QUALITY_1080P,
            CamcorderProfile.QUALITY_720P,
            CamcorderProfile.QUALITY_480P
    };

    public VideoRecorder() {
        recordingVideoFlag = false;
    }

    public File getVideoPath() {
        return videoPath;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public void setSceneView(SceneView sceneView) {
        this.sceneView = sceneView;
    }


    public void setVideoSize(int width, int height) {
        videoSize = new Size(width, height);
    }

    public void setVideoCodec(int videoCodec) {
        this.videoCodec = videoCodec;
    }

    public void setAudioCodec(int audioCodec) {
        this.audioCodec = audioCodec;
    }


    public boolean isRecording() {
        return recordingVideoFlag;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setVideoQuality(int quality, int orientation) {
        CamcorderProfile profile = null;

        if (CamcorderProfile.hasProfile(quality)) {
            profile = CamcorderProfile.get(quality);
        }

        if (profile == null) {

            for (int level : FALLBACK_QUALITY_LEVELS) {
                if (CamcorderProfile.hasProfile(level)) {
                    profile = CamcorderProfile.get(level);
                    break;
                }
            }

        }

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        } else {
            setVideoSize(profile.videoFrameHeight, profile.videoFrameWidth);
        }

        this.width = profile.videoFrameWidth;
        this.height = profile.videoFrameHeight;

        setVideoCodec(profile.videoCodec);
        setAudioCodec(profile.audioCodec);
        setBitRate(profile.videoBitRate);
        setFrameRate(profile.videoFrameRate);

    }

    public boolean onToggleRecord() {
        if (recordingVideoFlag) {
            stopRecordingVideo();
        } else {
            startRecordingVideo();
        }

        return recordingVideoFlag;
    }

    private void startRecordingVideo() {

        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }

        try {
            buildFilename();
            setUpMediaRecorder();
        } catch (IOException e) {
            Log.e(TAG, "Exception setting up recorder", e);
            return;
        }

        encoderSurface = mediaRecorder.getSurface();

        sceneView.startMirroringToSurface(
                encoderSurface,
                0,
                0,
                videoSize.getWidth(),
                videoSize.getHeight()
        );

        recordingVideoFlag = true;
    }

    private void setUpMediaRecorder() throws IOException {
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        mediaRecorder.setOutputFile(videoPath.getAbsolutePath());
        mediaRecorder.setVideoEncodingBitRate(bitRate);
        mediaRecorder.setVideoFrameRate(frameRate);
        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        mediaRecorder.setVideoEncoder(videoCodec);
        mediaRecorder.setAudioEncoder(audioCodec);

        mediaRecorder.prepare();

        try {
            mediaRecorder.start();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Exception starting capture: " + e.getMessage(), e);
        }
    }

    private void buildFilename() {
        if (videoDirectory == null) {
            videoDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/Sceneform");
        }

        if (videoBaseName == null || videoBaseName.isEmpty()) {
            videoBaseName = "Sample";
        }

        videoPath = new File(videoDirectory, videoBaseName + Long.toHexString(System.currentTimeMillis()) + ".mp4");
        File dir = videoPath.getParentFile();

        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    private void stopRecordingVideo() {
        recordingVideoFlag = false;

        if (encoderSurface != null) {
            sceneView.stopMirroringToSurface(encoderSurface);
            encoderSurface = null;
        }

        //Stop record
        mediaRecorder.stop();
        mediaRecorder.reset();
    }

}
