package kr.co.namulsan.smartvisit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

final class MessageImageComposer {
    /** Asset dimensions 941x1278. The bottom-right panel has been blanked. */
    static File create(Context c,String center,String employee) throws Exception {
        Bitmap bitmap;
        try(InputStream in=c.getAssets().open("guide_blank.png")) { bitmap=BitmapFactory.decodeStream(in); }
        if(bitmap==null) throw new IllegalStateException("안내 이미지 로드 실패");
        Bitmap mutable=bitmap.copy(Bitmap.Config.ARGB_8888,true);bitmap.recycle();
        Canvas canvas=new Canvas(mutable);
        // Upper affiliation and lower employee name, matching the original white card.
        drawCentered(canvas,center.trim(),762,1197,26,280,false);
        String displayName=employee.trim().endsWith("프로")?employee.trim():employee.trim()+" 프로";
        drawCentered(canvas,displayName,762,1248,44,284,true);
        File dir=new File(c.getCacheDir(),"shares"); if(!dir.exists()&&!dir.mkdirs()) throw new Exception("캐시 생성 실패");
        File output=new File(dir,"smartthings_visit_notice.jpg");
        try(FileOutputStream out=new FileOutputStream(output)) {
            if(!mutable.compress(Bitmap.CompressFormat.JPEG,88,out)) throw new Exception("이미지 저장 실패");
        } finally { mutable.recycle(); }
        if (output.length() > 1000000L) throw new Exception("MMS 첨부 제한(1MB)을 초과했습니다. 이미지 압축이 필요합니다.");
        return output;
    }
    private static void drawCentered(Canvas c,String text,float x,float baseline,float size,float width,boolean bold) {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(bold?Color.rgb(5,33,116):Color.rgb(18,43,98));
        p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);
        float measured=p.measureText(text);
        if(measured>width) p.setTextSize(Math.max(13,size*width/measured));
        c.drawText(text,x,baseline,p);
    }
}
