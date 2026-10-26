package com.gamevault.univibe;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class locationHelper {

    private final Activity activity;
    private final FusedLocationProviderClient fusedLocationClient;

    public locationHelper(Activity activity) {

        this.activity = activity;

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(activity);
    }

    public void obtenerUbicacion(LocationCallback callback) {

        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    100
            );

            return;
        }

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(activity, location -> {

            if (location != null) {

                double latitud = location.getLatitude();
                double longitud = location.getLongitude();

                callback.onLocationResult(latitud, longitud);

            } else {

                callback.onLocationError(
                        "No se pudo obtener la ubicación"
                );
            }
        });
    }

    public interface LocationCallback {

        void onLocationResult(
                double latitud,
                double longitud
        );

        void onLocationError(String mensaje);
    }
}