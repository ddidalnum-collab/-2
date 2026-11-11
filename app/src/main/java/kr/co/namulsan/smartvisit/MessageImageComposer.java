package kr.co.namulsan.smartvisit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Compose the fixed customer handout; only the original bottom-right white card is customized. */
final class MessageImageComposer {
    static File create(Context c,String center,String employee) throws Exception {
        Bitmap base;
        try(InputStream in=c.getAssets().open("guide_blank.png")) { base=BitmapFactory.decodeStream(in); }
        if(base==null) throw new IllegalStateException("안내 이미지 로드 실패");
        Bitmap mutable=base.copy(Bitmap.Config.ARGB_8888,true);base.recycle();
        try {
            Canvas canvas=new Canvas(mutable);
            String displayName=employee.trim().endsWith("프로")?employee.trim():employee.trim()+" 프로";
            if(ProfilePhoto.exists(c)) {
                Bitmap portrait=ProfilePhoto.load(c);
                if(portrait==null)throw new IllegalStateException("등록된 프로필 사진을 읽지 못했습니다. 사진을 다시 등록해 주세요.");
                try { drawPortrait(canvas,portrait); }
                finally { portrait.recycle(); }
                // Photo left, affiliation and name to its right. Never overlap the portrait.
                drawCentered(canvas,center.trim(),789,1198,22,199,false);
                drawCentered(canvas,displayName,789,1238,37,199,true);
            } else {
                // Backward-compatible identity layout when no portrait has been registered.
                drawCentered(canvas,center.trim(),762,1197,26,280,false);
                drawCentered(canvas,displayName,762,1248,44,284,true);
            }
            File dir=new File(c.getCacheDir(),"shares");
            if(!dir.exists()&&!dir.mkdirs()) throw new Exception("캐시 생성 실패");
            File output=new File(dir,"smartthings_visit_notice.jpg");
            // Retry at decreasing JPEG quality so profile portraits do not break MMS limits.
            for(int quality: new int[]{88,80,72,64}) {
                try(FileOutputStream out=new FileOutputStream(output)) {
                    if(!mutable.compress(Bitmap.CompressFormat.JPEG,quality,out))throw new Exception("이미지 저장 실패");
                }
                if(output.length()>0 && output.length()<=1_000_000L) return output;
            }
            throw new Exception("MMS 첨부 제한(1MB)을 초과했습니다. 다른 사진으로 다시 시도해 주세요.");
        } finally { mutable.recycle(); }
    }
    private static void drawPortrait(Canvas canvas,Bitmap portrait){
        // Fixed customer graphic is 941x1278. White card: approx x=594..914, y=1160..1265.
        RectF dest=new RectF(609,1173,684,1250);
        float targetRatio=dest.width()/dest.height();
        int w=portrait.getWidth(),h=portrait.getHeight();
        int sw=w,sh=h;
        if((float)w/h>targetRatio) sw=Math.round(h*targetRatio);
        else sh=Math.round(w/targetRatio);
        int l=(w-sw)/2,t=(h-sh)/2;
        Paint image=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        Path clip=new Path();clip.addRoundRect(dest,12f,12f,Path.Direction.CW);
        canvas.save();canvas.clipPath(clip);
        canvas.drawBitmap(portrait,new Rect(l,t,l+sw,t+sh),dest,image);
        canvas.restore();
        Paint border=new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setColor(Color.rgb(210,223,244));border.setStyle(Paint.Style.STROKE);border.setStrokeWidth(2f);
        canvas.drawRoundRect(dest,12f,12f,border);
    }
    private static void drawCentered(Canvas c,String text,float x,float baseline,float size,float maxWidth,boolean bold) {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(bold?Color.rgb(5,33,116):Color.rgb(18,43,98));
        p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);
        float measured=p.measureText(text);
        if(measured>maxWidth) p.setTextSize(Math.max(13,size*maxWidth/measured));
        c.drawText(text,x,baseline,p);
    }
}
