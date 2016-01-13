/*
 * Copyright 2015 Tomas David
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tomasdavid.vehicleroutingproblem.fragments;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.optaplanner.examples.vehiclerouting.domain.Customer;
import org.optaplanner.examples.vehiclerouting.domain.Depot;
import org.optaplanner.examples.vehiclerouting.domain.Vehicle;
import org.optaplanner.examples.vehiclerouting.domain.VehicleRoutingSolution;
import org.optaplanner.examples.vehiclerouting.domain.location.DistanceType;
import org.optaplanner.examples.vehiclerouting.domain.location.Location;
import org.optaplanner.examples.vehiclerouting.persistence.VehicleRoutingImporter;
import org.tomasdavid.vehicleroutingproblem.MainActivity;
import org.tomasdavid.vehicleroutingproblem.R;
import org.tomasdavid.vehicleroutingproblem.VrpKeys;
import org.tomasdavid.vehicleroutingproblem.components.AboutAppDialog;
import org.tomasdavid.vehicleroutingproblem.components.LegendDialog;
import org.tomasdavid.vehicleroutingproblem.components.VrpView;
import org.tomasdavid.vehicleroutingproblem.tasks.ProgressBarTask;
import org.tomasdavid.vehicleroutingproblem.tasks.VrpSolverTask;

import java.io.File;
import java.io.IOException;

/**
 * Vrp fragment for displaying and calculation of vrp problem.
 *
 * @author Tomas David
 */
public class VrpFragment extends Fragment implements OnMapReadyCallback {

    /**
     * Class tag.
     */
    private static final String TAG = "VrpFragment";

    /**
     * Actual vehicle routing solution.
     */
    private VehicleRoutingSolution vrs;

    /**
     * Vrp solver task.
     */
    private VrpSolverTask vrpSolverTask;

    /**
     * Vrp fragment progress bar.
     */
    private ProgressBarTask progressBarTask;

    /**
     * Time limit of calculation.
     */
    private int timeLimitInSeconds;

    /**
     * Algorithm for calculation.
     */
    private String algorithm;
    private MapView vrp_mapview;
    private GoogleMap googleMap;

    /**
     * Default constructor.
     */
    public VrpFragment() {
        super();
        this.vrs = null;
        this.vrpSolverTask = null;
        this.timeLimitInSeconds = 0;
        this.progressBarTask = null;
        this.algorithm = null;
    }

    /**
     * Sets actual vrs.
     * @param vrs Actual vrs.
     */
    public void setVrs(VehicleRoutingSolution vrs) {
        this.vrs = vrs;
        if(DistanceType.ROAD_DISTANCE == vrs.getDistanceType()) {
            showOnMap(vrs);
        }
        else{
            VrpView vrpView = (VrpView) getActivity().findViewById(R.id.vrp_view);
            vrpView.setActualSolution(vrs);
            vrpView.invalidate();
        }
    }

