package com.example.myarface.ar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.EnumSet;
import java.util.Set;

public class FaceArFragment extends ArFragment {

    @Override
    protected Set<Session.Feature> getSessionFeatures() {
        return EnumSet.of(Session.Feature.FRONT_CAMERA);
    }

    @Override
    protected Config getSessionConfiguration(Session session) {
        Config config = new Config(session);
        config.setAugmentedFaceMode(Config.AugmentedFaceMode.MESH3D);
        ;
        return config;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout frameLayout = (FrameLayout) super.onCreateView(inflater, container, savedInstanceState);

        getPlaneDiscoveryController().hide();
        getPlaneDiscoveryController().setInstructionView(null);

        return frameLayout;

    }

    /**
     * Fragment extends the ArFragment class to include the WRITER_EXTERNAL_STORAGE
     * permission. This adds this permission to the list of permissions presented to the user for
     * granting.
     */
    @Override
    public String[] getAdditionalPermissions() {
        String[] additionPermissions = super.getAdditionalPermissions();

        int permissionLength = additionPermissions != null ? additionPermissions.length : 0;

        String[] permissions = new String[permissionLength + 2];
        permissions[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        permissions[1] = Manifest.permission.RECORD_AUDIO;

        if (permissionLength > 0) {
            System.arraycopy(
                    additionPermissions,
                    0,
                    permissions,
                    1,
                    additionPermissions.length
            );
        }

        return permissions;
    }

    boolean hasWritePermission() {
        return ActivityCompat.checkSelfPermission(
                this.requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
    }

    boolean hasRecordAudioPermission() {
        return ActivityCompat.checkSelfPermission(
                this.requireActivity(),
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Launch Application Setting to grant permissions.
     */
    public void launchPermissionSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", requireActivity().getPackageName(), null));
        requireActivity().startActivity(intent);
    }

}
