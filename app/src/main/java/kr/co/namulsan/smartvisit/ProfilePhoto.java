package kr.co.namulsan.smartvisit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Profile portrait stored only in this app's private files directory.
 * The system document picker does not require the app to read the photo library.
 * Photos are saved upright and resized; the original is never modified.
 */
final class ProfilePhoto {
    private static final String FILENAME="profile_portrait.jpg";
    static File file(Context c) { return new File(c.getFilesDir(),FILENAME); }
    static boolean exists(Context c) { return file(c).isFile() && file(c).length()>0; }
    static void delete(Context c) throws IOException {
        File f=file(c);
        if(f.exists() && !f.delete()) throw new IOException("등록된 사진을 삭제하지 못했습니다.");
    }
    static Bitmap load(Context c) {
        if(!exists(c)) return null;
        return BitmapFactory.decodeFile(file(c).getAbsolutePath());
    }
    static void save(Context c, Uri selected) throws IOException {
        if(selected==null) throw new IOException("사진을 선택하지 않았습니다.");
        int orientation=ExifInterface.ORIENTATION_NORMAL;
        try(InputStream s=c.getContentResolver().openInputStream(selected)) {
            if(s==null) throw new IOException("선택한 사진에 접근할 수 없습니다.");
            ExifInterface exif=new ExifInterface(s);
            orientation=exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
        } catch(Exception ignored) {
            // Some picture providers do not provide EXIF metadata. Continue upright.
        }
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        try(InputStream s=c.getContentResolver().openInputStream(selected)){
            if(s==null) throw new IOException("선택한 사진을 읽을 수 없습니다.");
            BitmapFactory.decodeStream(s,null,bounds);
        }
        if(bounds.outWidth<=0 || bounds.outHeight<=0) throw new IOException("지원하지 않는 사진 파일입니다.");
        int sample=1;
        while(Math.max(bounds.outWidth/sample,bounds.outHeight/sample)>1600) sample*=2;
        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=sample;
        Bitmap decoded;
        try(InputStream s=c.getContentResolver().openInputStream(selected)) {
            if(s==null) throw new IOException("선택한 사진을 읽을 수 없습니다.");
            decoded=BitmapFactory.decodeStream(s,null,opts);
        }
        if(decoded==null) throw new IOException("사진을 불러오지 못했습니다.");
        Bitmap fixed=decoded;
        try {
            Matrix transform=new Matrix();
            switch(orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:transform.postRotate(90);break;
                case ExifInterface.ORIENTATION_ROTATE_180:transform.postRotate(180);break;
                case ExifInterface.ORIENTATION_ROTATE_270:transform.postRotate(270);break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:transform.postScale(-1,1);break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:transform.postScale(1,-1);break;
                case ExifInterface.ORIENTATION_TRANSPOSE:transform.postRotate(90);transform.postScale(-1,1);break;
                case ExifInterface.ORIENTATION_TRANSVERSE:transform.postRotate(270);transform.postScale(-1,1);break;
                default:break;
            }
            if(!transform.isIdentity()){
                fixed=Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),transform,true);
            }
            File target=file(c),temp=new File(c.getFilesDir(),FILENAME+".tmp");
            try(FileOutputStream stream=new FileOutputStream(temp)){
                if(!fixed.compress(Bitmap.CompressFormat.JPEG,88,stream))throw new IOException("사진 저장에 실패했습니다.");
            }
            if(temp.length()==0 || temp.length()>6_000_000){temp.delete();throw new IOException("사진 용량을 처리할 수 없습니다.");}
            // Keep the previous portrait if saving the new portrait failed.
            try(java.io.FileInputStream in=new java.io.FileInputStream(temp);
                FileOutputStream out=new FileOutputStream(target)) {
                byte[] buffer=new byte[8192];int n;
                while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            } finally { temp.delete(); }
        } finally {
            if(fixed!=decoded)fixed.recycle();
            decoded.recycle();
        }
    }
}
