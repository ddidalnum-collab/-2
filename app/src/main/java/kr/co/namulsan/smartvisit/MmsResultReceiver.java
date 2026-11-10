package kr.co.namulsan.smartvisit;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public final class MmsResultReceiver extends BroadcastReceiver {
    public static final String ACTION="kr.co.namulsan.smartvisit.MMS_RESULT";
    @Override public void onReceive(Context c,Intent i){
        if(!ACTION.equals(i.getAction())) return;
        String number=i.getStringExtra("phone");
        if(number==null)number="";
        int result=getResultCode();
        String status=result==Activity.RESULT_OK?"통신사 MMS 전송 성공 응답":"MMS 전송 실패 (코드 "+result+")";
        Store.append(c,number,status);
        Toast.makeText(c,status,Toast.LENGTH_LONG).show();
    }
}
