package zdravkovic.stefan.parkingapp;

import android.app.ActionBar;
import android.app.Dialog;
import android.Manifest;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Modules.DirectionFinder;
import Modules.DirectionFinderListener;
import Modules.Route;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, DirectionFinderListener, View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener,ResultCallback<Status> {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean setFocusToCurrentLocation = false;
    private Location currentLocation;
    private Marker myCar, newParking, myLocation;
    private DatabaseReference databaseReference, databaseRefRatings;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    public RatingInformation rInfo;
    private boolean checkOnInfoWindowClick = false, directions = false;
    private String ratedMarkerID;
    private int markersType = 0;
    private MenuItem btnFind, btnCancel,btnClearGeofence;
    private ProgressDialog progressDialog;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (googleServicesAvailable()) {
            setContentView(R.layout.activity_maps);
            initMap();

            databaseReference = FirebaseDatabase.getInstance().getReference("Markers");
            databaseRefRatings = FirebaseDatabase.getInstance().getReference("Ratings");
            firebaseAuth = FirebaseAuth.getInstance();
            user = firebaseAuth.getCurrentUser();


        }
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    public boolean googleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Cant connect to play services", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    private void showAlertDialog(double lat, double lng){
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapsActivity.this);
        View v = getLayoutInflater().inflate(R.layout.info_window, null);
        Button btnSave = (Button) v.findViewById(R.id.btn_save_marker);
        final RadioGroup rgFree = (RadioGroup) v.findViewById(R.id.radio_g_free);
        final TextView tvPrice = (TextView) v.findViewById(R.id.tv_price);
        final EditText editTextPrice = (EditText) v.findViewById(R.id.editTextPrice);
        final LinearLayout linearLayoutPrice = (LinearLayout) v.findViewById(R.id.ll_price);
        final EditText editTextTitle = (EditText) v.findViewById(R.id.et_title);

        final String latitude = String.valueOf(lat);
        final String longitude = String.valueOf(lng);

        rgFree.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if(checkedId == R.id.rb_yes){

                    tvPrice.setVisibility(View.GONE);
                    linearLayoutPrice.setVisibility(View.GONE);
                }
                if(checkedId == R.id.rb_no){
                    tvPrice.setVisibility(View.VISIBLE);
                    linearLayoutPrice.setVisibility(View.VISIBLE);
                }
            }
        });
        mBuilder.setView(v);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String title = editTextTitle.getText().toString().trim();
                String price;
                int checkedID = rgFree.getCheckedRadioButtonId();
                if(checkedID == R.id.rb_no){
                    price = editTextPrice.getText().toString().trim() + "€";
                }else{
                    price = "Free";
                }

                String id = latitude+""+longitude;
                id =id.replaceAll("\\.","P");
                MarkerInformation markerInformation = new MarkerInformation(latitude,longitude,title,0,0,0,price);
                databaseReference.child(id).setValue(markerInformation);
                Toast.makeText(MapsActivity.this,"Saved successfully!",Toast.LENGTH_SHORT).show();
                newParking.setVisible(false);
                dialog.dismiss();
            }
        });
    }

    private void showRatingDialog(double lat, double lng, float rating, Boolean checked, int sumChecked, int sumUnchecked){
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapsActivity.this);
        final View v = getLayoutInflater().inflate(R.layout.rating_window, null);

        Button btnSave = (Button) v.findViewById(R.id.btnSaveRating);
        final RadioGroup rgVerify = (RadioGroup) v.findViewById(R.id.radioGroupVerify);
        final RatingBar ratingBar = (RatingBar) v.findViewById(R.id.ratingBar);
        final RadioButton rbChecked = (RadioButton) v.findViewById(R.id.rb_check);
        final RadioButton rbUnchecked = (RadioButton) v.findViewById(R.id.rb_unchecked);

        //(MapsActivity.this,"Your ratings in dialog are: " + rating,Toast.LENGTH_SHORT).show();
        ratingBar.setRating(rating);
        if(rating>0){
            ratingBar.setIsIndicator(true);
            btnSave.setVisibility(View.GONE);
        }

        if(checked != null){
            if(checked){
                rgVerify.check(R.id.rb_check);
                rbChecked.setBackgroundColor(getResources().getColor(R.color.veryLightGreen));
                rbUnchecked.setBackgroundColor(Color.TRANSPARENT);
            }else {
                rgVerify.check(R.id.rb_unchecked);
                rbUnchecked.setBackgroundColor(getResources().getColor(R.color.veryLightRed));
                rbChecked.setBackgroundColor(Color.TRANSPARENT);
            }
            rbChecked.setEnabled(false);
            rbUnchecked.setEnabled(false);
        }
        rbChecked.setText(String.valueOf(sumChecked));
        rbUnchecked.setText(String.valueOf(sumUnchecked));

        rgVerify.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {

             //   RadioButton rbChecked = (RadioButton) v.findViewById(R.id.rb_check);
              //  RadioButton rbUnchecked =  (RadioButton) v.findViewById(R.id.rb_unchecked);

                if(checkedId == R.id.rb_check){
                    rbChecked.setBackgroundColor(getResources().getColor(R.color.veryLightGreen));
                    rbUnchecked.setBackgroundColor(Color.TRANSPARENT);
                }
                if (checkedId == R.id.rb_unchecked){
                    rbUnchecked.setBackgroundColor(getResources().getColor(R.color.veryLightRed));
                    rbChecked.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });

        String tempId = lat+""+lng;
        ratedMarkerID = tempId.replaceAll("\\.","P");
        mBuilder.setView(v);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean validation = true;
                boolean checked = false;
                float rating = ratingBar.getRating();

                int checkedId = rgVerify.getCheckedRadioButtonId();
                if(checkedId == R.id.rb_check){
                    checked = true;
                }
                if(checkedId == R.id.rb_unchecked){
                    checked = false;
                }
                if(checkedId == -1){
                    Toast.makeText(MapsActivity.this,"You didn't verified this parking. Your rating wont be saved.",Toast.LENGTH_SHORT).show();
                    validation = false;
                }

                if(validation){
                   // Toast.makeText(MapsActivity.this,rating + " /// " + checked,Toast.LENGTH_LONG).show();
                    RatingInformation ratingInfo = new RatingInformation(rating,checked);
                    databaseRefRatings.child(ratedMarkerID).child(user.getUid()).setValue(ratingInfo);
                    updateRatingToMarker(ratedMarkerID);
                }
                dialog.dismiss();
            }
        });
    }

    boolean check = false;

    private void updateRatingToMarker(String markerId) {
        checkRating = true;
        //resultOfCalculation = new RatingObject();
        getRatingsForMarker(markerId,null,0.0,0.0);
        //RatingObject ratingObject = resultOfCalculation;
    }

    boolean checkRating = false;
    public RatingObject resultOfCalculation = null;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        btnFind = menu.findItem(R.id.find);
        btnCancel = menu.findItem(R.id.findClose);
        btnClearGeofence = menu.findItem(R.id.btnClear);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.menuAll:
                markersType = 0;
                setAllMarkers();
                break;
            case R.id.menuFree:
                markersType = 1;
                setAllMarkers();
                break;
            case R.id.menuPaid:
                markersType = 2;
                setAllMarkers();
                break;
            case R.id.btnMyCar:
                myCar.setPosition(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()));
                myCar.setVisible(true);
                startGeofence();
                break;
            case R.id.find:
                sendRequest();
                btnFind.setVisible(false);
                btnCancel.setVisible(true);
                break;
            case R.id.findClose:
                clearRout();
                btnCancel.setVisible(false);
                btnFind.setVisible(true);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()), 16));
                break;
            case R.id.btnClear:
                clearGeofence();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(mMap != null){

            int height = 50;
            int width = 50;
            BitmapDrawable bitmapdraw =(BitmapDrawable)getResources().getDrawable(R.drawable.unnamed);
            Bitmap b = bitmapdraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
            myCar = mMap.addMarker(new MarkerOptions()
                    .draggable(false)
                    .position(new LatLng(0,0))
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                    .snippet("my car")
                    .visible(false));
            int h = 100;
            int w = 80;
            BitmapDrawable bitmapd =(BitmapDrawable)getResources().getDrawable(R.drawable.locationmarkerparking);
            Bitmap bmp = bitmapd.getBitmap();
            Bitmap sMarker = Bitmap.createScaledBitmap(bmp, w, h, false);
            newParking = mMap.addMarker(new MarkerOptions()
                                .draggable(true)
                                .icon(BitmapDescriptorFactory.fromBitmap(sMarker))
                                .position(new LatLng(0,0))
                                .visible(false));

            myLocation = mMap.addMarker(new MarkerOptions().position(new LatLng(0,0)).visible(false));
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    if(marker.getId().equals(myLocation.getId()) || marker.getId().equals(myCar.getId()) || marker.getId().equals( newParking.getId())){
                        return true;
                    }
                    for (Marker m : originMarkers) {
                        if (marker.getId().equals(m.getId()))
                            return true;
                    }
                    for (Marker m : destinationMarkers) {
                        if (marker.getId().equals( m.getId()))
                            return true;
                    }
                    return false;
                }
            });
            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    MapsActivity.this.setNewMarker(latLng.latitude, latLng.longitude);
                    //showAlertDialog();
                }
            });
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                }
            });

           /* mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    return false;
                }
            });*/

            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {

                }

                @Override
                public void onMarkerDrag(Marker marker) {

                }

                @Override
                public void onMarkerDragEnd(Marker marker) {

                    LatLng ll = marker.getPosition();
                    double lat = ll.latitude;
                    double lng = ll.longitude;

                    showAlertDialog(lat,lng);
                }
            });
            setAllMarkers();

            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter(){

                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    View v = getLayoutInflater().inflate(R.layout.info, null);

                    TextView tvTitle = (TextView) v.findViewById(R.id.tv_title);
                    TextView tvPrice = (TextView) v.findViewById(R.id.tv_parking_cost);
                    TextView tvRating = (TextView) v.findViewById(R.id.tv_rating);
                    TextView tvChecked = (TextView) v.findViewById(R.id.tv_checked_green);
                    TextView tvUnchecked = (TextView) v.findViewById(R.id.tv_checked_red);

                    MarkerInformation markerInfo = (MarkerInformation) marker.getTag();
                    if(markerInfo!=null){

                        tvTitle.setText(marker.getTitle());
                        tvPrice.setText(markerInfo.price);
                        tvRating.setText(String.valueOf(markerInfo.averageRating));
                        tvChecked.setText(String.valueOf(markerInfo.sumChecked));
                        tvUnchecked.setText(String.valueOf(markerInfo.sumUnchecked));
                    }
                    return v;
                }
            });

            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    double lat = marker.getPosition().latitude;
                    double lng = marker.getPosition().longitude;

                    MarkerInformation mInfo = (MarkerInformation) marker.getTag();

                    if(mInfo != null) {
                       // Toast.makeText(MapsActivity.this,mInfo.title + " ",Toast.LENGTH_SHORT).show();
                        String tmp = mInfo.latitude + mInfo.longitude;
                        String markerId = tmp.replaceAll("\\.", "P");
                        //Toast.makeText(MapsActivity.this,markerId + " ",Toast.LENGTH_SHORT).show();
                        rInfo = null;
                        checkOnInfoWindowClick = true;
                        getRatingsForMarker(markerId,mInfo,lat,lng);
                    }else
                        showRatingDialog(lat,lng, 0, null, 0, 0);
                }
            });
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        setFocusToCurrentLocation = false;
    }

    private void getRatingsForMarker(final String markerId, final MarkerInformation mInfo, final double lat, final double lng) {

        databaseRefRatings.child(markerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(checkOnInfoWindowClick){
                    rInfo = dataSnapshot.child(user.getUid()).getValue(RatingInformation.class);
                    //Toast.makeText(MapsActivity.this,"Your ratings are: " + rInfo.rating,Toast.LENGTH_SHORT).show();
                    checkOnInfoWindowClick = false;
                    if (rInfo != null)
                        showRatingDialog(lat, lng, rInfo.rating, rInfo.checked, mInfo.sumChecked, mInfo.sumUnchecked);
                    else
                        showRatingDialog(lat,lng, 0, null, mInfo.sumChecked, mInfo.sumUnchecked);
                }
                if(checkRating){
                    checkRating = false;
                    RatingObject rOFCalculation = new RatingObject();
                    Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                    int sumRating = 0;
                    int numRatings = 0;
                    int sumChecked = 0;
                    int sumUnchecked = 0;
                    float avgRating = 0;

                    for (DataSnapshot child : children){

                        RatingInformation tmpRating = child.getValue(RatingInformation.class);
                        sumRating += tmpRating.rating;
                        numRatings++;
                        if(tmpRating.checked)
                            sumChecked++;
                        else
                            sumUnchecked++;
                    }
                    if (numRatings > 0){
                        avgRating = sumRating / numRatings;
                        rOFCalculation.avgRating = avgRating;
                        rOFCalculation.sumChecked = sumChecked;
                        rOFCalculation.sumUnchecked = sumUnchecked;

                        check = true;
                        getMarkerInfoOrSetAllMarkers(markerId,rOFCalculation);
                    }

                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                checkRating = false;
                checkOnInfoWindowClick = false;
            }
        });
        //Toast.makeText(MapsActivity.this,"Your ratings at the end are: " + rInfo.rating,Toast.LENGTH_SHORT).show();
    }


    ArrayList<Marker> markers = new ArrayList<Marker>();

    private void setMarker(String title, double lat, double lng, MarkerInformation markerInfo,boolean dragable) {

            MarkerOptions options = new MarkerOptions()
                    .title(title)
                    .draggable(dragable)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.markerparking))
                    .position(new LatLng(lat, lng));
            Marker m = mMap.addMarker(options);
            m.setTag(markerInfo);
            markers.add(m);
    }

    private void setNewMarker(double lat, double lng) {

                newParking.setPosition(new LatLng(lat, lng));
                newParking.setVisible(true);
    }
    private void removeAllMarkers(){
        for(Marker marker : markers) {
            marker.remove();
        }
        markers.clear();
    }
    @Override
    protected void onResume() {
        super.onResume();
        setFocusToCurrentLocation = false;
    }

    private void setMarkerOnCurrentLocation(double currentLatitude, double currentLongitude){
            if (currentLatitude != 0 && currentLongitude != 0){

                LatLng ll = new LatLng(currentLatitude, currentLongitude);

                myLocation.setPosition(ll);
                myLocation.setTitle("Your position");
                myLocation.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_position));
                myLocation.setVisible(true);

                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 16);
                mMap.animateCamera(update);
            }
        }

    @Override
    public void onLocationChanged(Location location) {

        if (location == null) {
            Toast.makeText(this, "Cant get current location", Toast.LENGTH_LONG).show();
        } else {

            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            myLocation.setPosition(ll);
            currentLocation = location;
            if(!setFocusToCurrentLocation) {
                setMarkerOnCurrentLocation(location.getLatitude(), location.getLongitude());
                setFocusToCurrentLocation = true;
            }
           // if (directions){
                //sendRequest();
            //}
        }

    }

    private void setAllMarkers(){

        getAllMarkersFromDatabase();
    }

    public void getMarkerInfoOrSetAllMarkers(final String markerId, final RatingObject ratingObject ){
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
              /*  if (setAllMarkers){

                    Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                    removeAllMarkers();
                    for (DataSnapshot child: children) {

                        MarkerInformation markerInfo = child.getValue(MarkerInformation.class);
                        double lat = Double.parseDouble(markerInfo.latitude);
                        double lng = Double.parseDouble(markerInfo.longitude);
                        if(markersType == 0){

                            setMarker(markerInfo.title,lat,lng,markerInfo,false);
                        }
                        if(markersType == 1){
                            if(markerInfo.price.equals("Free")){
                                setMarker(markerInfo.title,lat,lng,markerInfo,false);
                            }
                        }
                        if (markersType == 2){
                            if(!markerInfo.price.equals("Free")){
                                setMarker(markerInfo.title,lat,lng,markerInfo,false);
                            }
                        }

                    }
                    setAllMarkers = false;
                }*/
               if (check) {

                   MarkerInformation markerInformation = dataSnapshot.child(ratedMarkerID).getValue(MarkerInformation.class);
                   check = false;
                  // Toast.makeText(MapsActivity.this,"Your ratings",Toast.LENGTH_SHORT).show();
                   if(ratingObject != null && markerInformation != null){
                       if(ratingObject.avgRating > 0){
                           markerInformation.averageRating = ratingObject.avgRating;
                           markerInformation.sumChecked = ratingObject.sumChecked;
                           markerInformation.sumUnchecked = ratingObject.sumUnchecked;

                           databaseReference.child(markerId).setValue(markerInformation);
                           Toast.makeText(MapsActivity.this,"Your ratings are successfully added. Thanks!",Toast.LENGTH_SHORT).show();

                           //setAllMarkers();
                       }
                   }
               }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if(check){
                    check = false;
                }
            }
        });
    }

    private void getAllMarkersFromDatabase(){
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                removeAllMarkers();
                for (DataSnapshot child: children) {

                    MarkerInformation markerInfo = child.getValue(MarkerInformation.class);
                    double lat = Double.parseDouble(markerInfo.latitude);
                    double lng = Double.parseDouble(markerInfo.longitude);
                    if(markersType == 0){

                        setMarker(markerInfo.title,lat,lng,markerInfo,false);
                    }
                    if(markersType == 1){
                        if(markerInfo.price.equals("Free")){
                            setMarker(markerInfo.title,lat,lng,markerInfo,false);
                        }
                    }
                    if (markersType == 2){
                        if(!markerInfo.price.equals("Free")){
                            setMarker(markerInfo.title,lat,lng,markerInfo,false);
                        }
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onClick(View v) {
    }

    private void clearRout() {

        for (Polyline p: polylinePaths) {
            p.remove();
        }
        for (Marker m: originMarkers) {
            m.remove();
        }
        for (Marker m: destinationMarkers) {
            m.remove();
        }
        polylinePaths.clear();
        originMarkers.clear();
        destinationMarkers.clear();
    }

    private void sendRequest() {

        String origin = currentLocation.getLatitude() + "," + currentLocation.getLongitude();

        Location min = getNearestParking();
        String destination = min.getLatitude() + "," + min.getLongitude();

        try {
            new DirectionFinder(this, origin, destination).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private Location getNearestParking() {
        float min = 999999999;
        Location minDest = null;
        for (Marker marker : markers) {

            double lat = marker.getPosition().latitude;
            double lng = marker.getPosition().longitude;
            Location dest = new Location("");
            dest.setLatitude(lat);
            dest.setLongitude(lng);
            float distanceInMeters = currentLocation.distanceTo(dest);
            if(distanceInMeters < min){

                minDest = dest;
                min = distanceInMeters;
            }
        }
        //destination = minDest.getLatitude()+"," + minDest.getLongitude();
      //  Toast.makeText(this,min+"m", Toast.LENGTH_SHORT).show();
        return minDest;
    }

    @Override
    public void onDirectionFinderStart() {
        progressDialog = ProgressDialog.show(this, "Molimo sacekajte.", "", true);
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> route) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        for (Route r : route) {

            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(r.startLocation, 15));

            originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pushpin))
                    .title(r.startAddress)
                    .position(r.startLocation)));

            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green))
                    .title(r.endAddress)
                    .position(r.endLocation)));


            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(10);
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (int i = 0; i < r.points.size(); i++) {
                polylineOptions.add(r.points.get(i));
                builder.include(r.points.get(i));
            }

            builder.include(r.startLocation);
            builder.include(r.endLocation);

            //mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
            LatLngBounds bounds = builder.build();
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 50);
            mMap.animateCamera(cu, new GoogleMap.CancelableCallback(){
                public void onCancel(){}
                public void onFinish(){
                    CameraUpdate zout = CameraUpdateFactory.zoomBy(-1);
                    mMap.animateCamera(zout);
                }
            });

            polylinePaths.add(mMap.addPolyline(polylineOptions));

        }
    }
    private static final String NOTIFICATION_MSG = "You forgot your car!";
    // Create a Intent send by the notification
    public static Intent makeNotificationIntent(Context context, String msg) {
        Intent intent = new Intent( context, MapsActivity.class );
        intent.putExtra( NOTIFICATION_MSG, msg );
        return intent;
    }

    // Start Geofence creation process
    private void startGeofence() {
      //  Log.i(TAG, "startGeofence()");
        if( myCar != null ) {
            Geofence geofence = createGeofence( myCar.getPosition(), GEOFENCE_RADIUS );
            GeofencingRequest geofenceRequest = createGeofenceRequest( geofence );
            addGeofence( geofenceRequest );
        } else {
           // Log.e(TAG, "Geofence marker is null");
        }
    }

    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final float GEOFENCE_RADIUS = 5.0f; // in meters

    // Create a Geofence
    private Geofence createGeofence( LatLng latLng, float radius ) {
        //Log.d(TAG, "createGeofence");
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion( latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration( GEO_DURATION )
                .setTransitionTypes( Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT )
                .build();
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest( Geofence geofence ) {
        //Log.d(TAG, "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER )
                .addGeofence( geofence )
                .build();
    }

    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;
    private PendingIntent createGeofencePendingIntent() {
       // Log.d(TAG, "createGeofencePendingIntent");
        if ( geoFencePendingIntent != null )
            return geoFencePendingIntent;

        Intent intent = new Intent( this, GeofenceTransitionService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT );
    }
    // Check for permission to access Location
    private boolean checkPermission() {
       // Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }
    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
       // Log.d(TAG, "addGeofence");
        if (checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this);
    }

    @Override
    public void onResult(@NonNull Status status) {
     //   Log.i(TAG, "onResult: " + status);
        if ( status.isSuccess() ) {
            saveGeofence();
            drawGeofence();
        } else {
            // inform about fail
        }
    }

    // Draw Geofence circle on GoogleMap
    private Circle geoFenceLimits;
    private void drawGeofence() {
      //  Log.d(TAG, "drawGeofence()");

        if ( geoFenceLimits != null )
            geoFenceLimits.remove();

        CircleOptions circleOptions = new CircleOptions()
                .center( myCar.getPosition())
                .strokeColor(Color.argb(50, 70,70,70))
                .fillColor( Color.argb(100, 150,150,150) )
                .radius( GEOFENCE_RADIUS );
        geoFenceLimits = mMap.addCircle( circleOptions );
    }

    private final String KEY_GEOFENCE_LAT = "GEOFENCE LATITUDE";
    private final String KEY_GEOFENCE_LON = "GEOFENCE LONGITUDE";

    // Saving GeoFence marker with prefs mng
    private void saveGeofence() {
       // Log.d(TAG, "saveGeofence()");
        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putLong( KEY_GEOFENCE_LAT, Double.doubleToRawLongBits( myCar.getPosition().latitude ));
        editor.putLong( KEY_GEOFENCE_LON, Double.doubleToRawLongBits( myCar.getPosition().longitude ));
        editor.apply();
    }

    // Recovering last Geofence marker
    private void recoverGeofenceMarker() {
        //Log.d(TAG, "recoverGeofenceMarker");
        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );

        if ( sharedPref.contains( KEY_GEOFENCE_LAT ) && sharedPref.contains( KEY_GEOFENCE_LON )) {
            double lat = Double.longBitsToDouble( sharedPref.getLong( KEY_GEOFENCE_LAT, -1 ));
            double lon = Double.longBitsToDouble( sharedPref.getLong( KEY_GEOFENCE_LON, -1 ));
            LatLng latLng = new LatLng( lat, lon );
            markerForGeofence(latLng);
            drawGeofence();
        }
    }
    private void markerForGeofence(LatLng latLng) {
        //Log.i(TAG, "markerForGeofence("+latLng+")");
        String title = latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title);
        if ( mMap!=null ) {
            // Remove last geoFenceMarker
            if (myCar != null)
                myCar.remove();

            myCar = mMap.addMarker(markerOptions);

        }
    }
    // Clear Geofence
    private void clearGeofence() {
       // Log.d(TAG, "clearGeofence()");
        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                createGeofencePendingIntent()
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if ( status.isSuccess() ) {
                    // remove drawing
                    removeGeofenceDraw();
                }
            }
        });
    }

    private void removeGeofenceDraw() {
        //Log.d(TAG, "removeGeofenceDraw()");
        if ( myCar != null)
            myCar.remove();
        if ( geoFenceLimits != null )
            geoFenceLimits.remove();
    }
}
