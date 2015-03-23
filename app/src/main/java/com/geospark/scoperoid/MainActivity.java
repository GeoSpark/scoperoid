// Copyright (c) 2015 GeoSpark
//
// Released under the MIT License (MIT)
// See the LICENSE file, or visit http://opensource.org/licenses/MIT

package com.geospark.scoperoid;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.math.BigDecimal;


public class MainActivity extends ActionBarActivity implements Scope.ScopeCallback {
    private static final String ACTION_USB_PERMISSION = "com.geospark.scoperoid.USB_PERMISSION";
    private static final String TAG = "MAIN";

    private static final BigDecimal NANO =  new BigDecimal("0.000000001");
    private static final BigDecimal MICRO = new BigDecimal("0.000001");
    private static final BigDecimal MILLI = new BigDecimal("0.001");
    private static final BigDecimal TIMEBASE_SCALAR = new BigDecimal("100");
    private static final BigDecimal VERTICAL_SCALE_SCALAR = new BigDecimal("25");
    private static final BigDecimal HORIZONTAL_DIVISIONS = new BigDecimal("6");

    private UsbManager mUsbManager;
    private WaveformView waveformView;
    private Scope _scope;

    private PendingIntent mPermissionIntent = null;

    private boolean _scopeRunning = true;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
                        View decorView = getWindow().getDecorView();
                        decorView.setSystemUiVisibility(uiOptions);

                        if (device != null) {
                            _scope.connectUSB(mUsbManager, device);
                            _scope.postCommand(Scope.WAV_SOURCE, "CHAN1");
                            _scope.postCommand(Scope.WAV_MODE, "NORM");
                            _scope.postCommand(Scope.WAV_FORMAT, "BYTE");
                            _scope.postCommand(Scope.WAV_DATA_Q);
                        }
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mUsbConnectedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (device != null && device.getVendorId() == 6833 && device.getProductId() == 1230) {
                        if (!mUsbManager.hasPermission(device)) {
                            mUsbManager.requestPermission(device, mPermissionIntent);
                        }
                    }
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null && device.getVendorId() == 6833 && device.getProductId() == 1230) {
                        _scope.disconnectUSB();
                    }
                }
            }
        }
    };

    @Override
    protected void onPause() {
        waveformView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        waveformView.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);

        waveformView = (WaveformView) findViewById(R.id.waveformView);

        _scope = new Scope();
        _scope.register(this);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

//        Intent intent = getIntent();
//        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//
//        if (device != null) {
//            connectUSB(device);
//            _refresh_handler.post(_refresh_runnable);
//        } else {
            registerReceiver(mUsbConnectedReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
            registerReceiver(mUsbConnectedReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

            mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
//        }
    }

    @Override
    protected void onDestroy() {
        _scope.unregister();
        unregisterReceiver(mUsbConnectedReceiver);
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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

        return super.onOptionsItemSelected(item);
    }

    public void onStartStopButton(View v) {
        // We appear to have no way of determining the run/stop state of the scope, so we'll have to assume it's running when we start the app.
        if (_scope != null) {
            if (_scopeRunning) {
                _scope.postCommand(Scope.STOP);
            } else {
                _scope.postCommand(Scope.RUN);
            }
            _scopeRunning = !_scopeRunning;
        }
    }

    @Override
    public void result(String command, byte[] data) {
        if (Scope.WAV_DATA_Q.equals(command) && data.length > 11) {
            waveformView.setWaveformData(data);
            _scope.postCommand(Scope.WAV_PREAMBLE_Q);
            _scope.postCommand(Scope.WAV_DATA_Q);
        } else if (Scope.WAV_PREAMBLE_Q.equals(command)) {
            try {
                String sdata = new String(data);
                String[] params = sdata.split(",");

                // The documentation says that YIncrement is the vertical scale divided by 25, so we factor that out. The lower limit is 5mV.
                BigDecimal vscale = new BigDecimal(params[Scope.WAV_PREAMBLE_YINCREMENT]).setScale(4, BigDecimal.ROUND_HALF_UP).multiply(VERTICAL_SCALE_SCALAR);
                TextView vscaleView = (TextView) findViewById(R.id.verticalScale);

                if (vscale.compareTo(BigDecimal.ONE) < 0.0) {
                    String s = String.format(getString(R.string.vscale), vscale.scaleByPowerOfTen(3), getString(R.string.millivolts));
                    vscaleView.setText(s);
                } else {
                    String s = String.format(getString(R.string.vscale), vscale, getString(R.string.volts));
                    vscaleView.setText(s);
                }

                // The documentation says that the XIncrement parameter is the timescale divided by 100, so we factor that out. The lower
                // limit is 5ns.
                BigDecimal hscale = new BigDecimal(params[Scope.WAV_PREAMBLE_XINCREMENT]).setScale(11, BigDecimal.ROUND_HALF_UP).multiply(TIMEBASE_SCALAR);
                TextView hscaleView = (TextView) findViewById(R.id.timebase);

                if (hscale.compareTo(MICRO) < 0) {
                    String s = String.format(getString(R.string.timebase), hscale.scaleByPowerOfTen(9), getString(R.string.nanoseconds));
                    hscaleView.setText(s);
                } else if (hscale.compareTo(MILLI) < 0) {
                    String s = String.format(getString(R.string.timebase), hscale.scaleByPowerOfTen(6), getString(R.string.microseconds));
                    hscaleView.setText(s);
                } else if (hscale.compareTo(BigDecimal.ONE) < 0) {
                    String s = String.format(getString(R.string.timebase), hscale.scaleByPowerOfTen(3), getString(R.string.milliseconds));
                    hscaleView.setText(s);
                } else {
                    String s = String.format(getString(R.string.timebase), hscale, getString(R.string.seconds));
                    hscaleView.setText(s);
                }

                BigDecimal XStart = hscale.multiply(HORIZONTAL_DIVISIONS);
                BigDecimal hoffset = new BigDecimal(params[Scope.WAV_PREAMBLE_XORIGIN]).setScale(12, BigDecimal.ROUND_HALF_UP).add(XStart);
                TextView hoffsetView = (TextView) findViewById(R.id.timeoffset);

                if (hoffset.abs().compareTo(NANO) < 0) {
                    String s = String.format(getString(R.string.timeoffset), hoffset.scaleByPowerOfTen(12), getString(R.string.picoseconds));
                    hoffsetView.setText(s);
                } else if (hoffset.abs().compareTo(MICRO) < 0) {
                    String s = String.format(getString(R.string.timeoffset), hoffset.scaleByPowerOfTen(9), getString(R.string.nanoseconds));
                    hoffsetView.setText(s);
                } else if (hoffset.abs().compareTo(MILLI) < 0) {
                    String s = String.format(getString(R.string.timeoffset), hoffset.scaleByPowerOfTen(6), getString(R.string.microseconds));
                    hoffsetView.setText(s);
                } else if (hoffset.abs().compareTo(BigDecimal.ONE) < 0) {
                    String s = String.format(getString(R.string.timeoffset), hoffset.scaleByPowerOfTen(3), getString(R.string.milliseconds));
                    hoffsetView.setText(s);
                } else {
                    String s = String.format(getString(R.string.timeoffset), hoffset, getString(R.string.seconds));
                    hoffsetView.setText(s);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.w(TAG, "Incomplete data packet. Has the USB cable been unplugged?");
            }
        }
    }
}
