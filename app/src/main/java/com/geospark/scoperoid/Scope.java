// Copyright (c) 2015 GeoSpark
//
// Released under the MIT License (MIT)
// See the LICENSE file, or visit http://opensource.org/licenses/MIT

package com.geospark.scoperoid;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class Scope {
    public static final String TAG = "USBTMC";

    public static final String IEEE4882_CLS = "*CLS";
    public static final String IEEE4882_ESE = "*ESE";
    public static final String IEEE4882_ESE_Q = "*ESE?";
    public static final String IEEE4882_ESR_Q = "*ESR?";
    public static final String IEEE4882_IDN_Q = "*IDN?";
    public static final String IEEE4882_OPC = "*OPC";
    public static final String IEEE4882_OPC_Q = "*OPC?";
    public static final String IEEE4882_RST = "*RST";
    public static final String IEEE4882_SRE = "*SRE";
    public static final String IEEE4882_SRE_Q = "*SRE?";
    public static final String IEEE4882_STB_Q = "*STB?";
    public static final String IEEE4882_TST_Q = "*TST?";
    public static final String IEEE4882_WAI = "*WAI";

    public static final String RUN = ":RUN";
    public static final String STOP = ":STOP";

    public static final String WAV_SOURCE = ":WAV:SOUR";
    public static final String WAV_MODE = ":WAV:MODE";
    public static final String WAV_FORMAT = ":WAV:FORM";
    public static final String WAV_DATA_Q = ":WAV:DATA?";
    public static final String WAV_XINCREMENT_Q = ":WAV:XINC?";
    public static final String WAV_XORIGIN_Q = ":WAV:XOR?";
    public static final String WAV_XREFERENCE_Q = ":WAV:XREF?";
    public static final String WAV_YINCREMENT_Q = ":WAV:YINC?";
    public static final String WAV_YORIGIN_Q = ":WAV:YOR?";
    public static final String WAV_YREFERENCE_Q = ":WAV:YREF?";
    public static final String WAV_START = ":WAV:STAR";
    public static final String WAV_STOP = ":WAV:STOP";
    public static final String WAV_PREAMBLE_Q = ":WAV:PRE?";

    public static final String DISP_GBR = ":DISP:GBR";

    public static final int WAV_PREAMBLE_FORMAT = 0;
    public static final int WAV_PREAMBLE_TYPE = 1;
    public static final int WAV_PREAMBLE_POINTS = 2;
    public static final int WAV_PREAMBLE_COUNT = 3;
    public static final int WAV_PREAMBLE_XINCREMENT = 4;
    public static final int WAV_PREAMBLE_XORIGIN = 5;
    public static final int WAV_PREAMBLE_XREFERENCE = 6;
    public static final int WAV_PREAMBLE_YINCREMENT = 7;
    public static final int WAV_PREAMBLE_YORIGIN = 8;
    public static final int WAV_PREAMBLE_YREFERENCE = 9;

    private static final byte USBTMC_MSGID_DEV_DEP_MSG_OUT = 1;
    private static final byte USBTMC_MSGID_DEV_DEP_MSG_IN = 2;

    public interface ScopeCallback {
        void result(String command, byte[] data);
    }

    private UsbDeviceConnection _connection = null;
    private UsbEndpoint _endpoint_in = null;
    private UsbEndpoint _endpoint_out = null;
    private int _max_packet_size = 64;
    private byte _mbtag = 0;
    private ByteBuffer _packet_buffer;
    private ByteBuffer _result_buffer;

    private Queue<String> _command_queue = new ArrayDeque<>();

    ScopeCallback _result_callback = null;

    public void register(ScopeCallback cb) {
        _result_callback = cb;
    }

    public void unregister() {
        _result_callback = null;
    }

    public Scope() {
    }

    public void connectUSB(UsbManager mgr, UsbDevice device) {
        UsbInterface device_interface = device.getInterface(0);
        _connection = mgr.openDevice(device);

        if (_connection != null) {
            _connection.claimInterface(device_interface, true);
            _endpoint_in = device_interface.getEndpoint(1);
            _endpoint_out = device_interface.getEndpoint(2);
            _max_packet_size = _endpoint_in.getMaxPacketSize();
            _packet_buffer = ByteBuffer.allocate(_max_packet_size);
            _packet_buffer.order(ByteOrder.LITTLE_ENDIAN);
            _result_buffer = ByteBuffer.allocate(4096);
        }
    }

    public void disconnectUSB() {
        if (_connection != null) {
            _connection.close();
            _connection = null;
        }
    }

    void postCommand(String command, String... params) {
        StringBuilder sb = new StringBuilder();
        sb.append(command);

        for (String param : params) {
            sb.append(' ');
            sb.append(param);
        }

        _command_queue.add(sb.toString());
//        Log.d("USBTMC >>>", command);

        if (_command_queue.size() == 1) {
            new GetDataTask().execute();
        }
    }

    private class GetDataTask extends AsyncTask<Void, Void, byte[]> {
        private static final int USB_TIMEOUT = 1000;

        // Assumes for now command <= 52 bytes long.
        protected void send_command(String command) {
            _mbtag = (byte) ((_mbtag % 255) + 1);
            _packet_buffer.clear();
            _packet_buffer.put(USBTMC_MSGID_DEV_DEP_MSG_OUT);
            _packet_buffer.put(_mbtag);
            _packet_buffer.put((byte) (_mbtag ^ 0xff));
            _packet_buffer.put((byte) 0x00);

            _packet_buffer.putInt(command.length());
            _packet_buffer.put((byte) 0x01); // EoM
            _packet_buffer.put((byte) 0x00);
            _packet_buffer.put((byte) 0x00);
            _packet_buffer.put((byte) 0x00);

            _packet_buffer.put(command.getBytes());
            int length_padded = (_packet_buffer.position() + 3) - ((_packet_buffer.position() - 1) % 4);

            if (_connection != null) {
                _connection.bulkTransfer(_endpoint_out, _packet_buffer.array(), length_padded, USB_TIMEOUT);
            }
        }

        protected void request_data(int size) {
            _mbtag = (byte) ((_mbtag % 255) + 1);
            _packet_buffer.clear();
            _packet_buffer.put(USBTMC_MSGID_DEV_DEP_MSG_IN);
            _packet_buffer.put(_mbtag);
            _packet_buffer.put((byte) (_mbtag ^ 0xff));
            _packet_buffer.put((byte) 0x00);

            _packet_buffer.putInt(size);
            _packet_buffer.put((byte) 0x01);
            _packet_buffer.put((byte) 0x00);
            _packet_buffer.put((byte) 0x00);
            _packet_buffer.put((byte) 0x00);

            if (_connection != null) {
                _connection.bulkTransfer(_endpoint_out, _packet_buffer.array(), 12, USB_TIMEOUT);
            }
        }

        protected boolean receive_buffer(ByteBuffer bytes) {
            request_data(_max_packet_size - 12);

            _packet_buffer.clear();

            if (_connection == null) {
                return false;
            }

            int ret_size = _connection.bulkTransfer(_endpoint_in, _packet_buffer.array(), _max_packet_size, USB_TIMEOUT);

            if (ret_size > 0) {
                _packet_buffer.position(ret_size);
                _packet_buffer.flip();
                byte msgid = _packet_buffer.get();
                byte btag = _packet_buffer.get();
                byte btaginv = _packet_buffer.get();
                byte unused = _packet_buffer.get();

                int xfer_size = _packet_buffer.getInt();
                byte xfer_attr = _packet_buffer.get();
                unused = _packet_buffer.get();
                unused = _packet_buffer.get();
                unused = _packet_buffer.get();
                boolean eom = (xfer_attr & 0x01) == 1;

                bytes.put(_packet_buffer);
                return !eom;
            } else {
                return false;
            }
        }

        @Override
        protected byte[] doInBackground(Void... params) {
            String command = _command_queue.peek();
            send_command(command);

            if (command.endsWith("?")) {
                _result_buffer.clear();
                while (receive_buffer(_result_buffer)) {}

                return Arrays.copyOf(_result_buffer.array(), _result_buffer.position());
            }

            return null;
        }

        @Override
        protected void onPostExecute(byte[] result) {
            String command = _command_queue.remove();
//            Log.d("USBTMC <<<", command);

            if (_command_queue.size() > 0) {
                new GetDataTask().execute();
            }

            if (_result_callback != null) {
                _result_callback.result(command, result);
            }
        }
    }
}
