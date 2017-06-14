package com.example.acer.taxiapp.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.acer.taxiapp.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapFragment extends Fragment {

    private MapView mapView;
    private GoogleMap googleMap;
    private LatLng customerLatLng;
    private LatLng currentLatLng;
    private Marker currentLocationMarker;

    // For tracking the route
    private ArrayList<LatLng> points;
    private Polyline line;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        points = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = (MapView) rootView.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();

        MapsInitializer.initialize(getActivity().getApplicationContext());
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap _googleMap) {
                googleMap = _googleMap;

                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                    googleMap.setMyLocationEnabled(true);
//                    googleMap.addMarker(new MarkerOptions().position(new LatLng(42, 21)).title("Test Location"));
//                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(42, 21)).zoom(12).build()));
//                } else {
//                    ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1234);
                }

                // Display current position if it is already known
                if (currentLatLng != null) {
                    currentLocationMarker = googleMap.addMarker(new MarkerOptions().position(currentLatLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(currentLatLng).zoom(14).build()));
                }

                // Display position of the customer if there is one waiting for the driver
                if(customerLatLng != null) {
                    googleMap.addMarker(new MarkerOptions().position(customerLatLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(customerLatLng).zoom(14).build()));
                    // Redraw the polyline if there was a polyline already
                    if(points.size() > 0) {
                        if (line != null) {
                            line.remove();
                        }
                        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE);
                        points.add(currentLatLng);
                        for (LatLng latLng : points) {
                            options.add(latLng);
                        }
                        line = googleMap.addPolyline(options);
                    }
                }


            }
        });
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e("LIFECYCLE", "On destroy view");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("C", "On destroy");
    }

    public void initLocation(float lat, float lng) {
        currentLatLng = new LatLng(lat, lng);
    }

    public void updateLocation(float lat, float lng) {
//        googleMap.clear();
        currentLatLng = new LatLng(lat, lng);
        if(currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        // Draw line if driver is traveling towards customer
        if(customerLatLng != null) {
            if (line != null) {
                line.remove();
            }
            PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE);
            points.add(currentLatLng);
            for (LatLng latLng : points) {
                options.add(latLng);
            }
            // Edge case where the location is updaafter the fragmted ent is drawn,
            // But before the map is loaded
            if(googleMap != null) {
                line = googleMap.addPolyline(options);
            }
        }

        // Edge case where the location is updaafter the fragmted ent is drawn,
        // But before the map is loaded
        if(googleMap != null) {
            currentLocationMarker = googleMap.addMarker(new MarkerOptions().position(currentLatLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(currentLatLng).zoom(14).build()));
        }
    }

    public void setCustomerLatLng(float lat, float lng) {
        customerLatLng = new LatLng(lat, lng);
    }
}
