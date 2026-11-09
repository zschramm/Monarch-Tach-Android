// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Monarch Utility contributors
package com.lamalo.monarch;

import android.app.*;
import android.content.*;
import android.hardware.usb.*;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import com.hoho.android.usbserial.driver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
 private static final String PERMISSION="com.lamalo.monarch.USB";
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private UsbManager manager; private UsbSerialPort port; private UsbDevice selected;
 private Protocol protocol; private BigDecimal zero,full; private Protocol.Config reviewed; private volatile boolean writeAttempted=false;
 private TextView status,current,details,usbInfo,diagnostics; private final StringBuilder diagnosticLog=new StringBuilder(); private EditText z,f,ppr; private Spinner decimal,mode; private Button decimalApply,modeApply,pprApply;
 private Button connect,read,wz,wf; private volatile boolean busy=false,dead=false;
 private boolean ready=false,inForeground=false,permissionGranted=false; private Intent permissionIntent; private UsbDevice pending; private final Handler timer=new Handler(Looper.getMainLooper()); private boolean live=false; private TextView displayValue; private Button displayRead,liveButton; private String units="unknown/custom units"; private final Runnable poll=new Runnable(){public void run(){if(!live||dead)return;if(!busy&&ready)readDisplay();timer.postDelayed(this,1000);}};
 private final BroadcastReceiver receiver=new BroadcastReceiver(){ public void onReceive(Context c,Intent i){
  if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(i.getAction())){UsbDevice d=i.getParcelableExtra(UsbManager.EXTRA_DEVICE);if(d!=null&&pending!=null&&d.getDeviceId()==pending.getDeviceId()){clearPending();show("USB unplugged during permission request. Reconnect and try again.");}if(d!=null&&selected!=null&&d.getDeviceId()==selected.getDeviceId()){ready=false;render();boolean uncertain=writeAttempted;worker.execute(()->{close();show(uncertain?"USB disconnected during write. Outcome uncertain; reconnect and read before deciding.":"USB disconnected. Reconnect to read settings.");});}}
  else if(PERMISSION.equals(i.getAction())){UsbDevice d=i.getParcelableExtra(UsbManager.EXTRA_DEVICE);if(pending==null||permissionIntent==null||!permissionIntent.filterEquals(i)||d==null||pending.getDeviceId()!=d.getDeviceId())return;permissionGranted=i.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false)&&manager.hasPermission(d);updateUsbInfo();log("USB permission result: "+permissionGranted);if(permissionGranted)resumeGrantedRequest();else{clearPending();show("USB permission denied. Tap Connect to try again.");}}

 }};
 @Override public void onCreate(Bundle b){super.onCreate(b);manager=(UsbManager)getSystemService(USB_SERVICE);
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(24,24,24,24);ScrollView scroll=new ScrollView(this);scroll.addView(box);scroll.setOnApplyWindowInsetsListener((v,insets)->{box.setPadding(24+insets.getSystemWindowInsetLeft(),24+insets.getSystemWindowInsetTop(),24+insets.getSystemWindowInsetRight(),24+insets.getSystemWindowInsetBottom());return insets;});setContentView(scroll);
  label(box,"Monarch Utility",26);label(box,"ACT-1B  /  USB  /  115200 8N1",16);
  label(box,"Changes affect the analog output immediately. Make the connected system safe first. No hardware writes have been validated for this app yet.",16);
  status=label(box,"Connect the FTDI programming cable, then read settings.",16);connect=button(box,"Connect / choose USB",v->{if(pending!=null){clearPending();show("USB request cancelled. Tap Connect to try again.");}else choose();});read=button(box,"Read current settings",v->run(false,()->refresh()));
  usbInfo=label(box,"",14);button(box,"USB diagnostics",v->{updateUsbInfo();new AlertDialog.Builder(this).setTitle("USB diagnostics").setMessage(usbInfo.getText()+"\n\n"+diagnosticLog.toString()).setPositiveButton("Copy",(d,n)->((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Monarch USB diagnostics",usbInfo.getText()+"\n\n"+diagnosticLog.toString()))).setNegativeButton("Close",null).show();});
  current=label(box,"Current values: not read",20);label(box,"Zero Scale and Full Scale are readings in display units at the low and high ends of the installed analog output. Output type is not detected.",14);
  displayValue=label(box,"Remote display: not read",24);displayRead=button(box,"Read remote display",v->readDisplay());liveButton=button(box,"Start live display",v->{if(live)stopLive();else{live=true;liveButton.setText("Stop live display");timer.post(poll);}});
  label(box,"Analog Output - Full Scale (FSCAL)",18);f=input(box,"New full-scale reading in display units");wf=button(box,"Apply",v->confirm("FSCAL",f.getText().toString()));
  LinearLayout more=new LinearLayout(this);more.setOrientation(LinearLayout.VERTICAL);more.setVisibility(View.GONE);
  button(box,"Other programming options",v->more.setVisibility(more.getVisibility()==View.GONE?View.VISIBLE:View.GONE));box.addView(more);
  label(more,"Display decimal places",18);decimal=choices(more,new String[]{"NONE","1","2","3"});decimalApply=button(more,"Review decimal places",v->confirmOption("DECPT",decimal.getSelectedItem().toString()));
  label(more,"Measurement mode",18);mode=choices(more,new String[]{"RPM","FREQ"});modeApply=button(more,"Review measurement mode",v->confirmOption("TYPE",mode.getSelectedItem().toString()));
  label(more,"Pulses per revolution for RPM",18);ppr=input(more,"PPR, normally 1 (integer 1-999)");ppr.setInputType(InputType.TYPE_CLASS_NUMBER);pprApply=button(more,"Review pulses per revolution",v->confirmOption("PPR",ppr.getText().toString()));
  label(more,"PPR uses SCALE mode with 60/PPR. Values needing rounding beyond four decimals are disabled until device precision is verified. An existing custom SCALE value does not prove a PPR.",14);
  label(more,"Analog Output - Zero Scale (0SCAL)",18);z=input(more,"New zero-scale reading in display units");wz=button(more,"Apply 0SCAL",v->confirm("0SCAL",z.getText().toString()));
  label(box,"App guardrails: 0–99999, up to four decimals, 0SCAL < FSCAL. These are conservative app limits, not a verified manufacturer setter range. Endpoints use display units: RPM, Hz, or unknown/custom units in SCALE mode. Mode reviews explain related scale changes.",14);
  button(box,"About / licenses",v->new AlertDialog.Builder(this).setTitle("Monarch Utility 0.2.4").setMessage("Independent, unofficial ACT-1B utility. GPL-3.0-only, without warranty. Source accompanies this app. USB serial driver is MIT; AndroidX/Kotlin/Gradle components retain their own licenses. Full texts are packaged in assets/licenses and the source package.").setPositiveButton("GPL v3",(d,n)->showLicense("GPL-3.0.txt")).setNeutralButton("Third-party notices",(d,n)->showLicense("THIRD_PARTY_NOTICES.txt")).setNegativeButton("Close",null).show());
  details=label(box,"",14);details.setTextIsSelectable(true);
  IntentFilter filter=new IntentFilter(PERMISSION);filter.addDataScheme("monarch-usb-permission");
  androidx.core.content.ContextCompat.registerReceiver(this,receiver,filter,androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
  androidx.core.content.ContextCompat.registerReceiver(this,receiver,new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
  render();updateUsbInfo();handleUsbIntent(getIntent());
 }
 private TextView label(LinearLayout p,String t,int size){TextView v=new TextView(this);v.setText(t);v.setTextSize(size);v.setPadding(0,8,0,8);p.addView(v);return v;}
 private Spinner choices(LinearLayout p,String[] values){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));p.addView(s);return s;}
 private EditText input(LinearLayout p,String hint){EditText e=new EditText(this);e.setHint(hint);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);p.addView(e);return e;}
 private Button button(LinearLayout p,String t,View.OnClickListener l){Button b=new Button(this);b.setText(t);b.setOnClickListener(l);p.addView(b);return b;}
 private void render(){connect.setEnabled(!busy);connect.setText(pending==null?"Connect / choose USB":"Cancel USB request");read.setEnabled(!busy&&ready);wz.setEnabled(!busy&&ready&&zero!=null&&full!=null);wf.setEnabled(wz.isEnabled());z.setEnabled(!busy);f.setEnabled(!busy);boolean enabled=!busy&&ready&&reviewed!=null;decimalApply.setEnabled(enabled);modeApply.setEnabled(enabled);pprApply.setEnabled(enabled);decimal.setEnabled(!busy);mode.setEnabled(!busy);ppr.setEnabled(!busy);displayRead.setEnabled(!busy&&ready);liveButton.setEnabled(ready);}
 private void show(String s){runOnUiThread(()->{if(!dead){status.setText(s);render();}});}
 private void log(String message){runOnUiThread(()->{if(dead)return;diagnosticLog.append(message).append('\n');if(diagnosticLog.length()>6000)diagnosticLog.delete(0,diagnosticLog.length()-6000);});}
 private void updateUsbInfo(){StringBuilder text=new StringBuilder("USB host: "+getPackageManager().hasSystemFeature("android.hardware.usb.host"));Collection<UsbDevice> devices=manager.getDeviceList().values();text.append("\nDevices visible to Android: ").append(devices.size());for(UsbDevice d:devices){UsbSerialDriver driver=UsbSerialProber.getDefaultProber().probeDevice(d);text.append("\n").append(String.format(Locale.US,"%04X:%04X",d.getVendorId(),d.getProductId())).append(" / ").append(driver==null?"unsupported USB device":driver.getClass().getSimpleName()).append(" / permission ").append(manager.hasPermission(d)?"granted":"needed");}usbInfo.setText(text.toString());}
 private void clearPending(){pending=null;permissionIntent=null;permissionGranted=false;render();}
 private void resumeGrantedRequest(){if(!inForeground||!permissionGranted||pending==null||busy||dead)return;UsbDevice d=pending;clearPending();if(manager.hasPermission(d))open(d);else show("USB permission no longer available. Tap Connect.");}
 private void requestConnect(UsbDevice d){if(busy||pending!=null)return;updateUsbInfo();log("Selected USB "+String.format(Locale.US,"%04X:%04X",d.getVendorId(),d.getProductId()));pending=d;permissionGranted=manager.hasPermission(d);render();if(permissionGranted){resumeGrantedRequest();return;}show("Waiting for Android USB permission. Accept the USB dialog to connect.");try{permissionIntent=new Intent(PERMISSION).setPackage(getPackageName()).setData(android.net.Uri.parse("monarch-usb-permission://request/"+UUID.randomUUID()));int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=31)flags|=PendingIntent.FLAG_MUTABLE;manager.requestPermission(d,PendingIntent.getBroadcast(this,0,permissionIntent,flags));}catch(RuntimeException e){clearPending();show("USB permission request failed: "+e.getMessage());}}
 private void handleUsbIntent(Intent intent){if(intent!=null&&UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())){UsbDevice d=intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);if(d!=null){log("Android launched this app for attached USB device");requestConnect(d);}}}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleUsbIntent(intent);}
 private void choose(){updateUsbInfo();List<UsbSerialDriver> ds=UsbSerialProber.getDefaultProber().findAllDrivers(manager);ds.removeIf(d->!(d instanceof FtdiSerialDriver));if(ds.isEmpty()){show(manager.getDeviceList().isEmpty()?"Android currently reports no USB devices. See USB diagnostics.":"USB device detected, but no supported FTDI port. See USB diagnostics for VID:PID.");return;}if(ds.size()==1){requestConnect(ds.get(0).getDevice());return;}String[] names=new String[ds.size()];for(int n=0;n<names.length;n++){UsbDevice d=ds.get(n).getDevice();names[n]=String.format(Locale.US,"FTDI %04X:%04X",d.getVendorId(),d.getProductId())+" / "+d.getDeviceName();}new AlertDialog.Builder(this).setTitle("Select FTDI cable").setItems(names,(dialog,n)->requestConnect(ds.get(n).getDevice())).setNegativeButton("Cancel",null).show();}
 private void open(UsbDevice d){run(false,()->{close();log("Opening FTDI port at 115200 8N1");UsbSerialDriver driver=UsbSerialProber.getDefaultProber().probeDevice(d);if(!(driver instanceof FtdiSerialDriver))throw new IOException("Not FTDI");UsbDeviceConnection connection=manager.openDevice(d);if(connection==null)throw new IOException("Android could not open USB; check permission and close other serial apps");selected=d;port=driver.getPorts().get(0);try{port.open(connection);port.setParameters(115200,8,UsbSerialPort.STOPBITS_1,UsbSerialPort.PARITY_NONE);}catch(Exception e){connection.close();throw e;}log("USB port opened; reading ACT-1B identity");protocol=new Protocol(this::exchange);refresh();ready=true;});}
 private String exchange(String cmd)throws IOException {
  byte[] buffer=new byte[4096];if(port.read(buffer,80)>0)throw new IOException("Unexpected pending USB data; reconnect required");
  log("TX "+cmd);if(cmd.contains(" = "))writeAttempted=true;port.write((cmd+"\r\n").getBytes(StandardCharsets.US_ASCII),1500);
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();long deadline=SystemClock.elapsedRealtime()+3500,last=0;
  while(SystemClock.elapsedRealtime()<deadline){int n=port.read(buffer,100);if(n>0){bytes.write(buffer,0,n);last=SystemClock.elapsedRealtime();if(bytes.size()>16384)throw new IOException("Response too long");}else if(last>0&&SystemClock.elapsedRealtime()-last>=400){String raw=bytes.toString(StandardCharsets.US_ASCII.name());if(!raw.endsWith("\r")&&!raw.endsWith("\n"))throw new IOException("Incomplete response");log("RX "+raw.replace("\r","\\r").replace("\n","\\n"));return raw;}}
  log("RX before timeout: "+bytes.toString(StandardCharsets.US_ASCII.name()).replace("\r","\\r").replace("\n","\\n"));throw new IOException("Serial response timeout at 115200; see USB diagnostics");
 }
 private void refresh()throws IOException {String info=protocol.identify();String all=protocol.settings();Protocol.Config config=new Protocol.Config(all);BigDecimal a=protocol.read("0SCAL"),b=protocol.read("FSCAL");config.expect("0SCAL",a.toPlainString());config.expect("FSCAL",b.toPlainString());zero=a;full=b;reviewed=config;String modeNow=config.get("TYPE"),decNow=config.get("DECPT"),scaleNow=config.get("SCALE");units=OutputLabels.units(modeNow);runOnUiThread(()->{if(!dead){current.setText(OutputLabels.scaling(a,b,OutputLabels.units(modeNow))+"\nMode  "+modeNow+"   Decimals  "+decNow+"\nReported SCALE  "+scaleNow);details.setText(info+"\n\n"+all+"\n\nDIAG is displayed without interpretation; this app does not change it.");}});}
 private void confirm(String key,String input){final String value;try{value=Protocol.value(input);BigDecimal v=new BigDecimal(value);if(key.equals("0SCAL")?v.compareTo(full)>=0:v.compareTo(zero)<=0)throw new IllegalArgumentException("Require 0SCAL < FSCAL.");}catch(Exception e){show(e.getMessage());return;}BigDecimal before=key.equals("0SCAL")?zero:full;final Protocol.Config snapshot=reviewed;review("Apply "+key+"?",(key.equals("FSCAL")?"Full Scale (FSCAL)":"Zero Scale (0SCAL)")+": "+before.toPlainString()+" -> "+value+" "+units+"\n\nThis changes the instrument setting. Apply to confirm.",()->{protocol.applyScale(snapshot,key,value);refresh();});}
 private void confirmOption(String key,String input){final Protocol.Config snapshot=reviewed;try{String message;Job action;if(key.equals("DECPT")){message=snapshot.get("DECPT")+" -> "+input+" maximum display decimals";action=()->protocol.applyDecimal(snapshot,input);}else if(key.equals("TYPE")){message=snapshot.get("TYPE")+" -> "+input+"\n\n"+(input.equals("RPM")?"RPM uses the standard 1-pulse/revolution conversion. This replaces any multipulse/custom SCALE correction.":"Frequency displays input pulses per second (Hz); it is not shaft RPM. This replaces the existing scale conversion.")+"\n\nFSCAL and 0SCAL numeric values stay unchanged but now refer to the new display units. Review analog scaling afterward.";action=()->protocol.applyMode(snapshot,input);}else{String factor=Protocol.pprScale(input);message="Use "+Integer.parseInt(input)+" pulses/revolution for RPM\nSCALE factor = "+factor+"\nCurrent mode: "+snapshot.get("TYPE")+", reported SCALE: "+snapshot.get("SCALE")+"\n\nChanges to SCALE mode, then sets its factor. Display units become RPM. Analog endpoint numbers stay unchanged; review them afterward. Selecting standard RPM later resets the multipulse correction.";action=()->protocol.applyPpr(snapshot,input);}review("Review "+key,message,()->{action.execute();refresh();});}catch(Exception e){show(e.getMessage());}}
 private void review(String title,String message,Job action){stopLive();new AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Cancel",null).setPositiveButton("Apply",(d,n)->run(true,action)).show();}
 interface Job {void execute()throws Exception;}
 private void run(boolean write,Job job){if(busy||dead)return;busy=true;writeAttempted=false;render();show(write?"Writing once and checking readback…":"Reading…");worker.execute(()->{try{job.execute();show(write?"Applied and readback verified.":"Read complete. No settings changed.");}catch(Exception e){boolean uncertain=writeAttempted;log("ERROR "+e.getClass().getSimpleName()+": "+e.getMessage());close();show((uncertain?"Write stopped. Outcome may be partial or uncertain. Do not repeat blindly; reconnect and read. ":"No settings written. ")+e.getMessage());}finally{writeAttempted=false;busy=false;runOnUiThread(()->{if(!dead)render();});}});}
 private void close(){runOnUiThread(this::stopLive);ready=false;zero=null;full=null;reviewed=null;if(port!=null)try{port.close();}catch(IOException ignored){}port=null;protocol=null;runOnUiThread(()->{if(!dead){displayValue.setText("Remote display: stale / not read");current.setText("Current values: stale / not verified");render();}});}
 private void showLicense(String name){try(InputStream in=getAssets().open("licenses/"+name);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);TextView text=new TextView(this);text.setText(bytes.toString("UTF-8"));text.setTextIsSelectable(true);text.setPadding(24,24,24,24);ScrollView scroll=new ScrollView(this);scroll.addView(text);new AlertDialog.Builder(this).setTitle(name).setView(scroll).setPositiveButton("Close",null).show();}catch(IOException e){show("Cannot open license: "+e.getMessage());}}
 private void stopLive(){live=false;timer.removeCallbacks(poll);if(liveButton!=null)liveButton.setText("Start live display");}
 private void readDisplay(){run(false,()->{String value=protocol.display();String stamp=java.text.DateFormat.getTimeInstance().format(new Date());runOnUiThread(()->{if(!dead)displayValue.setText(value+" "+units+"\nRead at "+stamp);});});}
 @Override protected void onResume(){super.onResume();inForeground=true;updateUsbInfo();resumeGrantedRequest();}
 @Override protected void onPause(){inForeground=false;stopLive();super.onPause();}
 @Override protected void onDestroy(){dead=true;unregisterReceiver(receiver);worker.execute(this::close);worker.shutdown();super.onDestroy();}
}










