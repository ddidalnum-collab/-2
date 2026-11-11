package kr.co.namulsan.smartvisit;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.File;

/** Preview only; no dialer and no send action. */
public final class PersonalizedImagePreviewActivity extends Activity {
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=Ui.column(this);
        root.setBackgroundColor(0xffF4F7FC);
        Ui.add(root,Ui.text(this,"고객 발송 이미지 미리보기",22,true,Ui.NAVY),2);
        Ui.add(root,Ui.text(this,"등록된 사진과 저장된 소속·이름이 함께 표시됩니다. 이 화면에서는 문자를 발송하지 않습니다.",13,false,0xff536179),7);
        try {
            String company=getIntent().getStringExtra("company");
            String name=getIntent().getStringExtra("name");
            File f=MessageImageComposer.create(this,
                company==null?Store.company(this):company,
                name==null?Store.name(this):name);
            ImageView img=new ImageView(this);
            img.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            img.setAdjustViewBounds(true);
            Ui.add(root,img,12);
        }catch(Exception ex){
            Ui.add(root,Ui.text(this,"미리보기 생성 실패: "+ex.getMessage(),14,false,0xffB13E27),12);
            Toast.makeText(this,"안내 이미지 확인 필요",Toast.LENGTH_LONG).show();
        }
        android.widget.Button close=Ui.button(this,"닫기",false);
        Ui.add(root,close,12);close.setOnClickListener(v->finish());
        setContentView(Ui.scroll(this,root));
    }
}
