Scoperoid
=========
A simple second monitor for Rigol MSO1000Z/DS1000Z-series digital oscilloscopes and Android devices.

Caveats
-------
* I AM NOT RESPONSIBLE IF THIS BREAKS YOUR ANDROID DEVICE OR YOUR TEST EQUIPMENT, SENDS IT OUT OF
  CALIBRATION, VOIDS THE WARRANTY, OR RELEASES THE MAGIC BLUE SMOKE. IT IS BEING PROVIDED AS-IS.
* This has only been tested on a Rigol DS1054Z and an Android Nexus 6 running Lollipop 5.0.1
* It requires a "USB-on-the-go" adapter to enable the Android device to act as host, and this
  should ultimately plug in to the USB port on the *back* of the 'scope.
* Currently it only detects the 'scope when it is plugged in *after* the app has been launched.
* Due to a bug in Android (or maybe a misunderstanding on my part), the app will ask you for
  permission to access the 'scope every time you plug it in, regardless of whether you check the
  "remember permission" box.
* Only channel 1 is supported at the moment, but other channels are easy to add.
* It seems there is no way to determine the RUN/STOP mode the 'scope is currently in, so the
  RUN/STOP button on the phone provides no feedback, merely sends the command to the 'scope and
  assumes the 'scope is in RUN mode when the app is started.

TODO
----
* Implement remaining channels.
* Add channel statistics as selected from the left-side buttons.
* Detect the presence of the 'scope when the app is started.
* Add support for other 'scopes that use the USBTMC standard. Anyone with a bunch of 'scopes and
  a desire to write some Android code?
* Test on a wider variety of phones.
* See if reasonable throughput can be achieved via a UDB Bluetooth dongle.
* Experiment with the Ethernet interface. Does anyone have any information on this that isn't just
  "use the NI VISA driver"? My initial thoughts are just to throw data at it via a simple script
  and see what I get.

What I may do
-------------
It might be quite fun to record waveform data on the Android device, but as we can't gurantee we
get every part of the waveform due to USB bandwith and CPU speed, and there appears to be no way of
getting an absolute timestamp, this might only be useful as a series of frames rather than a
comntinuous signal. Maybe in rolling mode we could get something suitable for playback, but the
timebase for that has to be quite large so there will be signal bandwidth limits.
We could trigger the 'scope's record/playback mode and maybe suck out the data via that interface,
but I've not looked at that.

What I am unlikely to do
------------------------
This app was developed for fun, and so I can see the waveform without moving my head much when
probing hard-to-reach and fiddly pins. I have no intention of turning it into a full interface
to the 'scope unless someone pays me to do it. Of course, feel free to fork and enhance the app
if you want.

This code is released under the MIT license, so you can pretty much do what you like with it, but I
would like to reserve the right to put it on the Play Store at some point in the future. If you
have a burning issue with this, let me know and we can sort something out.