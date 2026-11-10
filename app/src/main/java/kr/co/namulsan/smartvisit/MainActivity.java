package kr.co.namulsan.smartvisit;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/** Settings only. Never dials a number and does not replace AnyZone/Phone. */
public final class MainActivity extends Activity {
    private static final int PERMS_PHONE=101;
    private static final int PERMS_NOTIFICATION=102;
    private static final int PERMS_CONTACTS=104;
    private static final int REQUEST_SCREENING_ROLE=103;
    private final int[] hourChoices={1,12,24,48};
    private EditText companyField,nameField;
    private CheckBox enabledCheck;
    private Spinner duplicateSpinner;
    private TextView roleStatus,lastCallStatus;
    private Button openLast;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        draw();
    }

    private boolean screeningAllowed() {
        if(Build.VERSION.SDK_INT<29) return false;
        RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);
        return rm!=null && rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                 && rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    private void draw() {
        LinearLayout root=Ui.column(this);
        root.setBackgroundColor(0xffF4F7FC);
        Ui.add(root,Ui.text(this,"SmartVisit  |  통화 후 고객 안내",23,true,Ui.BLUE),2);
        Ui.add(root,Ui.text(this,"애니존 · 기본 전화 앱 · 연락처에서 평소처럼 전화하세요. 종료 후 안내 이미지 발송 여부를 선택합니다.",14,false,0xff536179),8);

        Ui.label(root,"① 최초 1회 외근프로 정보",18);
        companyField=Ui.edit(this,Store.company(this),"소속");Ui.add(root,companyField,7);
        nameField=Ui.edit(this,Store.name(this),"이름 (예: 임성택)");Ui.add(root,nameField,7);
        enabledCheck=new CheckBox(this);
        enabledCheck.setText("통화 종료 후 알림 사용 (ON/OFF)");
        enabledCheck.setTextSize(15);
        enabledCheck.setChecked(Store.enabled(this));
        Ui.add(root,enabledCheck,10);
        Ui.label(root,"중복 발송 경고 기간",13);
        duplicateSpinner=new Spinner(this);
        duplicateSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
            new String[]{"1시간","12시간","24시간 (추천)","48시간"}));
        for(int i=0;i<hourChoices.length;i++) {
            if(hourChoices[i]==Store.duplicateHours(this))duplicateSpinner.setSelection(i);
        }
        Ui.add(root,duplicateSpinner,2);
        Button save=Ui.button(this,"소속 · 이름 및 설정 저장",true);
        Ui.add(root,save,9);
        save.setOnClickListener(v->saveSettings());

        Ui.label(root,"② 통화 감지 권한",22);
        roleStatus=Ui.text(this,"권한 상태 확인 중",14,false,Ui.NAVY);
        Ui.add(root,roleStatus,6);
        Button grant=Ui.button(this,"통화 식별 역할 허용",false);
        Ui.add(root,grant,8);
        grant.setOnClickListener(v->requestCallScreeningRole());
        Button permissions=Ui.button(this,"통화 상태 · 알림 권한 허용",false);
        Ui.add(root,permissions,7);
        permissions.setOnClickListener(v->requestPhoneAndNotificationPermissions());
        Button contacts=Ui.button(this,"연락처 저장 번호도 감지 허용 (선택)",false);
        Ui.add(root,contacts,7);
        contacts.setOnClickListener(v->requestContactsPermission());
        Ui.add(root,Ui.text(this,"연락처 번호도 통화 감지하려면 별도 허용이 필요할 수 있습니다. 이 앱은 연락처 목록을 읽어 저장하지 않습니다.",12,false,0xff66758F),5);

        Ui.label(root,"③ 통화 후 안내",22);
        lastCallStatus=Ui.text(this,"최근 통화 안내가 없습니다.",14,false,0xff405777);
        Ui.add(root,lastCallStatus,7);
        openLast=Ui.button(this,"최근 통화 이미지 확인 · 발송 선택",true);
        Ui.add(root,openLast,8);
        openLast.setOnClickListener(v->openLastDetected());
        Button history=Ui.button(this,"발송 선택 이력",false);
        Ui.add(root,history,8);
        history.setOnClickListener(v->startActivity(new Intent(this,HistoryActivity.class)));

        Ui.add(root,Ui.text(this,
            "※ 애니존·기본 전화 앱·연락처에서 직접 통화합니다. 기본 전화 앱을 교체하지 않습니다.\n"+
            "※ 통화 식별 역할 허용이 필요하며, 기기 또는 연락처 저장 여부에 따라 발신번호를 받지 못할 수 있습니다.\n"+
            "※ 실제 통화 연결 여부는 자동 판별하지 않습니다.\n"+
            "※ 통화 종료 후 알림을 눌러 보내기/취소를 결정합니다.\n"+
            "※ 실제 MMS 전송은 기본 문자 앱에서 최종 확인합니다.",12,false,0xff6B778C),17);
        setContentView(Ui.scroll(this,root));
        refreshStatus();
    }
    private void saveSettings() {
        String company=companyField.getText().toString().trim();
        String name=nameField.getText().toString().trim();
        if(company.isEmpty()||name.isEmpty()) { toast("소속과 이름을 입력해 주세요.");return; }
        Store.saveProfile(this,company,name,enabledCheck.isChecked(),hourChoices[duplicateSpinner.getSelectedItemPosition()]);
        if(!enabledCheck.isChecked())Store.clearPending(this);
        toast("설정을 저장했습니다.");
        refreshStatus();
    }
    private void requestCallScreeningRole() {
        if(Build.VERSION.SDK_INT<29) {toast("안드로이드 10 이상이 필요합니다.");return;}
        RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);
        if(rm==null || !rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)){
            toast("이 기기에서 통화 식별 역할을 사용할 수 없습니다.");return;
        }
        if(rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)){
            toast("통화 식별 역할이 이미 허용되어 있습니다.");return;
        }
        startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),REQUEST_SCREENING_ROLE);
    }
    private void requestPhoneAndNotificationPermissions() {
        if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},PERMS_PHONE);return;
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},PERMS_NOTIFICATION);return;
        }
        toast("통화 상태 및 알림 권한이 허용되어 있습니다.");
    }
    private void requestContactsPermission() {
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},PERMS_CONTACTS);
        } else toast("연락처 번호 감지 권한이 이미 허용되어 있습니다.");
    }
    private void openLastDetected(){
        String n=Store.pendingNumber(this);
        if(!Store.validNumber(n)) {toast("확인할 고객번호가 없습니다. 통화 식별 권한과 최근 통화 상태를 확인해 주세요.");return;}
        startActivity(new Intent(this,PreviewActivity.class).putExtra("phone",n));
    }
    private void refreshStatus(){
        if(roleStatus==null) return;
        boolean role=screeningAllowed();
        boolean phone=checkSelfPermission(Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED;
        boolean notifications=Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        boolean contacts=checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED;
        roleStatus.setText("통화 식별: "+(role?"허용됨":"허용 필요")
            +"  |  통화 상태: "+(phone?"허용됨":"허용 필요")
            +"  |  알림: "+(notifications?"허용됨":"허용 필요")
            +"  |  연락처 번호: "+(contacts?"허용됨":"선택 허용"));
        String n=Store.pendingNumber(this);
        if(Store.validNumber(n)) {
            lastCallStatus.setText(Store.ready(this)?
               ("최근 통화 종료 감지: "+Store.masked(n)+" (전송 여부 선택 가능)"):
               ("최근 통화 번호 감지: "+Store.masked(n)+" (종료 감지 대기/미확인)"));
            openLast.setEnabled(true);
        } else {
            lastCallStatus.setText("최근 감지된 고객번호가 없습니다.");
            openLast.setEnabled(false);
        }
    }
    @Override protected void onResume(){super.onResume();refreshStatus();}
    @Override protected void onActivityResult(int code,int result,Intent data){
        super.onActivityResult(code,result,data);
        if(code==REQUEST_SCREENING_ROLE){
            refreshStatus();
            toast(screeningAllowed()?"통화 식별 역할이 허용되었습니다.":"통화 식별 역할이 허용되지 않았습니다.");
        }
    }
    @Override public void onRequestPermissionsResult(int code,String[] perms,int[] results){
        super.onRequestPermissionsResult(code,perms,results);
        refreshStatus();
        if(code==PERMS_PHONE && checkSelfPermission(Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED) {
            if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},PERMS_NOTIFICATION);
        }
    }
    private void toast(String t){Toast.makeText(this,t,Toast.LENGTH_LONG).show();}
}
