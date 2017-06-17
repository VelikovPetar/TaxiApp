package com.example.acer.taxiapp.fragments;

import android.app.Fragment;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.acer.taxiapp.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapFragment extends Fragment {

    private MapView mapView;
    private GoogleMap googleMap;
    private LatLng customerLatLng;
    private Marker customerLocationMarker;
    private LatLng currentLatLng;
    private Marker currentLocationMarker;
    private Location location;
    private float bearing;

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

                // Zoom the camera such that both current and customer location are visible
                if(currentLatLng != null && customerLatLng != null) {
                    LatLngBounds bounds = new LatLngBounds.Builder()
                            .include(currentLatLng)
                            .include(customerLatLng)
                            .build();
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 25);
                    googleMap.animateCamera(cameraUpdate);
                }

                // Display current position if it is already known
                if (currentLatLng != null) {
                    currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                            .position(currentLatLng)
                            .flat(true)
                            .rotation(bearing)
                            .anchor(0.5f, 0.5f)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow32)));
                    if(customerLatLng == null) {
                        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder()
                                        .target(currentLatLng)
                                        .zoom(16)
                                        .build()));
                    }
                }

                // Display position of the customer if there is one waiting for the driver
                if(customerLatLng != null) {
                    customerLocationMarker = googleMap.addMarker(new MarkerOptions()
                            .position(customerLatLng)
                            .flat(true)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                    // Redraw the polyline if there was a polyline already
                    if(points.size() > 0) {
                        if (line != null) {
                            line.remove();
                        }
                        PolylineOptions options = new PolylineOptions()
                                .width(5)
                                .color(Color.BLUE);
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void initLocation(Location location) {
        this.location = location;
        currentLatLng = new LatLng(this.location.getLatitude(), this.location.getLongitude());
    }

    public void updateLocation(Location location) {
        if(this.location != null) {
            bearing = this.location.bearingTo(location);
        }
        this.location = location;
        currentLatLng = new LatLng(this.location.getLatitude(), this.location.getLongitude());
        if(currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        // Draw line if driver is traveling towards customer
        if(customerLatLng != null) {
            if (line != null) {
                line.remove();
            }
            PolylineOptions options = new PolylineOptions()
                    .width(5)
                    .color(Color.BLUE);
            points.add(currentLatLng);
            for (LatLng latLng : points) {
                options.add(latLng);
            }
            // Edge case where the location is updated after the fragment is drawn,
            // but before the map is loaded
            if(googleMap != null) {
                line = googleMap.addPolyline(options);
            }
        }

        // Edge case where the location is updated after the fragment is drawn,
        // But before the map is loaded
        if(googleMap != null) {
            currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .flat(true)
                    .rotation(bearing)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow32)));
        }
    }

    public void setCustomerLatLng(float lat, float lng) {
        customerLatLng = new LatLng(lat, lng);
    }

    public void clear() {
        if(isResumed()) {
            if (customerLocationMarker != null) {
                customerLocationMarker.remove();
            }
            if (line != null) {
                line.remove();
            }
        }
        customerLocationMarker = null;
        customerLatLng = null;
        line = null;
        points.clear();
    }
}
