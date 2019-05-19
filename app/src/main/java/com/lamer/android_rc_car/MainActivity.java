package com.lamer.android_rc_car;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.VelocityTracker;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.android_rc_car.R;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static com.lamer.android_rc_car.CommandService.DEVICE_NAME;
import static com.lamer.android_rc_car.CommandService.MESSAGE_DEVICE_NAME;
import static com.lamer.android_rc_car.CommandService.MESSAGE_STATE_CHANGE;
import static com.lamer.android_rc_car.CommandService.MESSAGE_TOAST;
import static com.lamer.android_rc_car.CommandService.TOAST;


public class MainActivity extends Activity {

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

	ImageView wheel=null;
	//JoystickView joystick= null;
	//ArduinoModule car=null;
	boolean mouse=false;
	float st_angle=95;
	double d_angle=0;
	static int int_angle=95;//55 - 95 - 135
	static int int_acel=100;
	private VelocityTracker mVelocityTracker = null;
	private Timer mTimer;
	private Timer connectTimer;
	private TimerTask mTimerTask;
	private SurfaceView area;
	private float centerX;
	private float centerY;
	private Timer rTimer;
	private TimerTask rTimerTask;
	private boolean returnToZero = false;

    private CommandService car = null;
    private BluetoothAdapter mBluetoothAdapter;
    private String mConnectedDeviceName;
    private TextView mTitle;
    private int mAccel = 0;
    private static final int RETURN = 0;
    private static final int UP = 1;
    private static final int BREAK = 2;
    private boolean dataChanget = false;

    @Override
    protected void onStart() {
        Log.v("Activity","onStart");
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            if (car==null)
                car = new UdpCommandService(this,mHandler);
            //Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        // otherwise set up the command service
        else {
            if (car==null)
                setupCommand();
        }
    }

