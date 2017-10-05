package zdravkovic.stefan.parkingapp;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity implements View.OnClickListener{

    private Button btnAddFriend;
    private BluetoothAdapter bluetoothAdapter = null;
    private static final int BT_DISCOVERABLE_TIME = 240;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private String connectedDeviceName = null;
    private StringBuffer outStringBuffer;

    String idUser;

    private final static String TAG = "BT";
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    private ChatService chatService = null;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private DatabaseReference friendsReference, userReference;

    private ListView lvFriendsList;
    private ArrayAdapter<String> friendsList;
    private ArrayList<String> friendsIDList;
    private ArrayAdapter<String> friendsIDListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        btnAddFriend = (Button) findViewById(R.id.addFriend);
        btnAddFriend.setOnClickListener(this);

        lvFriendsList = (ListView) findViewById(R.id.lvFriendsList);
        friendsList = new ArrayAdapter<String>(this,R.layout.friend_name);
        lvFriendsList.setAdapter(friendsList);
        friendsIDList = new ArrayList<String>();

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        idUser = user.getUid();

        friendsReference = FirebaseDatabase.getInstance().getReference("Friends");
        userReference = FirebaseDatabase.getInstance().getReference("Users");

        getAllFriends();
    }


    @Override
    public void onClick(View v) {
        if (v == btnAddFriend){

            //  Snackbar.make(view, "Wait for incoming friend request or send one.", Snackbar.LENGTH_LONG).setAction("Action", null).show();

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                Toast.makeText(FriendsActivity.this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
                //finish();
                return;
            }
            ensureDiscoverable(bluetoothAdapter);
        }
    }


    private void saveNewFriend(final String friendId){

        DatabaseReference ref = userReference.child(friendId);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                UserInformation userInfo = dataSnapshot.getValue(UserInformation.class);

                if(userInfo != null){

                    String friendName = userInfo.first_name + " " + userInfo.last_name;
                    Friend f = new Friend(friendName, friendId);
                    friendsReference.child(user.getUid()).child(user.getUid()).setValue(f);
                    Toast.makeText(FriendsActivity.this,"You got new friend!",Toast.LENGTH_SHORT).show();
                }else{

                    String friendName = " ";
                    Friend f = new Friend(friendName, friendId);
                    friendsReference.child(user.getUid()).child(user.getUid()).setValue(f);
                    Toast.makeText(FriendsActivity.this,"You got new friend!",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private void getAllFriends(){

        DatabaseReference ref = friendsReference.child(user.getUid());

        ref.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {

                    Friend f = child.getValue(Friend.class);
                    if(f != null) {
                        friendsList.add(">    " + f.name + "\n" + f.id);
                        friendsIDList.add(f.id);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void ensureDiscoverable(BluetoothAdapter bluetoothAdapter) {

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BT_DISCOVERABLE_TIME);
        startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == BT_DISCOVERABLE_TIME) {
                    //Toast.makeText(this,"Setup chat", Toast.LENGTH_SHORT).show();
                    setupChat();
                    addNewFriend();
                } else {
                   // Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    //finish();
                }
                break;
        }
    }

    private void connectDevice(Intent data, boolean secure) {

        String address = data.getExtras().getString(DeviceListActivity1.DEVICE_ADDRESS);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        try {
            chatService.connect(device, secure);
        } catch (Exception e) {
            Toast.makeText(this, "Error! Other user must click on + button.", Toast.LENGTH_LONG).show();
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500);
        }
    }


    private void addNewFriend() {
       // Log.d(TAG, "Friends addNewFriend started");
        Runnable r = new Runnable() {

            @Override
            public void run() {
                Intent serverIntent = new Intent(FriendsActivity.this, DeviceListActivity1.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            }
        };
        Thread btThread = new Thread(r);
        btThread.start();
    }


    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "MainActivity: handleMessage started");
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChatService.STATE_CONNECTED:
                            Log.d(TAG, "MainActivity: handleMessage MESSAGE_STATE_CHANGE STATE_CONNECTED");     //for new devices
                            sendFriendRequest();
                            break;
                        case ChatService.STATE_CONNECTING:
                            Log.d(TAG, "MainActivity: handleMessage MESSAGE_STATE_CHANGE STATE_CONNECTING");    //for paired devices??
                            sendFriendRequest();
                            break;
                        case ChatService.STATE_LISTEN:
                        case ChatService.STATE_NONE:
                            Log.d(TAG, "MainActivity: handleMessage MESSAGE_STATE_CHANGE STATE_NONE");
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    Log.d(TAG, "MainActivity: handleMessage MESSAGE_WRITE");
                    byte[] writeBuf = (byte[]) msg.obj;

                    String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    Log.d(TAG, "MainActivity: handleMessage MESSAGE_READ");
                    byte[] readBuf = (byte[]) msg.obj;

                    String readMessage = new String(readBuf, 0, msg.arg1);


                    Log.d(TAG, "readMessage:" + readMessage);


                    final String message = readMessage;

                    int _char = message.lastIndexOf("_");
                    String messageCheck = message.substring(0, _char + 1);
                    final String friendsUid = message.substring(_char + 1);


                    runOnUiThread(new Runnable() {
                        public void run() {
                            new AlertDialog.Builder(FriendsActivity.this)
                                    .setTitle("FRIEND REQUEST")
                                    .setMessage("Are you sure you want to become friends with a device " + connectedDeviceName + "?")
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {

                                            String idReceive = message;
                                            //Toast.makeText(FriendsActivity.this, "Your friend id: " + message, Toast.LENGTH_LONG).show();
                                            //contains(idReceive);
                                            if(contains(idReceive)){
                                                Toast.makeText(FriendsActivity.this, "You are already friends", Toast.LENGTH_LONG).show();
                                            }
                                            else {

                                                saveNewFriend(idReceive);
                                            }

                                       //     adapter.notifyDataSetChanged();
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            Toast.makeText(FriendsActivity.this, "You declined friend request", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                    });
                    //}
                    break;
                case MESSAGE_DEVICE_NAME:
                    //Log.d(TAG, "MainActivity: handleMessage MESSAGE_DEVICE_NAME");
                    connectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + connectedDeviceName, Toast.LENGTH_LONG).show();
                    break;

                case MESSAGE_TOAST:
                   // Log.d(TAG, "MainActivity: handleMessage MESSAGE_TOAST");
                    //  Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    public boolean contains(String id) {

        boolean yes = false;

        yes = friendsIDList.contains(id);

        return yes;
    }

    // BITNO!!!
    private void sendFriendRequest() {
        String message = idUser;
        Log.d(TAG, "MainActivity: addNewFriend sendingMessage:" + message);
        sendMessage(message);
    }


    private void sendMessage(String message) {

        if (chatService.getState() != ChatService.STATE_CONNECTED) {
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            chatService.write(send);
            outStringBuffer.setLength(0);
        }
    }


    private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_NULL
                    && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    private boolean setupChat() {
        Log.d(TAG, "MainActivity: setupChat started");

        chatService = new ChatService(this, handler);

        outStringBuffer = new StringBuffer("");

        if (chatService.getState() == ChatService.STATE_NONE) {
            chatService.start();
        }
        return true;
    }


}
