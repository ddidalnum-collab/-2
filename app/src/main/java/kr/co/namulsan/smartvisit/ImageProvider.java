package kr.co.namulsan.smartvisit;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Read-only single-image content provider; non-exported, URI grant required. */
public final class ImageProvider extends ContentProvider {
    static final Uri URI=Uri.parse("content://kr.co.namulsan.smartvisit.images/notice.jpg");
    private File file() { return new File(new File(getContext().getCacheDir(),"shares"),"smartthings_visit_notice.jpg"); }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return valid(uri)?"image/jpeg":null; }
    private boolean valid(Uri uri) { return URI.equals(uri); }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!valid(uri)||!"r".equals(mode)) throw new FileNotFoundException();
        return ParcelFileDescriptor.open(file(),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder) {
        if(!valid(uri)) return null;
        String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
        Object[] row=new Object[columns.length];
        for(int i=0;i<columns.length;i++) {
            if(OpenableColumns.DISPLAY_NAME.equals(columns[i])) row[i]="SmartThings_방문전안내.jpg";
            else if(OpenableColumns.SIZE.equals(columns[i])) row[i]=file().length();
        }
        MatrixCursor out=new MatrixCursor(columns);out.addRow(row);return out;
    }
    @Override public Uri insert(Uri u,ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u,ContentValues v,String s,String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u,String s,String[] args) { throw new UnsupportedOperationException(); }
}