    private void setupCommand() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        car = new BluetoothCommandService(this, mHandler);
    }

    @Override
    protected void onDestroy() {
        Log.v("Activity","onDestroy");
        super.onDestroy();
        if (car != null) {
            car.stop();
            car=null;
        }
    }

    @Override
    protected void onResume() {
        Log.v("Activity","onResume");
        super.onResume();
        if (car != null) {
            if (car.getState() == BluetoothCommandService.STATE_NONE) {
                car.start();
                TimerStart();
            }
        }
    }

    private void connectToCar() {
        if(car instanceof BluetoothCommandService) {
            if (car.getState() != BluetoothCommandService.STATE_NONE && car.getState() != BluetoothCommandService.STATE_LISTEN)
                return;
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals("HC-06")) {
                        ((BluetoothCommandService) car).connect(device);
                    }
                }
            }
        }else if(car instanceof UdpCommandService) {
            ((UdpCommandService) car).connect();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Log.v("Activity","onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.activity_main);
        mTitle = (TextView) findViewById(R.id.mTitle);

        wheel=(ImageView)findViewById(R.id.whellView);

		area = (SurfaceView)findViewById(R.id.textureView);
		if(area!=null) {
			centerX = area.getWidth() >> 1;

			centerY = area.getHeight() >> 1;

            area.setOnTouchListener(new View.OnTouchListener() {
				private int acel_touch = -1;
				private int wheel_touch = -1;

				@Override
				public boolean onTouch(View v, MotionEvent m) {
					//Log.v("Touch","ACTION:"+event.getActionMasked()+" "+event.getPointerId(0)+" "+(event.getPointerCount()>1?event.getPointerId(1):"")+" "+event.getPointerCount());
//					int pointerCount = m.getPointerCount();
//
//					for (int i = 0; i < pointerCount; i++) {
//						int x = (int) m.getX(i);
//						int y = (int) m.getY(i);
//						int id = m.getPointerId(i);
//						int action = m.getActionMasked();
//						int actionIndex = m.getActionIndex();
//
//						String actionString;

						switch (m.getAction())
						{
							case MotionEvent.ACTION_POINTER_DOWN:
							case MotionEvent.ACTION_DOWN:
								//actionString = "DOWN";
//								if(wheel_touch == -1){
//									wheel_touch = id;
									centerX = m.getX();
									//actionString += " wheel";
                                    ReturnTimerStop();
								//}
//								else if(acel_touch == -1 && id == actionIndex){
//									acel_touch = id;
//									centerY = y;
//									actionString += " acel";
//								}
								break;
                            case MotionEvent.ACTION_POINTER_UP:
                            case MotionEvent.ACTION_UP:
								//if(wheel_touch == id){
								//	wheel_touch = -1;
									ReturnTimerStart();
								//}
//								if(acel_touch == id){
//									acel_touch = -1;
//								}
								//actionString = "UP";
								break;
//							case MotionEvent.ACTION_POINTER_UP:
//								if(wheel_touch == id && id == actionIndex){
//									wheel_touch = -1;
//									ReturnTimerStart();
//								}
////								if(acel_touch == id && id == actionIndex){
////									acel_touch = -1;
////								}
//								actionString = "POINTER_UP";
//								break;
							case MotionEvent.ACTION_MOVE:
								//if(wheel_touch == id){
									SetAngle(95 + (int) ((m.getX() - centerX) * 0.4));
								//}
//								if(acel_touch == id){
//									SetSpeed(int_acel - (int) ((y - centerY) * 0.02));
//								}
								//actionString = "MOVE";
								break;
							default:
								//actionString = "";
						}
						//Log.v("Touch","ACTION:"+actionString+" Index: " + actionIndex + " ID: " + id + " X: " + x + " Y: " + y);
					//}
					return true;
				}
			});
			//area.on
		}
        findViewById(R.id.accelView).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_POINTER_DOWN:
                    case MotionEvent.ACTION_DOWN:
                        mAccel = UP;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        mAccel = RETURN;
                        break;
                    case MotionEvent.ACTION_MOVE:
                    default:
                        break;
                }
                return true;
            }
        });
        findViewById(R.id.breakView).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_POINTER_DOWN:
                    case MotionEvent.ACTION_DOWN:
                        mAccel = BREAK;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        mAccel = RETURN;
                        break;
                    case MotionEvent.ACTION_MOVE:
                    default:
                        break;
                }
                return true;
            }
        });
        ((SeekBar) findViewById(R.id.seekBar1)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int_acel=progress;
                dataChanget=true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
	}

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothCommandService.STATE_CONNECTED:
                            //car.write("#1#"+(190-int_angle)+"#2#"+int_acel);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((ToggleButton)findViewById(R.id.toggleButton1)).setChecked(true);
                                }
                            });
                            mTitle.setText(R.string.title_connected_to);
                            mTitle.append("HC-06");
                            break;
                        case BluetoothCommandService.STATE_CONNECTING:
                            mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothCommandService.STATE_LISTEN:
                        case BluetoothCommandService.STATE_NONE:
                            mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                /*if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
                    // Attempt to connect to the device
                    car.connect(device);
                }*/
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupCommand();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

	int whill_touch_id=-1;

	public void TimerStart()
	{
		if(mTimer==null)
		{
		    mTimer=new Timer();
			mTimer.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        if(car==null){
                            Log.v("mTimer","CANCEL");
                            mTimer.cancel();
                            mTimer=null;
                            return;
                        }
                        if(car.getState()==BluetoothCommandService.STATE_CONNECTED && dataChanget){
                            Log.v("mTimer","WRITE");
                            car.write("#1#"+(190-int_angle)+"#2#"+int_acel);
                            dataChanget=false;
                        }
                    }
                },10,10);
		}
		if(connectTimer==null){
		    connectTimer = new Timer();
		    connectTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(car==null){
                        connectTimer.cancel();
                        connectTimer=null;
                        return;
                    }
                    if(car.getState()!=BluetoothCommandService.STATE_CONNECTED)connectToCar();
                }
            },1000,1000);
        }
	}



	public void ReturnTimerStart()
	{
		returnToZero = true;
		if(rTimer==null)
		{
			rTimer = new Timer();
		}else{
			return;
		}
		if(rTimerTask == null)
		{
			rTimerTask = new TimerTask()
			{
                private int nAccel;

                @Override
				public void run()
				{
				    nAccel = ((SeekBar)findViewById(R.id.seekBar1)).getProgress();
				    switch (mAccel){
                        case RETURN:
                            if(int_acel!=nAccel){
                                if((int_acel-nAccel)/2.0>5) {
                                    SetSpeed((int) (nAccel+((int_acel-nAccel)/2.0)));
                                }else SetSpeed(nAccel);
                            }
                            break;
                        case UP:
                            if(int_acel<195){
                                SetSpeed((int) (int_acel+(200-int_acel)/2.0));
                            }else SetSpeed(200);
                            break;
                        case BREAK:
                            if(int_acel>0){
                                SetSpeed((int) (int_acel-int_acel/4.0));
                            }else SetSpeed(0);
                            break;
                    }
					if(!returnToZero)return;
					if((int_angle-95.0)/2.0>5) {
						SetAngle(95 + (int_angle - 95) / 2);
					}else{
						SetAngle(95);
					}
				}
			};
		}
		if(rTimer!=null && rTimerTask!=null)
			rTimer.schedule(rTimerTask,50,50);
	}
	private void ReturnTimerStop() {
		returnToZero = false;
	}
	public void SetSpeed(int a){
		if(a!=int_acel){
			int_acel=a;
            dataChanget = true;
			//((SeekBar)findViewById(R.id.seekBar1)).setProgress(a+100);
			//			try {
			//if(car.getState()!=BluetoothCommandService.STATE_CONNECTED)connectToCar();
		}
	}
	public void SetAngle(int a){
        if(a<55)a=55;
        if(a>135)a=135;
		if(a!=int_angle){
			int_angle=a;
            //if(car.getState()!=BluetoothCommandService.STATE_CONNECTED)connectToCar();
			runOnUiThread(new Runnable() {
				@SuppressLint("DefaultLocale")
				@Override
				public void run() {
					wheel.setRotation((int_angle-95)*2);
					//int acel=((SeekBar)findViewById(R.id.seekBar1)).getProgress();
					((ToggleButton)findViewById(R.id.toggleButton1)).setText(
							String.format("%d", int_angle - 95));
				}
			});
			dataChanget = true;
//			Matrix matrix = new Matrix();
//			wheel.setScaleType(ImageView.ScaleType.MATRIX);   //required
//			matrix.postRotate(((float) int_angle-95)*2,250,248);// wheel.getWidth()/2, wheel.getHeight()/2+150);
//			wheel.setImageMatrix(matrix);
		}
	}
	/*private final BroadcastReceiver BTReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
				if(car==null){
					car=new ArduinoModule();
				    car.create(getApplicationContext());
				}
			}else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
				if(car==null){
					car=new ArduinoModule();
				    car.create(getApplicationContext());
				}
			} else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)) {
				if(car!=null) {
                    car.closeBT();
                    car = null;
                }
			} else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
				if(car!=null) {
                    car.closeBT();
                    car = null;
                }
			}
		};
	};*/
}
