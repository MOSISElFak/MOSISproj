package zdravkovic.stefan.parkingapp;

import android.app.Dialog;
import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.ExifInterface;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import Modules.DirectionFinder;
import Modules.DirectionFinderListener;
import Modules.Route;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, DirectionFinderListener, View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{


    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean setFocusToCurrentLocation = false;
    private Location currentLocation;
    private Marker newParking, myLocation;
    private DatabaseReference databaseReference, databaseRefRatings,dbRefUsersLocation;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    public RatingInformation rInfo;
    private boolean checkOnInfoWindowClick = false, directions = false;
    private String ratedMarkerID;
    private int markersType = 0;
    private MenuItem btnFind, btnCancel;
    private ProgressDialog progressDialog;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ArrayList<Marker> markers = new ArrayList<Marker>();
    private ArrayList<Marker> friendsMarkers = new ArrayList<Marker>();
    private ArrayList<Marker> usersMarkers = new ArrayList<Marker>();
    private DatabaseReference friendsReference;

    private ArrayList<String> friendsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        if (googleServicesAvailable()) {
            setContentView(R.layout.activity_maps);
            initMap();

            databaseReference = FirebaseDatabase.getInstance().getReference("Markers");
            databaseRefRatings = FirebaseDatabase.getInstance().getReference("Ratings");
            dbRefUsersLocation = FirebaseDatabase.getInstance().getReference("Current Location");
            friendsReference = FirebaseDatabase.getInstance().getReference("Friends");

            firebaseAuth = FirebaseAuth.getInstance();
            user = firebaseAuth.getCurrentUser();


            friendsList = new ArrayList<String>();
            GetAllFriends();
        }
    }


    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void GetAllFriends(){
        DatabaseReference ref = friendsReference.child(user.getUid());

        ref.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {

                    Friend f = child.getValue(Friend.class);
                    if(f != null) {
                        friendsList.add(f.id);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
    boolean checkRating = false;

    private void updateRatingToMarker(String markerId) {
        checkRating = true;

        getRatingsForMarker(markerId,null,0.0,0.0);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_map, menu);
        btnFind = menu.findItem(R.id.find);
        btnCancel = menu.findItem(R.id.findClose);

        return super.onCreateOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.menuAll:
                markersType = 0;
                getAllMarkersFromDatabase();
                break;
            case R.id.menuFree:
                markersType = 1;
                getAllMarkersFromDatabase();
                break;
            case R.id.menuPaid:
                markersType = 2;
                getAllMarkersFromDatabase();
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
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        if(mMap != null){

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
                    if(marker.getId().equals(myLocation.getId()) || marker.getId().equals( newParking.getId())){
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
                    for (Marker m : usersMarkers) {
                        if (marker.getId().equals( m.getId()))
                            return true;
                    }
                    for (Marker m : friendsMarkers) {

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
            getAllMarkersFromDatabase();
            SetFriendsMarkers();

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

                        if (mInfo != null) {
                            // Toast.makeText(MapsActivity.this,mInfo.title + " ",Toast.LENGTH_SHORT).show();
                            String tmp = mInfo.latitude + mInfo.longitude;
                            String markerId = tmp.replaceAll("\\.", "P");
                            //Toast.makeText(MapsActivity.this,markerId + " ",Toast.LENGTH_SHORT).show();
                            rInfo = null;
                            checkOnInfoWindowClick = true;
                            getRatingsForMarker(markerId, mInfo, lat, lng);
                        } else
                            showRatingDialog(lat, lng, 0, null, 0, 0);
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

    private void SetFriendsMarkers() {

        dbRefUsersLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RemoveUserAndFriendsMarkers();
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {

                    UserLocationInfo location = child.getValue(UserLocationInfo.class);
                    LatLng latlng = new LatLng(location.latitude,location.longitude);
                    boolean isFriend = friendsList.contains(location.id);
                    if (!location.id.equals(user.getUid()))
                        addMarker(latlng,location.id ,isFriend);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void RemoveUserAndFriendsMarkers(){
        for (Marker m : usersMarkers) {
            m.remove();
        }
        for (Marker m : friendsMarkers) {
            m.remove();
        }
        usersMarkers.clear();
        friendsMarkers.clear();
    }

    private void addMarker(final LatLng latlng, String userID, boolean isFriend) {
        if (isFriend){

            MarkerOptions options = new MarkerOptions()
                    .draggable(false)
                    .title(userID)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_friend_marker))
                    .position(latlng);
            Marker m = mMap.addMarker(options);

            friendsMarkers.add(m);
        }else{

            MarkerOptions options = new MarkerOptions()
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_users_marker))
                    .position(latlng);
            Marker m = mMap.addMarker(options);

            usersMarkers.add(m);
        }
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
                        avgRating = (float)sumRating / (float)numRatings;
                        rOFCalculation.avgRating = round(avgRating, 2);
                        rOFCalculation.sumChecked = sumChecked;
                        rOFCalculation.sumUnchecked = sumUnchecked;

                        check = true;
                        getMarkerInfo(markerId,rOFCalculation);
                    }

                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                checkRating = false;
                checkOnInfoWindowClick = false;
            }
        });
    }


    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

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

            UserLocationInfo lInfo = new UserLocationInfo(location.getLatitude(),location.getLongitude(),user.getUid());
            dbRefUsersLocation.child(user.getUid()).setValue(lInfo);
        }

    }



    public void getMarkerInfo(final String markerId, final RatingObject ratingObject ){
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
               if (check) {

                   MarkerInformation markerInformation = dataSnapshot.child(ratedMarkerID).getValue(MarkerInformation.class);
                   check = false;
                  // Toast.makeText(MapsActivity.this,"Your ratings",Toast.LENGTH_SHORT).show();
                   if(ratingObject != null && markerInformation != null){
                       if(ratingObject.avgRating > 0 || ratingObject.sumUnchecked > 0){
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
        mLocationRequest.setInterval(5000);

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

        float min = Float.MAX_VALUE;
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
}
