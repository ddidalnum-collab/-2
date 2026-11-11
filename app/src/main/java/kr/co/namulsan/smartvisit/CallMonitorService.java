package kr.co.namulsan.smartvisit;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.*;
import android.widget.*;

/** User-activated, visibly disclosed foreground call monitoring service.
 *  V4 trial: unlocked device, special draw-over-apps grant required.
 *  Never initiates a call or sends without pressing the Send button.
 */
public final class CallMonitorService extends Service {
    static final String CHANNEL="smartvisit_monitor_v3";
    static CallMonitorService active;
    private BroadcastReceiver receiver;
    private WindowManager wm;
    private View overlay;
    private boolean confirmDuplicate = false;

    static void start(Context c) {
        Intent i=new Intent(c,CallMonitorService.class);
        if(Build.VERSION.SDK_INT>=26) c.startForegroundService(i); else c.startService(i);
    }
    static void stop(Context c) { c.stopService(new Intent(c,CallMonitorService.class)); }
    static boolean showOnTop(Context c) {
        CallMonitorService service=active;
        if(service==null || !Store.enabled(c)) return false;
        if(!Settings.canDrawOverlays(service)) return false;
        KeyguardManager kg=(KeyguardManager)service.getSystemService(KEYGUARD_SERVICE);
        if(kg!=null && kg.isDeviceLocked()) return false;
        String number=Store.pendingNumber(c);
        if(!Store.validNumber(number)) return false;
        new Handler(Looper.getMainLooper()).post(()->service.showPrompt(number));
        return true;
    }
    @Override public void onCreate() {
        super.onCreate(); active=this;
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL,"SmartVisit 통화 안내 활성화",NotificationManager.IMPORTANCE_LOW));
        Intent main=new Intent(this,MainActivity.class);
        PendingIntent p=PendingIntent.getActivity(this,9,main,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("SmartVisit 사용 중")
            .setContentText("발신 통화 종료 후 안내 여부를 확인합니다.")
            .setContentIntent(p).setOngoing(true).build();
        startForeground(301,n);
        receiver=new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent intent) {
                if(TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction()))
                    new PhoneStateReceiver().onReceive(c,intent);
            }
        };
        IntentFilter f=new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(receiver,f,Context.RECEIVER_EXPORTED);
        else registerReceiver(receiver,f);
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
    }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        return START_STICKY;
    }
    @Override public IBinder onBind(Intent intent) {return null;}
    @Override public void onDestroy() {
        closeOverlay();
        if(receiver!=null) try{unregisterReceiver(receiver);}catch(Exception ignored){}
        if(active==this) active=null;
        super.onDestroy();
    }
    private GradientDrawable rounded(int color) {
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(Ui.px(this,18));return g;
    }
    private void showPrompt(String number) {
        if(!Store.enabled(this)||!Settings.canDrawOverlays(this)||wm==null) return;
        closeOverlay();confirmDuplicate=false;
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.px(this,22),Ui.px(this,22),Ui.px(this,22),Ui.px(this,22));
        root.setBackground(rounded(Color.WHITE));
        root.setElevation(Ui.px(this,20));
        Ui.add(root,Ui.text(this,"통화가 종료되었습니다",22,true,Ui.NAVY),0);
        Ui.add(root,Ui.text(this,"방문 전 스마트싱스 안내 이미지를 보낼까요?",16,false,0xff45556A),12);
        Ui.add(root,Ui.text(this,"받는 번호: "+number,18,true,Ui.BLUE),12);
        String n=Store.name(this).trim();
        String employee=Store.company(this)+"  "+(n.endsWith("프로")?n:n+" 프로")+(ProfilePhoto.exists(this)?"  ·  사진 포함":"");
        Ui.add(root,Ui.text(this,"안내 이미지: "+employee,13,false,0xff56667C),10);
        TextView warning=Ui.text(this,"",13,true,0xffBB3F21);
        Ui.add(root,warning,7);
        LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel=Ui.button(this,"보내지 않기",false),send=Ui.button(this,"보내기",true);
        LinearLayout.LayoutParams b1=new LinearLayout.LayoutParams(0,Ui.px(this,54),1);b1.rightMargin=Ui.px(this,8);
        buttons.addView(cancel,b1);buttons.addView(send,new LinearLayout.LayoutParams(0,Ui.px(this,54),1));
        Ui.add(root,buttons,16);
        Ui.add(root,Ui.text(this,"'보내기'를 누르면 별도의 문자 앱을 열지 않고 MMS 전송을 요청합니다. 요금이 발생할 수 있습니다.",12,false,0xff65758B),12);
        cancel.setOnClickListener(v->{Store.append(this,number,"보내지 않음");Store.clearPending(this);closeOverlay();});
        send.setOnClickListener(v->{
            if(!Store.enabled(this)||!Store.validNumber(number)) {warning.setText("전송 조건을 확인해 주세요.");return;}
            if(!number.equals(Store.pendingNumber(this))) {warning.setText("새 통화가 감지되었습니다. 이번 창은 닫아 주세요.");return;}
            if(Store.duplicate(this,number) && !confirmDuplicate) {
                confirmDuplicate=true;warning.setText("중복 발송 가능성이 있습니다. 다시 '보내기'를 눌러 확정해 주세요.");return;
            }
            send.setEnabled(false);
            try {
                DirectMmsSender.requestSend(this,number);
                Store.append(this,number,"MMS 전송 요청");
                Store.clearPending(this);closeOverlay();
                Toast.makeText(this,"MMS 전송을 요청했습니다. 발송 결과는 이력에서 확인해 주세요.",Toast.LENGTH_LONG).show();
            } catch(Exception ex) {
                send.setEnabled(true);
                warning.setText("발송 요청 실패: "+ex.getMessage());
                Store.append(this,number,"발송 요청 실패");
            }
        });
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(
            Math.min(getResources().getDisplayMetrics().widthPixels-Ui.px(this,36),Ui.px(this,420)),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.CENTER;
        try {wm.addView(root,lp);overlay=root;} catch(Exception ignored) { NotificationHelper.show(this); }
    }
    private void closeOverlay() {
        if(wm!=null && overlay!=null) try {wm.removeView(overlay);}catch(Exception ignored){}
        overlay=null;
    }
}
