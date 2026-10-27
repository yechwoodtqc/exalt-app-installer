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
    private static final String PREF="installer_preferences", KEY="access_code", DEFAULT="ys90";
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
        TextView hint=text("Enter your access code",12,MUTED,false);hint.setGravity(Gravity.CENTER);LinearLayout.LayoutParams hp=full();hp.topMargin=dp(4);r.addView(hint,hp);
        EditText code=new EditText(this);code.setSingleLine(true);code.setTextColor(TEXT);code.setHintTextColor(MUTED);code.setHint("Access code");code.setInputType( android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);code.setPadding(dp(12),0,dp(12),0);code.setBackground(shape(PANEL,10,LIGHT));LinearLayout.LayoutParams cp=full();cp.topMargin=dp(16);r.addView(code,cp);
        Button enter=button("Unlock",CYAN,BG);LinearLayout.LayoutParams ep=full();ep.topMargin=dp(8);r.addView(enter,ep);
        enter.setOnClickListener(v->{String saved=getSharedPreferences(PREF,0).getString(KEY,DEFAULT);if(saved.equals(code.getText().toString())){unlocked=true;showHome();}else{code.setError("Incorrect code");code.setText("");}});
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
    private void settings(){
        EditText old=new EditText(this);old.setSingleLine(true);old.setHint("Current code");old.setInputType(129);
        EditText n=new EditText(this);n.setSingleLine(true);n.setHint("New code");n.setInputType(129);
        EditText confirm=new EditText(this);confirm.setSingleLine(true);confirm.setHint("Confirm new code");confirm.setInputType(129);
        LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(18),0,dp(18),0);form.addView(old,full());form.addView(n,full());form.addView(confirm,full());
        new AlertDialog.Builder(this).setTitle("Change access code").setView(form).setPositiveButton("Save",(d,w)->{
            SharedPreferences p=getSharedPreferences(PREF,0);String current=p.getString(KEY,DEFAULT),next=n.getText().toString();
            if(!current.equals(old.getText().toString()))toast("Current code is incorrect.");
            else if(next.trim().length()<4)toast("Use at least 4 characters.");
            else if(!next.equals(confirm.getText().toString()))toast("Codes do not match.");
            else{p.edit().putString(KEY,next).apply();toast("Access code updated.");}
        }).setNegativeButton("Cancel",null).show();
    }
    private void pickApk(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/vnd.android.package-archive");try{startActivityForResult(i,PICK_APK);}catch(Exception e){Intent f=new Intent(Intent.ACTION_GET_CONTENT);f.addCategory(Intent.CATEGORY_OPENABLE);f.setType("*/*");try{startActivityForResult(Intent.createChooser(f,"Select an APK"),PICK_APK);}catch(Exception ex){toast("No file picker is available.");}}}
    private void installApk(Uri uri){PackageInstaller.Session session=null;try{
        if(uri==null)throw new IllegalArgumentException("No APK selected.");String scheme=uri.getScheme();if(scheme!=null&&!"content".equalsIgnoreCase(scheme)&&!"file".equalsIgnoreCase(scheme))throw new IllegalArgumentException("Unsupported file location.");
        ContentResolver resolver=getContentResolver();InputStream input=resolver.openInputStream(uri);if(input==null)throw new IllegalStateException("Cannot read APK.");
        PackageInstaller pi=getPackageManager().getPackageInstaller();PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);int id=pi.createSession(params);session=pi.openSession(id);
        try(InputStream in=input;OutputStream out=session.openWrite("base.apk",0,-1)){byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);session.fsync(out);}
        Intent status=new Intent(this,InstallStatusReceiver.class);status.setAction("com.lge.lgmdmservice.INSTALL_STATUS."+id);int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=31)flags|=PendingIntent.FLAG_MUTABLE;PendingIntent piStatus=PendingIntent.getBroadcast(this,id,status,flags);session.commit(piStatus.getIntentSender());session.close();session=null;toast("APK submitted to Android installer.");
    }catch(Exception e){if(session!=null){try{session.abandon();}catch(Exception ignored){}try{session.close();}catch(Exception ignored){}}toast("Could not start installation: "+(e.getMessage()==null?"unknown error":e.getMessage()));}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==PICK_APK){if(result==RESULT_OK&&data!=null&&data.getData()!=null)installApk(data.getData());else toast("No APK selected.");}}
    @Override public void onBackPressed(){if(unlocked)new AlertDialog.Builder(this).setTitle("Lock app?").setMessage("Return to the access-code screen?").setPositiveButton("Lock",(d,w)->showLock()).setNegativeButton("Stay",null).show();else super.onBackPressed();}
}
