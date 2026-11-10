package kr.co.namulsan.smartvisit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public final class PreviewActivity extends Activity {
    private EditText phoneField;
    private boolean awaitingReturn=false;
    private String outgoingNumber="";
    private File imageFile;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        NotificationHelper.clear(this);
        Store.setReady(this,false);
        draw();
    }
    private void draw() {
        LinearLayout root=Ui.column(this); root.setBackgroundColor(0xffF4F7FC);
        Ui.add(root,Ui.text(this,"안내 이미지 확인",23,true,Ui.BLUE),1);
        Ui.add(root,Ui.text(this,"이미지와 고객번호를 확인한 뒤 전송 여부를 선택하세요.",14,false,0xff536179),6);
        Ui.label(root,"받는 분 전화번호",18);
        String phone=getIntent().getStringExtra("phone");
        if(phone==null)phone=Store.pendingNumber(this);
        phoneField=Ui.edit(this,phone,"전화번호");
        phoneField.setInputType(InputType.TYPE_CLASS_PHONE);Ui.add(root,phoneField,6);
        try {
            imageFile=MessageImageComposer.create(this,Store.company(this),Store.name(this));
            ImageView image=new ImageView(this);
            image.setImageBitmap(BitmapFactory.decodeFile(imageFile.getAbsolutePath()));
            image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setBackground(Ui.bg(0xffFFFFFF,12,this));Ui.add(root,image,12);
        } catch(Exception error) {
            Ui.add(root,Ui.text(this,"이미지 생성 오류: "+error.getMessage(),15,true,0xffB00020),10);
        }
        Button send=Ui.button(this,"이미지 문자 앱에서 전송",true);Ui.add(root,send,15);send.setOnClickListener(v->confirmAndShare());
        Button copy=Ui.button(this,"고객번호 복사",false);Ui.add(root,copy,6);copy.setOnClickListener(v->copyPhone());
        Button skip=Ui.button(this,"이번에는 보내지 않기",false);Ui.add(root,skip,6);skip.setOnClickListener(v->skip());
        Ui.add(root,Ui.text(this,"고객번호 자동입력 및 이미지 첨부는 문자 앱에 따라 다릅니다.\n문자 앱에서 받는 사람과 첨부 이미지를 확인하고 직접 전송해 주세요.",12,false,0xff66758F),14);
        setContentView(Ui.scroll(this,root));
    }
    private String number() {return Store.normalize(phoneField.getText().toString());}
    private void confirmAndShare() {
        if(!Store.validNumber(number())) { toast("받는 분 전화번호를 확인해 주세요.");return; }
        if(imageFile==null||!imageFile.isFile()) {toast("이미지를 준비하지 못했습니다.");return;}
        if(Store.duplicate(this,number())) {
            new AlertDialog.Builder(this).setTitle("중복 발송 주의")
                .setMessage(Store.duplicateHours(this)+"시간 이내에 같은 고객에게 발송했다고 기록되어 있습니다. 다시 진행할까요?")
                .setNegativeButton("취소",null)
                .setPositiveButton("그래도 진행",(d,w)->shareImage()).show();
        } else shareImage();
    }
    private void shareImage() {
        String number=number();
        Uri uri=ImageProvider.URI;
        String caption="고객님, 방문 전 스마트싱스 연결 안내 이미지를 보내드립니다.";
        Intent share=new Intent(Intent.ACTION_SEND);
        share.setType("image/jpeg");
        share.putExtra(Intent.EXTRA_STREAM,uri);
        share.putExtra(Intent.EXTRA_TEXT,caption);
        share.putExtra("sms_body",caption);
        share.putExtra("address",number); // Optional, ignored by some messaging apps.
        share.setClipData(ClipData.newUri(getContentResolver(),"고객 안내 이미지",uri));
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String defaultSms=Telephony.Sms.getDefaultSmsPackage(this);
        if(defaultSms!=null&&!defaultSms.isEmpty()) {
            Intent direct=new Intent(share);direct.setPackage(defaultSms);
            try {
                startActivity(direct);
                outgoingNumber=number;awaitingReturn=true;
                Store.append(this,number,"문자 앱 열림");return;
            } catch(Exception ignored) { /* Fallback to a share chooser. */ }
        }
        try {
            startActivity(Intent.createChooser(share,"이미지를 보낼 앱 선택"));
            outgoingNumber=number;awaitingReturn=true;
            Store.append(this,number,"공유 화면 열림");
        } catch(Exception error) {
            toast("이미지를 공유할 앱을 찾지 못했습니다. 문자 앱 설치 상태를 확인해 주세요.");
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if(awaitingReturn) {
            awaitingReturn=false;
            new AlertDialog.Builder(this).setTitle("문자 발송 여부 확인")
                .setMessage("문자 앱에서 이미지 MMS 전송을 직접 완료하셨나요?\n(앱이 통신사 발송 결과를 확인한 것은 아닙니다.)")
                .setPositiveButton("네, 보냈어요",(d,w)->{
                    Store.append(this,outgoingNumber,"사용자 발송 확인"); Store.clearPending(this);finish();})
                .setNegativeButton("아니요",(d,w)->{
                    Store.append(this,outgoingNumber,"발송 여부 미확인");})
                .show();
        }
    }
    private void copyPhone() {
        if(!Store.validNumber(number())) {toast("전화번호를 확인해 주세요.");return;}
        ClipboardManager cb=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        if(cb!=null) {cb.setPrimaryClip(ClipData.newPlainText("고객번호",number()));toast("고객번호를 복사했습니다.");}
    }
    private void skip() {
        String phone=number();
        if(Store.validNumber(phone))Store.append(this,phone,"발송 안 함");
        Store.clearPending(this);finish();
    }
    private void toast(String m){Toast.makeText(this,m,Toast.LENGTH_LONG).show();}
}
