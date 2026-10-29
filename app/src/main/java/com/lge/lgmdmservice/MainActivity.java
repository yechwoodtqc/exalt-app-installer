package com.lge.lgmdmservice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int PICK_APK=1201;
    private boolean pickerOpen=false;
    private static final String PREF="installer_preferences", KEY="access_code", DEFAULT="ys90", OTP_KEY="totp_secret";
    private static final int BG=Color.rgb(12,16,30), PANEL=Color.rgb(21,27,47),
        LIGHT=Color.rgb(30,38,63), CYAN=Color.rgb(83,232,214),
        TEXT=Color.rgb(241,245,255), MUTED=Color.rgb(166,177,203);
    private boolean unlocked=false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(Build.VERSION.SDK_INT>=21){getWindow().setStatusBarColor(Color.rgb(8,11,22));getWindow().setNavigationBarColor(Color.rgb(8,11,22));}
        showLock();
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable shape(int fill,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    private LinearLayout root(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(dp(14),dp(10),dp(14),dp(10));l.setBackgroundColor(BG);return l;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout.LayoutParams full(){return new LinearLayout.LayoutParams(-1,-2);}
    private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(fg);b.setBackground(shape(bg,10,bg));if(Build.VERSION.SDK_INT>=21)b.setStateListAnimator(null);return b;}
    private void showLock(){
        unlocked=false; LinearLayout r=root();r.setGravity(Gravity.CENTER);r.setPadding(dp(22),dp(18),dp(22),dp(18));
        TextView mark=text("YW",24,CYAN,true);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(LIGHT,16,CYAN));
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(dp(58),dp(58));mp.gravity=Gravity.CENTER_HORIZONTAL;r.addView(mark,mp);
        TextView title=text("App Installer",21,TEXT,true);title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=full();tp.topMargin=dp(10);r.addView(title,tp);
        boolean totpEnabled=!getSharedPreferences(PREF,0).getString(OTP_KEY,"").isEmpty();
        TextView hint=text(totpEnabled?"Enter your 6-digit authenticator code":"Enter your access code",12,MUTED,false);hint.setGravity(Gravity.CENTER);LinearLayout.LayoutParams hp=full();hp.topMargin=dp(4);r.addView(hint,hp);
        EditText code=new EditText(this);code.setSingleLine(true);code.setTextColor(TEXT);code.setHintTextColor(MUTED);code.setHint(totpEnabled?"123456":"Access code");code.setInputType(totpEnabled?android.text.InputType.TYPE_CLASS_NUMBER:android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);code.setPadding(dp(12),0,dp(12),0);code.setBackground(shape(PANEL,10,LIGHT));LinearLayout.LayoutParams cp=full();cp.topMargin=dp(16);r.addView(code,cp);
        Button enter=button("Unlock",CYAN,BG);LinearLayout.LayoutParams ep=full();ep.topMargin=dp(8);r.addView(enter,ep);
        enter.setOnClickListener(v->{SharedPreferences p=getSharedPreferences(PREF,0);String secret=p.getString(OTP_KEY,"");boolean valid=secret.isEmpty()?p.getString(KEY,DEFAULT).equals(code.getText().toString()):TotpAuthenticator.verify(secret,code.getText().toString(),System.currentTimeMillis());if(valid){unlocked=true;showHome();}else{code.setError(secret.isEmpty()?"Incorrect code":"Invalid or expired code");code.setText("");}});
        code.setOnEditorActionListener((v,id,event)->{enter.performClick();return true;});
        setContentView(r);
    }
    private void showHome(){
        LinearLayout r=root();LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("YW App Installer",18,TEXT,true),new LinearLayout.LayoutParams(0,-2,1));
        Button settings=button("Settings",LIGHT,TEXT);settings.setTextSize(12);header.addView(settings,new LinearLayout.LayoutParams(-2,dp(34)));settings.setOnClickListener(v->settings());r.addView(header);
        TextView sub=text("Install or update an APK",12,MUTED,false);LinearLayout.LayoutParams sp=full();sp.topMargin=dp(2);r.addView(sub,sp);
        LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(14),dp(13),dp(14),dp(13));card.setBackground(shape(PANEL,15,LIGHT));LinearLayout.LayoutParams cardp=full();cardp.topMargin=dp(12);r.addView(card,cardp);
        TextView badge=text("APK",12,CYAN,true);badge.setGravity(Gravity.CENTER);badge.setBackground(shape(LIGHT,7,CYAN));card.addView(badge,new LinearLayout.LayoutParams(dp(46),dp(28)));
        TextView heading=text("Install application",16,TEXT,true);LinearLayout.LayoutParams h=full();h.topMargin=dp(8);card.addView(heading,h);
        TextView desc=text("Choose an APK from device storage. Android handles package checks and any required confirmation.",12,MUTED,false);LinearLayout.LayoutParams dp=full();dp.topMargin=dp(4);card.addView(desc,dp);
        Button choose=button("Choose APK file",CYAN,BG);LinearLayout.LayoutParams bp=full();bp.topMargin=dp(12);card.addView(choose,bp);choose.setOnClickListener(v->pickApk());
        TextView pkg=text("com.lge.lgmdmservice  •  Android 6+",10,MUTED,false);pkg.setGravity(Gravity.CENTER);LinearLayout.LayoutParams pp=full();pp.topMargin=dp(10);r.addView(pkg,pp);setContentView(r);
    }
    private EditText codeField(String hint){
        EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);
        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setTextColor(Color.rgb(25,31,48));e.setHintTextColor(Color.rgb(105,115,135));
        e.setPadding(dp(12),dp(8),dp(12),dp(8));e.setBackground(shape(Color.WHITE,9,Color.rgb(180,190,210)));
        e.setSelectAllOnFocus(true);
        e.setOnFocusChangeListener((v,focused)->e.setBackground(shape(Color.WHITE,9,focused?Color.rgb(0,145,160):Color.rgb(180,190,210))));
        return e;
    }
    private void settings(){
        SharedPreferences prefs=getSharedPreferences(PREF,0);
        boolean enabled=!prefs.getString(OTP_KEY,"").isEmpty();
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(18),dp(8),dp(18),dp(4));
        TextView info=text(enabled
                ?"Authenticator app is enabled. The app now uses rotating 6-digit codes at sign-in."
                :"Set up an authenticator app to replace the access code with rotating 6-digit codes.",
                13,Color.rgb(45,54,75),false);
        panel.addView(info,full());
        Button setup=button(enabled?"Reconfigure authenticator":"Set up authenticator",Color.rgb(0,145,160),Color.WHITE);
        LinearLayout.LayoutParams bp=full();bp.topMargin=dp(14);panel.addView(setup,bp);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Security settings").setView(panel)
                .setNegativeButton("Close",null).create();
        setup.setOnClickListener(v->{dialog.dismiss();beginAuthenticatorSetup();});
        dialog.show();
    }
    private void beginAuthenticatorSetup(){
        SharedPreferences prefs=getSharedPreferences(PREF,0);
        String existing=prefs.getString(OTP_KEY,"");
        String secret=TotpAuthenticator.newSecret();
        String account=getPackageName();
        String uri="otpauth://totp/"+account+"?secret="+secret+"&issuer=YW%20App%20Installer&digits=6&period=30";
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(18),dp(8),dp(18),dp(4));
        TextView instructions=text("1. Open your authenticator app and choose Add account / Enter setup key.\n2. Account: YW App Installer\n3. Enter this key:",13,Color.rgb(45,54,75),false);
        panel.addView(instructions,full());
        TextView key=text(secret,18,Color.rgb(0,110,125),true);key.setGravity(Gravity.CENTER);key.setPadding(dp(8),dp(12),dp(8),dp(12));key.setBackground(shape(Color.rgb(230,249,248),9,Color.rgb(0,145,160)));
        LinearLayout.LayoutParams kp=full();kp.topMargin=dp(8);panel.addView(key,kp);
        Button copy=button("Copy setup key",LIGHT,TEXT);LinearLayout.LayoutParams cop=full();cop.topMargin=dp(6);panel.addView(copy,cop);
        copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("Authenticator setup key",secret));toast("Setup key copied.");});
        TextView verifyHint=text("Then enter the current 6-digit code shown by your authenticator app to finish setup.",12,Color.rgb(65,75,95),false);
        LinearLayout.LayoutParams vh=full();vh.topMargin=dp(12);panel.addView(verifyHint,vh);
        EditText otp=codeField("6-digit verification code");otp.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);otp.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(6)});
        LinearLayout.LayoutParams op=full();op.topMargin=dp(8);panel.addView(otp,op);
        TextView uriText=text("Manual setup URI: "+uri,10,Color.rgb(90,100,120),false);uriText.setTextIsSelectable(true);LinearLayout.LayoutParams up=full();up.topMargin=dp(8);panel.addView(uriText,up);
        AlertDialog setupDialog=new AlertDialog.Builder(this).setTitle("Authenticator setup").setView(panel)
                .setPositiveButton("Verify and enable",null).setNegativeButton("Cancel",null).create();
        setupDialog.setOnShowListener(x->{setupDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(!TotpAuthenticator.verify(secret,otp.getText().toString(),System.currentTimeMillis())){
                otp.setError("Code not valid yet; check your device time");otp.requestFocus();return;
            }
            prefs.edit().putString(OTP_KEY,secret).apply();
            toast("Authenticator enabled. Future unlocks require a current 6-digit code.");
            setupDialog.dismiss();
        });});
        setupDialog.show();
    }
    private void pickApk(){
        if(pickerOpen)return;
        pickerOpen=true;
        Intent f=new Intent(Intent.ACTION_GET_CONTENT);f.addCategory(Intent.CATEGORY_OPENABLE);
        f.setType("application/vnd.android.package-archive");
        try{startActivityForResult(Intent.createChooser(f,"Choose APK file"),PICK_APK);}
        catch(Exception e){pickerOpen=false;toast("No file picker is available.");}
    }
    private void installApk(Uri uri){PackageInstaller.Session session=null;try{
        if(uri==null)throw new IllegalArgumentException("No APK selected.");String scheme=uri.getScheme();if(scheme!=null&&!"content".equalsIgnoreCase(scheme)&&!"file".equalsIgnoreCase(scheme))throw new IllegalArgumentException("Unsupported file location.");
        ContentResolver resolver=getContentResolver();InputStream input=resolver.openInputStream(uri);if(input==null)throw new IllegalStateException("Cannot read APK.");
        PackageInstaller pi=getPackageManager().getPackageInstaller();PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);int id=pi.createSession(params);session=pi.openSession(id);
        try(InputStream in=input;OutputStream out=session.openWrite("base.apk",0,-1)){byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);session.fsync(out);}
        Intent status=new Intent(this,InstallStatusReceiver.class);status.setAction("com.lge.lgmdmservice.INSTALL_STATUS."+id);int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=31)flags|=PendingIntent.FLAG_MUTABLE;PendingIntent piStatus=PendingIntent.getBroadcast(this,id,status,flags);session.commit(piStatus.getIntentSender());session.close();session=null;toast("APK submitted to Android installer.");
    }catch(Exception e){if(session!=null){try{session.abandon();}catch(Exception ignored){}try{session.close();}catch(Exception ignored){}}toast("Could not start installation: "+(e.getMessage()==null?"unknown error":e.getMessage()));}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==PICK_APK){pickerOpen=false;if(result==RESULT_OK&&data!=null&&data.getData()!=null)installApk(data.getData());}}
    @Override public void onBackPressed(){if(unlocked)new AlertDialog.Builder(this).setTitle("Lock app?").setMessage("Return to the access-code screen?").setPositiveButton("Lock",(d,w)->showLock()).setNegativeButton("Stay",null).show();else super.onBackPressed();}
}
