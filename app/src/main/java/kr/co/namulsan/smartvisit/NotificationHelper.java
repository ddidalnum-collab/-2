package kr.co.namulsan.smartvisit;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

final class NotificationHelper {
    private static final String CH="smart_visit_after_call";
    static void show(Context c) {
        if (Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return;
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm==null) return;
        NotificationChannel channel=new NotificationChannel(CH,"통화 종료 후 전송 확인",NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("방문 전 이미지 안내 발송 여부를 선택합니다.");nm.createNotificationChannel(channel);
        Intent i=new Intent(c,PreviewActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(c,200,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(c,CH)
            .setSmallIcon(android.R.drawable.ic_dialog_email).setContentTitle("고객 안내 이미지 전송 여부 확인")
            .setContentText("전화가 종료되었습니다. 전송할지 선택해 주세요.")
            .setContentIntent(pi).setAutoCancel(true).build();
        nm.notify(200,n);
    }
    static void clear(Context c) {
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm!=null) nm.cancel(200);
    }
}
