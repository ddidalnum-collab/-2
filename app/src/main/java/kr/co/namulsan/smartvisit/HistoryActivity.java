package kr.co.namulsan.smartvisit;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class HistoryActivity extends Activity {
    @Override public void onCreate(Bundle b) {super.onCreate(b);draw();}
    private void draw() {
        LinearLayout root=Ui.column(this);root.setBackgroundColor(0xffF4F7FC);
        Ui.add(root,Ui.text(this,"발송 이력",24,true,Ui.BLUE),2);
        Ui.add(root,Ui.text(this,"실제 통신사 전송 확인이 아닌 사용자 선택·작성 화면 기록입니다.",13,false,0xff64748B),6);
        JSONArray arr=Store.history(this);
        SimpleDateFormat fmt=new SimpleDateFormat("yyyy.MM.dd HH:mm",Locale.KOREA);
        if(arr.length()==0)Ui.add(root,Ui.text(this,"아직 기록이 없습니다.",16,false,0xff65758C),18);
        for(int i=0;i<arr.length();i++) {
            JSONObject item=arr.optJSONObject(i);if(item==null)continue;
            LinearLayout card=Ui.column(this);
            card.setBackground(Ui.bg(0xffffffff,12,this));
            card.addView(Ui.text(this,Store.masked(item.optString("number")),17,true,Ui.NAVY));
            card.addView(Ui.text(this,fmt.format(new Date(item.optLong("when")))+"  ·  "+item.optString("status"),13,false,0xff4D5A75));
            Ui.add(root,card,9);
        }
        Button clear=Ui.button(this,"기록 전체 삭제",false);Ui.add(root,clear,16);
        clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("이력을 삭제할까요?")
            .setMessage("기기에 저장된 발송 선택 기록이 삭제됩니다.")
            .setNegativeButton("취소",null)
            .setPositiveButton("삭제",(d,w)->{Store.p(this).edit().remove("history").commit();draw();}).show());
        setContentView(Ui.scroll(this,root));
    }
}
