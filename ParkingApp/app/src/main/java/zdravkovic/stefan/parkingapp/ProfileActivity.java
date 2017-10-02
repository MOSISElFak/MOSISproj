package zdravkovic.stefan.parkingapp;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

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
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int CAMERA_REQUEST = 1888;
    private static final int CAMERA_PERMISSIONS_REQUEST = 666;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private EditText editTextFirstName, editTextLastName, editTextAddress, editTextCity, editTextCountry, editTextZip, editTextPhoneNumber;
    private Button btnSave;
    private FirebaseUser user;
    private ImageView imageView;
    private StorageReference mStorageRef;
    private String userID;
    private String mImageFileLocation = "";
    private Uri mImageUri;
    private boolean photoIsTaken = false;
    private FloatingActionButton fabUpload,fabMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        mStorageRef = FirebaseStorage.getInstance().getReference("Avatars");
        firebaseAuth = FirebaseAuth.getInstance();

        user = firebaseAuth.getCurrentUser();
        if (user == null) {
            finish();
            startActivity(new Intent(this, LoginActivity.class));
        }
        userID = user.getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);

        editTextAddress = (EditText) findViewById(R.id.editTextAddress);
        editTextFirstName = (EditText) findViewById(R.id.editTextFirstName);
        editTextLastName = (EditText) findViewById(R.id.editTextLastName);
        editTextCity = (EditText) findViewById(R.id.editTextCity);
        editTextCountry = (EditText) findViewById(R.id.editTextCountry);
        editTextZip = (EditText) findViewById(R.id.editTextZip);
        editTextPhoneNumber = (EditText) findViewById(R.id.editTextPhoneNumber);
        imageView = (ImageView) findViewById(R.id.imageView);

        try {
            setProfilePicture();
        }catch (IOException e){
            e.printStackTrace();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });

        fabMenu = (FloatingActionButton) findViewById(R.id.fabMenu);
        fabMenu.setOnClickListener(this);

        fabUpload = (FloatingActionButton) findViewById(R.id.fabUpload);
        fabUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadPicture(mImageUri);
            }
        });
    }

    private void setProfilePicture() throws IOException {
        final File localFile = File.createTempFile("images", "jpg");
        StorageReference profileRef = mStorageRef.child(userID + "/avatar.jpg");
        profileRef.getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        String imageLocation = localFile.getAbsolutePath();
                        rotateImage(setReducedImageSize(imageLocation),imageLocation);
                       // imageView.setImageBitmap(bmp);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle failed download
                Toast.makeText(getApplicationContext(),"Something went wrong, couldn't download profile picture",Toast.LENGTH_LONG).show();
            }
        });
    }
    private void uploadPicture(Uri file) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading ...");
        progressDialog.show();

        StorageReference profileRef = mStorageRef.child(userID + "/avatar.jpg");
        profileRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Success!", Toast.LENGTH_LONG).show();
                        fabUpload.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        progressDialog.dismiss();
                        // Handle unsuccessful uploads
                        Toast.makeText(ProfileActivity.this, "Something went wrong.." + exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        //displaying the upload progress
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                    }
                });
    }

//-----------------------------------------------//
    public void takePhoto(){
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                ){
            callCameraApp();
        }else{
            if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) && shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                Toast.makeText(this,
                        "External storage and camera permission required to save images.",
                        Toast.LENGTH_SHORT).show();
            }

            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA}, CAMERA_PERMISSIONS_REQUEST);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){

        if (requestCode == CAMERA_PERMISSIONS_REQUEST){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                callCameraApp();
            }else{
                Toast.makeText(this,"External write and camera permissions has not been granted, cannot saved images",Toast.LENGTH_SHORT).show();
            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void callCameraApp(){

        Intent callCameraApplicationIntent = new Intent();
        callCameraApplicationIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = null;
        try{

            photoFile = createImageFile();
        }catch (IOException e){

            e.printStackTrace();
        }
        String authorities = getApplicationContext().getPackageName() + ".fileprovider";
        mImageUri = FileProvider.getUriForFile(this,authorities, photoFile);
        callCameraApplicationIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        startActivityForResult(callCameraApplicationIntent,CAMERA_REQUEST);
    }

   private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMAGE_" + timeStamp +"_";
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(imageFileName,".jpg",storageDirectory);
        mImageFileLocation = image.getAbsolutePath();

       return image;
    }
    private Bitmap setReducedImageSize(String fileLocation){
        int targetImageViewWidth = imageView.getWidth();
        int targetImageViewHeight = imageView.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileLocation,bmOptions);
        int cameraImageWidth = bmOptions.outWidth;
        int cameraImageHeight = bmOptions.outHeight;

        int scaleFactor = Math.min(cameraImageHeight/targetImageViewHeight,cameraImageWidth/targetImageViewWidth);

        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(fileLocation, bmOptions);
    }

    private void rotateImage(Bitmap bitmap,String fileLocation){

        ExifInterface exifInterface = null;
        try{
            exifInterface = new ExifInterface(fileLocation);
        }catch (IOException e){
            e.printStackTrace();
        }
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED);
        Matrix matrix = new Matrix();
        switch (orientation){
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
        }
        Bitmap rotatedBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        imageView.setImageBitmap(rotatedBmp);
    }
    //-----------------------------------------------//
    @Override
    public void onWindowFocusChanged(boolean focus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(focus);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(photoIsTaken){
           // Toast.makeText(this,photoIsTaken + "// Inside onWindowFocusChanged() if statemant",Toast.LENGTH_LONG).show();
            photoIsTaken = false;
            rotateImage(setReducedImageSize(mImageFileLocation),mImageFileLocation);
            fabUpload.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Toast.makeText(this,photoIsTaken + "//",Toast.LENGTH_LONG).show();
       if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
           photoIsTaken = true;
         //  Toast.makeText(this,photoIsTaken + "// Inside onActivityResult() if statemant",Toast.LENGTH_LONG).show();
       }
    }

    @Override
    protected void onStart() {
        super.onStart();
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserInformation userInfo = dataSnapshot.child(user.getUid()).getValue(UserInformation.class);
                if (userInfo != null){

                    editTextAddress.setText(userInfo.address);
                    editTextFirstName.setText(userInfo.first_name);
                    editTextLastName.setText(userInfo.last_name);
                    editTextCity.setText(userInfo.city);
                    editTextCountry.setText(userInfo.country);
                    editTextZip.setText(userInfo.zip);
                    editTextPhoneNumber.setText(userInfo.phone_number);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this,"Something went wrong. Please try again...",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserInfo(){
        String first_name = editTextFirstName.getText().toString().trim();
        String last_name = editTextLastName.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();
        String city = editTextCity.getText().toString().trim();
        String country = editTextCountry.getText().toString().trim();
        String zip = editTextZip.getText().toString().trim();
        String phone_number = editTextPhoneNumber.getText().toString().trim();

        UserInformation userInformation = new UserInformation(first_name,last_name,address,city,country,zip,phone_number);

        databaseReference.child(user.getUid()).setValue(userInformation);
        Toast.makeText(this, "Infomation saved..", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onClick(View v) {
        if(v == btnSave){
            //Save
            saveUserInfo();
        }
        if(v == fabMenu){
            finish();
            startActivity(new Intent(this, MenuActivity.class));
        }
    }
}
