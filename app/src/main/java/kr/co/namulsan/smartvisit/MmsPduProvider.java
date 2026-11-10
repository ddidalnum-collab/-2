package kr.co.namulsan.smartvisit;

import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Private provider serving a one-time encoded MMS PDU to Android system MMS service. */
public final class MmsPduProvider extends ContentProvider {
    private static final String AUTH="kr.co.namulsan.smartvisit.mmspdu";
    static Uri write(Context c,byte[] data) throws IOException {
        File dir=new File(c.getCacheDir(),"mms-pdu");
        if(!dir.exists() && !dir.mkdirs()) throw new IOException("MMS 캐시 디렉터리 오류");
        String id=System.currentTimeMillis()+"-"+Integer.toHexString((int)(Math.random()*999999));
        File f=new File(dir,id+".pdu");
        try(FileOutputStream out=new FileOutputStream(f)) {out.write(data);}
        return Uri.parse("content://"+AUTH+"/"+id+".pdu");
    }
    private File check(Uri u) throws FileNotFoundException {
        if(u==null || !AUTH.equals(u.getAuthority())) throw new FileNotFoundException();
        String name=u.getLastPathSegment();
        if(name==null||!name.matches("[0-9]+-[a-f0-9]+\\.pdu")) throw new FileNotFoundException();
        return new File(getContext().getCacheDir(),"mms-pdu/"+name);
    }
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri u){return "application/vnd.wap.mms-message";}
    @Override public ParcelFileDescriptor openFile(Uri u,String mode) throws FileNotFoundException {
        if(!"r".equals(mode)) throw new FileNotFoundException();
        return ParcelFileDescriptor.open(check(u),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri u,String[] proj,String sel,String[] args,String order){
        try{
            File f=check(u);
            String[] cols=proj==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:proj;
            Object[] row=new Object[cols.length];
            for(int i=0;i<cols.length;i++){
                if(OpenableColumns.DISPLAY_NAME.equals(cols[i]))row[i]=f.getName();
                else if(OpenableColumns.SIZE.equals(cols[i]))row[i]=f.length();
            }
            MatrixCursor cursor=new MatrixCursor(cols);cursor.addRow(row);return cursor;
        }catch(Exception ignored){return null;}
    }
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] args){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] args){throw new UnsupportedOperationException();}
}