    /**
     * Returns vrp solver task.
     * @return Vrp solver task.
     */
    public VrpSolverTask getVrpSolverTask() {
        return vrpSolverTask;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        // create vrs from input file
        String fileName = getArguments().getString(VrpKeys.VRP_FILE_NAME.name());
        try {
            vrs = (VehicleRoutingSolution)  VehicleRoutingImporter.readSolution(
                    fileName, getActivity().getAssets().open(getActivity().getString(R.string.vrps_dir) + File.separator + fileName));
        } catch (IOException e) {
            Log.e(TAG, "Problem with vrp file.", e);
            Toast.makeText(getActivity(), "File was not found.", Toast.LENGTH_SHORT).show();
            getActivity().onBackPressed();
        }

        // get time limit and algorithm from bundle
        timeLimitInSeconds = getArguments().getInt(VrpKeys.VRP_TIME_LIMIT.name());
        algorithm = getArguments().getString(VrpKeys.VRP_ALGORITHM.name());

        // create solver task and progress bar
        vrpSolverTask = new VrpSolverTask(this, timeLimitInSeconds, algorithm);
        progressBarTask = new ProgressBarTask(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vrp_mapview != null) {
            vrp_mapview.onResume();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vrp, container, false);
        if(DistanceType.ROAD_DISTANCE == vrs.getDistanceType()) {
            view.findViewById(R.id.vrp_view).setVisibility(View.GONE);
            vrp_mapview = (MapView) view.findViewById(R.id.vrp_mapview);
            vrp_mapview.onCreate(savedInstanceState);
            vrp_mapview.getMapAsync(this);
        }
        else{
            view.findViewById(R.id.vrp_mapview).setVisibility(View.GONE);
        }


        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainActivity mainActivity = (MainActivity) getActivity();
        ((VrpView) mainActivity.findViewById(R.id.vrp_view)).setActualSolution(vrs);
        mainActivity.unlockDrawer();

        // adds left menu button for navigation drawer
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((DrawerLayout) getActivity().findViewById(R.id.drawer_layout)).openDrawer(Gravity.LEFT);
            }
        });

        // initialize progress bar
        ProgressBar pb = (ProgressBar)getActivity().findViewById(R.id.progress_bar);
        pb.setMax(timeLimitInSeconds);
        pb.getProgressDrawable().setColorFilter(getResources().getColor(R.color.dark_blue), PorterDuff.Mode.SRC_IN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_vrp, menu);

        // according to solver task status change play/ stop icon
        MenuItem playStopButton = menu.findItem(R.id.action_run);
        if (vrpSolverTask.isRunning()) {
            playStopButton.setIcon(R.drawable.ic_stop_white_24dp);
        } else {
            playStopButton.setIcon(R.drawable.ic_play_arrow_white_24dp);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_run) {
            // according to solver task status change play/ stop icon, start or stop solver
            vrpSolverTask.cancelToast();
            if (vrpSolverTask.isRunning()) {
                vrpSolverTask.stopTask();
                item.setIcon(R.drawable.ic_play_arrow_white_24dp);
            } else {
                item.setIcon(R.drawable.ic_stop_white_24dp);
                if (vrpSolverTask.getStatus() != Status.PENDING) {
                    vrpSolverTask = new VrpSolverTask(this, timeLimitInSeconds, algorithm);
                }
                vrpSolverTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, vrs);
                if (progressBarTask.getStatus() != Status.PENDING) {
                    progressBarTask = new ProgressBarTask(this);
                }
                progressBarTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, timeLimitInSeconds);
            }
            return true;
        } else if (id == R.id.action_about) {
            AboutAppDialog aad = new AboutAppDialog();
            aad.show(getActivity().getSupportFragmentManager(), null);
            return true;
        } else if (id == R.id.action_legend) {
            LegendDialog aad = new LegendDialog();
            aad.show(getActivity().getSupportFragmentManager(), null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (vrp_mapview != null) {
            vrp_mapview.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (vrp_mapview != null) {
            vrp_mapview.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (vrp_mapview != null) {
            vrp_mapview.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (vrp_mapview != null) {
            vrp_mapview.onLowMemory();
        }
    }

    private void showOnMap(VehicleRoutingSolution vrs){

        googleMap.clear();

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for(Location location : vrs.getLocationList()){
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            googleMap.addMarker(new MarkerOptions().position(position).title(location.getName()));
            builder.include(position);
        }

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(builder.build(), 100);
        googleMap.animateCamera(cameraUpdate);

        for (Depot depot : vrs.getDepotList()) {
            googleMap.addMarker(new MarkerOptions().position(new LatLng(depot.getLocation().getLatitude(), depot.getLocation().getLongitude())).title("Depot"));
        }

        int colorIndex = 0;
        Resources res = getContext().getResources();

        for (Vehicle vehicle : vrs.getVehicleList()) {
            Customer vehicleInfoCustomer = null;
            int longestNonDepotDistance = -1;
            for (Customer customer : vrs.getCustomerList()) {
                if (customer.getPreviousStandstill() != null && customer.getVehicle() == vehicle) {
                    Location previousLocation = customer.getPreviousStandstill().getLocation();
                    Location location = customer.getLocation();
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .add(new LatLng(previousLocation.getLatitude(), previousLocation.getLongitude()), new LatLng(location.getLatitude(), location.getLongitude()))
                            .color(res.obtainTypedArray(R.array.vehicle_colors).getColor(colorIndex, 0));
                    googleMap.addPolyline(polylineOptions);
                    int distance = customer.getDistanceFromPreviousStandstill();

                    if (customer.getPreviousStandstill() instanceof Customer) {
                        if (longestNonDepotDistance < distance) {
                            longestNonDepotDistance = distance;
                            vehicleInfoCustomer = customer;
                        }
                    } else if (vehicleInfoCustomer == null) {
                        vehicleInfoCustomer = customer;
                    }

                    // draw route back to depot
                    if (customer.getNextCustomer() == null) {
                        Location vehicleLocation = vehicle.getLocation();
                        googleMap.addPolyline(new PolylineOptions().add(new LatLng(location.getLatitude(), location.getLongitude()), new LatLng(vehicleLocation.getLatitude(), vehicleLocation.getLongitude())));
                    }
                }
            }

            colorIndex = (colorIndex + 1) % res.obtainTypedArray(R.array.vehicle_colors).length();
        }
    }
}
