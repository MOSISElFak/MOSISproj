package zdravkovic.stefan.parkingapp;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MenuActivity extends AppCompatActivity implements View.OnClickListener{

    private Button btnProfile;
    private Button btnMap;
    private Button btnLogout;
    private Button btnFriends;
    private static final int LOCATION_PERMISSIONS_REQUEST = 999;

    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        firebaseAuth = FirebaseAuth.getInstance();
        if(firebaseAuth.getCurrentUser() == null){
            finish();
            startActivity(new Intent(this, LoginActivity.class));
        }
        FirebaseUser user = firebaseAuth.getCurrentUser();

        btnProfile = (Button) findViewById(R.id.btnProfile);
        btnMap = (Button) findViewById(R.id.btnMap);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnFriends = (Button) findViewById(R.id.btnFriends);

        btnFriends.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnMap.setOnClickListener(this);
        btnProfile.setOnClickListener(this);
    }




    @Override
    public void onClick(View v) {
        if(v == btnLogout){
            firebaseAuth.signOut();
            finish();
            startActivity(new Intent(this, LoginActivity.class));
        }
        if(v == btnMap){
            checkPermission();
            startActivity(new Intent(this,MapsActivity.class));
        }
        if (v == btnProfile){
            startActivity(new Intent(this, ProfileActivity.class));
        }
        if (v == btnFriends){
            startActivity(new Intent(this, FriendsActivity.class));
        }
    }


    private void checkPermission(){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ){
            startActivity(new Intent(this,MapsActivity.class));
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) && shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)){
                    Toast.makeText(this,
                            "Location permission required to access this activity.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSIONS_REQUEST);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){

        if (requestCode == LOCATION_PERMISSIONS_REQUEST){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //permission granted
                //proceed with this activity
                startActivity(new Intent(this,MapsActivity.class));
            }else{
                Toast.makeText(this,"For accessing this activity you need to grant location permissiom.",Toast.LENGTH_LONG).show();
                finish();
                startActivity(new Intent(this, MenuActivity.class));
            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


}
