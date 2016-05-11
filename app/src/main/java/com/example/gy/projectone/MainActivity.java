package com.example.gy.projectone;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Vibrator;
import android.view.*;
import android.support.v4.widget.DrawerLayout;
import android.widget.*;
import java.util.*;
import android.view.View.OnClickListener;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.sails.engine.Beacon;
import com.sails.engine.LocationRegion;
import com.sails.engine.SAILS;
import com.sails.engine.MarkerManager;
import com.sails.engine.PathRoutingManager;
import com.sails.engine.PinMarkerManager;
import com.sails.engine.SAILSMapView;
import com.sails.engine.core.model.GeoPoint;
import com.sails.engine.overlay.Marker;

import org.altbeacon.beacon.*;
import java.util.Collection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,BeaconConsumer {
    static SAILS mSails;
    static SAILSMapView mSailsMapView;
    ImageView lockcenter;
    ActionBar actionBar;
    Vibrator mVibrator;
    Spinner floorList;

    private BeaconManager beaconManager;
    private TextView RssiText,UuidText,MajorText,MinorText;
    private Handler mHandler;
    private int Rssi, Major, Minor;
    private String Uuid;
    public int PreviousMajor = 0,PreviousMinor = 0;
    public int PreviousRssi = -1000;

    private String address = "140.134.226.182";
    private int port = 8765;
    Socket clientSocket = new Socket();
    DataOutputStream outToServer;
    Thread thread;

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        mVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        floorList = (Spinner) findViewById(R.id.spinner);
        lockcenter = (ImageView) findViewById(R.id.lockcenter);

        //ibeacon scan
        thread = new Thread(sendtoServer);
        thread.start();

        mHandler = new Handler();
        RssiText = (TextView) findViewById(R.id.RssiText);
        UuidText = (TextView) findViewById(R.id.UuidText);
        MajorText = (TextView) findViewById(R.id.MajorText);
        MinorText = (TextView) findViewById(R.id.MinorText);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);

        mSails = new SAILS(this);
        //set location mode.
        mSails.setMode(SAILS.BLE_GFP_IMU);

        //new and insert a SAILS MapView from layout resource.
        mSailsMapView = new SAILSMapView(this);
        ((FrameLayout) findViewById(R.id.SAILSMap)).addView(mSailsMapView);

        mSailsMapView.post(new Runnable() {
            @Override
            public void run() {
                mSails.loadCloudBuilding("ad8538700fd94717bbeda154b2a1c584", "5705e42055cce32e10002a2d", new SAILS.OnFinishCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //mapViewInitial();
                                //routingInitial();
                                mSailsMapView.setSAILSEngine(mSails);
                                mSailsMapView.setLocationMarker(R.drawable.circle, R.drawable.arrow, null, 35);
                                mSailsMapView.setLocatorMarkerVisible(true);
                                mSailsMapView.loadFloorMap(mSails.getFloorNameList().get(0));
                                actionBar.setTitle("資電學院");
                                mSailsMapView.autoSetMapZoomAndView();;

                                mSailsMapView.setOnRegionClickListener(new SAILSMapView.OnRegionClickListener() {
                                    @Override
                                    public void onClick(List<LocationRegion> locationRegions) {
                                        if (mSailsMapView.getRoutingManager().getStartRegion() != null) {
                                            LocationRegion lr = locationRegions.get(0);
                                            mSailsMapView.getRoutingManager().setTargetMakerDrawable(Marker.boundCenterBottom(getDrawable(R.drawable.map_destination)));
                                            mSailsMapView.getRoutingManager().getPathPaint().setColor(0xFF85b038);
                                            mSailsMapView.getRoutingManager().setTargetRegion(lr);
                                            mSailsMapView.getRoutingManager().enableHandler();
                                        }
                                    }
                                });
                            }
                        });

                    }

                    @Override
                    public void onFailed(String response) {
                        Toast t = Toast.makeText(getBaseContext(), "Load cloud project fail", Toast.LENGTH_SHORT);
                        t.show();
                    }
                });
            }
        });

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.cons, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
            case 4:
                mTitle = getString(R.string.title_section4);
                break;
            case 5:
                mTitle = getString(R.string.title_section5);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        switch (id) {
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    void mapViewInitial() {
        //establish a connection of SAILS engine into SAILS MapView.
        mSailsMapView.setSAILSEngine(mSails);

        //set location pointer icon.
        mSailsMapView.setLocationMarker(R.drawable.circle, R.drawable.arrow, null, 35);

        //set location marker visible.
        mSailsMapView.setLocatorMarkerVisible(true);

        //load first floor map in package.
        mSailsMapView.loadFloorMap(mSails.getFloorNameList().get(0));
        actionBar.setTitle("資電學院");

        //Auto Adjust suitable map zoom level and position to best view position.
        mSailsMapView.autoSetMapZoomAndView();

        /*
        //set start region
        List<LocationRegion> locationRegions;
        if( rssi < -50 ) {
            locationRegions = mSails.findRegionByLabel("資電222 - 第三國際會議廳");
            mVibrator.vibrate(70);
            mSailsMapView.getMarkerManager().clear();
            mSailsMapView.getRoutingManager().setStartRegion(locationRegions.get(0));
            mSailsMapView.getMarkerManager().setLocationRegionMarker(locationRegions.get(0), Marker.boundCenter(getResources().getDrawable(R.drawable.start_point)));
        }
        else{
            locationRegions = mSails.findRegionByLabel("資電201 - 資訊系辦公室");
            mVibrator.vibrate(70);
            mSailsMapView.getMarkerManager().clear();
            mSailsMapView.getRoutingManager().setStartRegion(locationRegions.get(0));
            mSailsMapView.getMarkerManager().setLocationRegionMarker(locationRegions.get(0), Marker.boundCenter(getResources().getDrawable(R.drawable.start_point)));
        }*/

        mSailsMapView.setOnRegionClickListener(new SAILSMapView.OnRegionClickListener() {
            @Override
            public void onClick(List<LocationRegion> locationRegions) {
                if (mSailsMapView.getRoutingManager().getStartRegion() != null) {
                    LocationRegion lr = locationRegions.get(0);
                    mSailsMapView.getRoutingManager().setTargetMakerDrawable(Marker.boundCenterBottom(getDrawable(R.drawable.map_destination)));
                    mSailsMapView.getRoutingManager().getPathPaint().setColor(0xFF85b038);
                    mSailsMapView.getRoutingManager().setTargetRegion(lr);
                    mSailsMapView.getRoutingManager().enableHandler();
                }
            }
        });

        /*
        List<LocationRegion> lr = mSails.findRegionByLabel("資電201 - 資訊系辦公室");
        mSailsMapView.getRoutingManager().setTargetMakerDrawable(Marker.boundCenterBottom(getDrawable(R.drawable.map_destination)));
        mSailsMapView.getRoutingManager().getPathPaint().setColor(0xFF85b038);
        mSailsMapView.getRoutingManager().setTargetRegion(lr.get(0));
        mSailsMapView.getRoutingManager().enableHandler();
        if (mSailsMapView.getRoutingManager().enableHandler())
            distanceView.setVisibility(View.VISIBLE);
      /*
        //set location region click call back.
        mSailsMapView.setOnRegionClickListener(new SAILSMapView.OnRegionClickListener() {
            @Override
            public void onClick(List<LocationRegion> locationRegions) {
                LocationRegion lr = locationRegions.get(0);
                //begin to routing
                if (mSails.isLocationEngineStarted()) {
                    //set routing start point to current user location.
                    mSailsMapView.getRoutingManager().setStartRegion(PathRoutingManager.MY_LOCATION);
                    //set routing end point marker icon.
                    mSailsMapView.getRoutingManager().setTargetMakerDrawable(Marker.boundCenterBottom(getDrawable(R.drawable.destination)));

                    //set routing path's color.
                    mSailsMapView.getRoutingManager().getPathPaint().setColor(0xFF35b3e5);

                    endRouteButton.setVisibility(View.VISIBLE);
                    currentFloorDistanceView.setVisibility(View.VISIBLE);
                    msgView.setVisibility(View.VISIBLE);

                } else {
                    mSailsMapView.getRoutingManager().setTargetMakerDrawable(Marker.boundCenterBottom(getDrawable(R.drawable.map_destination)));
                    mSailsMapView.getRoutingManager().getPathPaint().setColor(0xFF85b038);
                    if (mSailsMapView.getRoutingManager().getStartRegion() != null)
                        endRouteButton.setVisibility(View.VISIBLE);
                }

                //set routing end point location.
                mSailsMapView.getRoutingManager().setTargetRegion(lr);
                //begin to route.
                if (mSailsMapView.getRoutingManager().enableHandler())
                    distanceView.setVisibility(View.VISIBLE);
            }
        });

        mSailsMapView.getPinMarkerManager().setOnPinMarkerClickCallback(new PinMarkerManager.OnPinMarkerClickCallback() {
            @Override
            public void OnClick(MarkerManager.LocationRegionMarker locationRegionMarker) {
                Toast.makeText(getApplication(), "(" + Double.toString(locationRegionMarker.locationRegion.getCenterLatitude()) + "," +
                        Double.toString(locationRegionMarker.locationRegion.getCenterLongitude()) + ")", Toast.LENGTH_SHORT).show();
            }
        });


        //set location region long click call back.
        mSailsMapView.setOnRegionLongClickListener(new SAILSMapView.OnRegionLongClickListener() {
            @Override
            public void onLongClick(List<LocationRegion> locationRegions) {
                if (mSails.isLocationEngineStarted())
                    return;
                mVibrator.vibrate(70);
                mSailsMapView.getMarkerManager().clear();
                mSailsMapView.getRoutingManager().setStartRegion(locationRegions.get(0));
                mSailsMapView.getMarkerManager().setLocationRegionMarker(locationRegions.get(0), Marker.boundCenter(getResources().getDrawable(R.drawable.start_point)));
            }
        });

        //design some action in floor change call back.
        mSailsMapView.setOnFloorChangedListener(new SAILSMapView.OnFloorChangedListener() {
            @Override
            public void onFloorChangedBefore(String floorName) {
                //get current map view zoom level.
                zoomSav = mSailsMapView.getMapViewPosition().getZoomLevel();
            }

            @Override
            public void onFloorChangedAfter(final String floorName) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        //check is locating engine is start and current brows map is in the locating floor or not.
                        if (mSails.isLocationEngineStarted() && mSailsMapView.isInLocationFloor()) {
                            //change map view zoom level with animation.
                            mSailsMapView.setAnimationToZoom(zoomSav);
                        }
                    }
                };
                new Handler().postDelayed(r, 1000);

                int position = 0;
                for (String mS : mSails.getFloorNameList()) {
                    if (mS.equals(floorName))
                        break;
                    position++;
                }
                floorList.setSelection(position);
            }
        });

        //design some action in mode change call back.
        mSailsMapView.setOnModeChangedListener(new SAILSMapView.OnModeChangedListener() {
            @Override
            public void onModeChanged(int mode) {
                if (((mode & SAILSMapView.LOCATION_CENTER_LOCK) == SAILSMapView.LOCATION_CENTER_LOCK) && ((mode & SAILSMapView.FOLLOW_PHONE_HEADING) == SAILSMapView.FOLLOW_PHONE_HEADING)) {
                    lockcenter.setImageDrawable(getResources().getDrawable(R.drawable.center3));
                } else if ((mode & SAILSMapView.LOCATION_CENTER_LOCK) == SAILSMapView.LOCATION_CENTER_LOCK) {
                    lockcenter.setImageDrawable(getResources().getDrawable(R.drawable.center2));
                } else {
                    lockcenter.setImageDrawable(getResources().getDrawable(R.drawable.center1));
                }
            }
        });

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mSails.getFloorDescList());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        floorList.setAdapter(adapter);
        floorList.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!mSailsMapView.getCurrentBrowseFloorName().equals(mSails.getFloorNameList().get(position)))
                    mSailsMapView.loadFloorMap(mSails.getFloorNameList().get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
    @Override
    public void onBeaconServiceConnect() {
        try {
            //beaconManager.startMonitoringBeaconsInRegion(new Region("all-beacons-region", null, null, null ));
            beaconManager.startRangingBeaconsInRegion(new Region("com.example.alex.ibeaconscan", null, null, null ));
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        //beaconManager.setMonitorNotifier(this);
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<org.altbeacon.beacon.Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    org.altbeacon.beacon.Beacon beacon = beacons.iterator().next();

                    Rssi = beacon.getRssi();
                    Uuid = beacon.getId1().toUuidString();
                    Major = beacon.getId2().toInt();
                    Minor = beacon.getId3().toInt();
                    mHandler.post(scanRunnable);
                }

            }
        });

    }

    public Runnable sendtoServer = new Runnable() {
        @Override
        public void run() {
            try {
                clientSocket = new Socket(InetAddress.getByName(address), port);
                outToServer = new DataOutputStream( clientSocket.getOutputStream() );
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    public Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            if( Rssi > PreviousRssi ) {
                if( PreviousMajor != Major && PreviousMinor != Minor ) {
                    //set start region
                    List<LocationRegion> locationRegions;
                    if (Major == 4369 && Minor == 8738) {
                        locationRegions = mSails.findRegionByLabel("資電222 - 第三國際會議廳");
                    } else {
                        locationRegions = mSails.findRegionByLabel("資電201 - 資訊系辦公室");
                    }
                    mVibrator.vibrate(70);
                    mSailsMapView.getMarkerManager().clear();
                    mSailsMapView.getRoutingManager().setStartRegion(locationRegions.get(0));
                    mSailsMapView.getMarkerManager().setLocationRegionMarker(locationRegions.get(0), Marker.boundCenter(getResources().getDrawable(R.drawable.start_point)));
                    mSailsMapView.getRoutingManager().setStartMakerDrawable(Marker.boundCenter(getResources().getDrawable(R.drawable.start_point)));
                }
            }
            PreviousRssi = Rssi;
            PreviousMajor = Major;
            PreviousMinor = Minor;

            RssiText.setText(":" + Rssi);
            UuidText.setText(":" + Uuid);
            MajorText.setText(":" + Major);
            MinorText.setText(":" + Minor);

            if(clientSocket.isConnected()) {
                try {
                    outToServer.writeUTF(Rssi + " " + Uuid + " " + Major + " " + Minor);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    };

}
