package org.totschnig.sendwithftp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteHelper extends SQLiteOpenHelper {

  public static final String TABLE_URI = "uris";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_URI = "uri";

  private static final String DATABASE_NAME = "database";
  private static final int DATABASE_VERSION = 1;

  // Database creation sql statement
  private static final String DATABASE_CREATE = "create table "
      + TABLE_URI + "( " + COLUMN_ID
      + " integer primary key autoincrement, " + COLUMN_URI
      + " text not null);";

  public SQLiteHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    database.execSQL(DATABASE_CREATE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w("SQLiteHelper",
        "Upgrading database from version " + oldVersion + " to "
            + newVersion + ".");
  }
}