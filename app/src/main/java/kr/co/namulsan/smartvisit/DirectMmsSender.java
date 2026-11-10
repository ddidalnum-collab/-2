package kr.co.namulsan.smartvisit;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import com.google.android.mms.pdu_alt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Direct-send experiment. Requires SEND_SMS grant; does not use ACTION_SEND.
 * Real carrier MMS success is ONLY known from MMS result PendingIntent.
 */
final class DirectMmsSender {
    static final String TEXT="고객님, 방문 전 스마트싱스 연결 안내 이미지를 보내드립니다.";
    static void requestSend(Context c,String number) throws Exception {
        if(!Store.validNumber(number)) throw new IllegalArgumentException("전화번호 확인 필요");
        if(c.checkSelfPermission(Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("앱에서 문자 전송 권한을 먼저 허용해 주세요.");
        if(!c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING))
            throw new IllegalStateException("이 휴대폰은 MMS 기능을 지원하지 않습니다.");
        int subscriptionId=SubscriptionManager.getDefaultSmsSubscriptionId();
        if(subscriptionId==SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            throw new IllegalStateException("기본 문자 발송 SIM을 설정해 주세요.");
        File image=MessageImageComposer.create(c,Store.company(c),Store.name(c));
        byte[] jpeg;
        try(FileInputStream in=new FileInputStream(image);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int r;
            while((r=in.read(b))!=-1) out.write(b,0,r);
            jpeg=out.toByteArray();
        }
        SendReq req=new SendReq();
        req.setTo(new EncodedStringValue[]{new EncodedStringValue(number)});
        req.setSubject(new EncodedStringValue(TEXT));
        req.setMessageSize(jpeg.length+2048);
        PduBody body=new PduBody();
        PduPart img=new PduPart();
        img.setContentType("image/jpeg".getBytes(StandardCharsets.US_ASCII));
        img.setName("smartvisit.jpg".getBytes(StandardCharsets.US_ASCII));
        img.setContentLocation("smartvisit.jpg".getBytes(StandardCharsets.US_ASCII));
        img.setContentId("image".getBytes(StandardCharsets.US_ASCII));
        img.setData(jpeg);
        body.addPart(img);
        PduPart text=new PduPart();
        text.setContentType("text/plain".getBytes(StandardCharsets.US_ASCII));
        text.setName("text.txt".getBytes(StandardCharsets.US_ASCII));
        text.setContentLocation("text.txt".getBytes(StandardCharsets.US_ASCII));
        text.setContentId("text".getBytes(StandardCharsets.US_ASCII));
        text.setCharset(CharacterSets.UTF_8);
        text.setData(TEXT.getBytes(StandardCharsets.UTF_8));
        body.addPart(text);
        req.setBody(body);
        byte[] pdu=new PduComposer(c,req).make();
        if(pdu==null || pdu.length==0) throw new IOException("MMS 데이터 생성에 실패했습니다.");
        Uri uri=MmsPduProvider.write(c,pdu);
        Intent result=new Intent(c,MmsResultReceiver.class);
        result.setAction(MmsResultReceiver.ACTION);
        result.putExtra("phone",number);
        result.setData(uri);
        PendingIntent sent=PendingIntent.getBroadcast(c,(int)(System.currentTimeMillis()&0x7fffffff),result,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
        SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            .sendMultimediaMessage(c,uri,null,null,sent);
    }
}
