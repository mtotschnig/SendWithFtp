package org.totschnig.sendwithftp;
import java.net.URI;
import java.net.URISyntaxException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

public class UriDataSource {

  // Database fields
  private SQLiteDatabase database;
  private SQLiteHelper dbHelper;
  private String[] allColumns = { SQLiteHelper.COLUMN_ID,
      SQLiteHelper.COLUMN_URI };
  
  Context context;

  public UriDataSource(Context context) {
    this.context = context;
    dbHelper = new SQLiteHelper(context);
  }

  public void open() throws SQLException {
    database = dbHelper.getWritableDatabase();
  }

  public void close() {
    dbHelper.close();
  }

  public long createUri(String target) {
    URI uri;
    try {
      uri = new URI(target);
    } catch (URISyntaxException e1) {
      Toast.makeText(context, R.string.ftp_uri_malformed, Toast.LENGTH_SHORT).show();
      return -1;
    }
    if (!uri.getScheme().equals("ftp")) {
      Toast.makeText(context, "Only FTP URIs are handled, not " + uri.getScheme(), Toast.LENGTH_SHORT).show();
      return -1;
    }
    ContentValues values = new ContentValues();
    values.put(SQLiteHelper.COLUMN_URI, target);
    long result = database.insert(SQLiteHelper.TABLE_URI, null,
        values);
    if (result == -1) {
      Toast.makeText(context, "Error saving URI to database", Toast.LENGTH_SHORT).show();
    }
    return result;
  }

  public void deleteUri(long id) {
    database.delete(SQLiteHelper.TABLE_URI, SQLiteHelper.COLUMN_ID
        + " = " + id, null);
  }

  public Cursor getAllUris() {
    return database.query(SQLiteHelper.TABLE_URI,
        allColumns, null, null, null, null, null);
  }
}
