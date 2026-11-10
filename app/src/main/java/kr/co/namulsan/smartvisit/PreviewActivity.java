package kr.co.namulsan.smartvisit;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.BitmapFactory;
import android.widget.*;
import java.io.File;

/** Fallback if overlay is not allowed or the screen was locked; still sends directly after approval. */
public final class PreviewActivity extends Activity {
    private String phone;
    private File imageFile;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        NotificationHelper.clear(this);
        Store.setReady(this,false);
        phone=Store.pendingNumber(this);
        LinearLayout root=Ui.column(this);
        root.setBackgroundColor(0xffF4F7FC);
        Ui.add(root,Ui.text(this,"통화 후 안내 전송",23,true,Ui.BLUE),3);
        Ui.add(root,Ui.text(this,"통화 종료 후 발송 여부를 선택해 주세요.",15,false,Ui.NAVY),8);
        Ui.add(root,Ui.text(this,"받는 고객번호: "+(Store.validNumber(phone)?phone:"확인되지 않음"),18,true,Ui.NAVY),12);
        try {
            imageFile=MessageImageComposer.create(this,Store.company(this),Store.name(this));
            ImageView img=new ImageView(this);
            img.setImageBitmap(BitmapFactory.decodeFile(imageFile.getAbsolutePath()));
            img.setAdjustViewBounds(true);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Ui.add(root,img,15);
        } catch(Exception e){
            Ui.add(root,Ui.text(this,"안내 이미지 준비 실패: "+e.getMessage(),14,true,0xffBC2222),8);
        }
        Button send=Ui.button(this,"보내기 · 바로 MMS 전송 요청",true);
        Ui.add(root,send,14);
        send.setOnClickListener(v->confirm());
        Button cancel=Ui.button(this,"보내지 않기",false);
        Ui.add(root,cancel,7);
        cancel.setOnClickListener(v->{
            if(Store.validNumber(phone)) Store.append(this,phone,"보내지 않음");
            Store.clearPending(this);finish();
        });
        Ui.add(root,Ui.text(this,"‘보내기’를 누르면 기본 메시지 앱을 열지 않고 전송을 시도합니다. MMS 전송 권한과 요금이 필요합니다.",12,false,0xff62748D),12);
        setContentView(Ui.scroll(this,root));
    }
    private void confirm(){
        if(!Store.validNumber(phone) || imageFile==null){
            Toast.makeText(this,"고객번호 또는 안내 이미지를 확인하지 못했습니다.",Toast.LENGTH_LONG).show();return;
        }
        if(Store.duplicate(this,phone)){
            new AlertDialog.Builder(this).setTitle("중복 발송 확인")
               .setMessage("이미 같은 번호로 전송을 요청했습니다. 그래도 다시 보내시겠습니까?")
               .setNegativeButton("취소",null)
               .setPositiveButton("다시 보내기",(d,w)->send()).show();
        } else send();
    }
    private void send(){
        try {
            DirectMmsSender.requestSend(this,phone);
            Store.append(this,phone,"MMS 전송 요청");
            Store.clearPending(this);
            Toast.makeText(this,"MMS 전송 요청. 실제 성공 여부는 이력에서 확인하세요.",Toast.LENGTH_LONG).show();
            finish();
        } catch(Exception e){
            new AlertDialog.Builder(this).setTitle("전송 요청 실패").setMessage(e.getMessage())
                .setPositiveButton("확인",null).show();
        }
    }
}
