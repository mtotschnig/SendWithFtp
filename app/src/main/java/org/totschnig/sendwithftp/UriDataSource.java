/*   This file is part of Send With FTP.
 *   Send With FTP is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Send With FTP is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Send With FTP.  If not, see <http://www.gnu.org/licenses/>.
*/
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
  private final SQLiteHelper dbHelper;
  private final String[] allColumns = { SQLiteHelper.COLUMN_ID,
      SQLiteHelper.COLUMN_URI };
  
  private Context context;

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
  private boolean validateUri(String target) {
    URI uri;
    try {
      uri = new URI(target);
      if (uri.getHost() ==null)
        throw new URISyntaxException(target,"No host found");
    } catch (URISyntaxException e1) {
      Toast.makeText(context, R.string.ftp_uri_malformed, Toast.LENGTH_SHORT).show();
      return false;
    }
    String scheme = uri.getScheme();
    if (scheme == null || !(scheme.equals("ftp") || scheme.equals("ftps"))) {
      Toast.makeText(context, "Only FTP(s) URIs are handled" +
          (scheme == null ? "" : " , not " + scheme), Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  public long createUri(String target) {
    if (!validateUri(target)) {
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
  public int updateUri(Long rowId, String target) {
    if (!validateUri(target)) {
      return -1;
    }
    ContentValues values = new ContentValues();
    values.put(SQLiteHelper.COLUMN_URI, target);
    int result = database.update(SQLiteHelper.TABLE_URI, values, SQLiteHelper.COLUMN_ID + "=" + rowId, null);
    if (result == 0) {
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
