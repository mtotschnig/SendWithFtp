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

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;

public class UriList extends ListActivity {
  private static final int DELETE_COMMAND_ID = 1;
  private static final int WEB_COMMAND_ID = 2;
  private static final int INFO_COMMAND_ID = 3;
  private static final int EDIT_COMMAND_ID = 4;
  private static final int PICK_COMMAND_ID = 5;
  private static final int INFO_DIALOG_ID = 0;
  private static final int ACTIVITY_TRANSFER = 0;
  private static final int ACTIVITY_PICK = 1;
  private UriDataSource datasource;
  private Cursor mUriCursor;
  private Button mAddButton;
  private EditText mUriText;
  private String uriHint = "ftp://login:password@my.example.org:port/my/directory/";
  private String action;
  private Bundle extras;
  private String type;
  /**
   * row that has been selected for editing
   */
  private Long mEditedRow = 0L;
  /**
   * row for which a pick file intent has been launched
   */
  private String mSelectedUri = "";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.uri_list);
    
    Intent intent = getIntent();
    action = intent.getAction();
    if (Intent.ACTION_SEND.equals(action)) {
      setTitle(R.string.select_ftp_target);
      extras = intent.getExtras();
      type = intent.getType();
    } else {
      setTitle(R.string.manage_ftp_targets);
    }

    datasource = new UriDataSource(this);
    datasource.open();
    mUriCursor = datasource.getAllUris();
    startManagingCursor(mUriCursor);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{SQLiteHelper.COLUMN_URI};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{android.R.id.text1};

    // Now create a simple cursor adapter and set it to display
    final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, mUriCursor, from, to); 
    setListAdapter(adapter);
    mUriText = (EditText) findViewById(R.id.uri_new);
    mUriText.setText(uriHint);
    mAddButton = (Button) findViewById(R.id.addOperation);
    mAddButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        createOrUpdateUri(mUriText.getText().toString());
      }
    });
    registerForContextMenu(getListView());
  }
  protected void createOrUpdateUri(String uri) {
    boolean success;
    if (mEditedRow != 0L) {
      success = datasource.updateUri(mEditedRow,uri) > 0;
    } else {
      success = datasource.createUri(uri) > -1;
    }
    if (success) {
      mUriCursor.requery();
      if (mEditedRow != 0L) {
        resetAddButton();
      } else {
        mUriText.setText(uriHint);
      }
      mAddButton.setText(R.string.button_add);
    }
  }
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0, DELETE_COMMAND_ID, 0, R.string.menu_delete);
    menu.add(0, EDIT_COMMAND_ID, 0, R.string.menu_edit);
    menu.add(0, PICK_COMMAND_ID, 0, R.string.menu_pick);
  } 
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case DELETE_COMMAND_ID:
      if (Long.valueOf(info.id) == mEditedRow) {
        resetAddButton();
      }
      datasource.deleteUri(info.id);
      mUriCursor.requery();
      return true;
    case EDIT_COMMAND_ID:
      mUriCursor.moveToPosition(info.position);
      mUriText.setText(mUriCursor.getString(1));
      mEditedRow = info.id;
      mAddButton.setText(R.string.button_change);
      return true;
    case PICK_COMMAND_ID:
      mUriCursor.moveToPosition(info.position);
      mSelectedUri = mUriCursor.getString(1);
      Intent intent = new Intent("org.openintents.action.PICK_FILE");
      startActivityForResult(intent, ACTIVITY_PICK);
      return true;
    }
      
    return super.onContextItemSelected(item);   
  }
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    if (Intent.ACTION_SEND.equals(action)) {
      Intent i = new Intent(this, FtpTransfer.class);
      mUriCursor.moveToPosition(position);
      String target = mUriCursor.getString(1);
      i.setDataAndType(android.net.Uri.parse(target),type);
      i.putExtras(extras);
      startActivityForResult(i, ACTIVITY_TRANSFER);
    }
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(Menu.NONE,INFO_COMMAND_ID,Menu.NONE,R.string.menu_info)
        .setIcon(android.R.drawable.ic_menu_info_details);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case INFO_COMMAND_ID:
        showDialog(INFO_DIALOG_ID);
        return true;
      case WEB_COMMAND_ID:
        viewWebSite();
        return true;
      }
      return super.onOptionsItemSelected(item);
  }
  private void viewWebSite() {
    Intent i = new Intent(Intent.ACTION_VIEW);
    i.setData(Uri.parse("http://mtotschnig.github.com/SendWithFtp/"));
    startActivity(i);
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    LayoutInflater li;
    View view;
    switch (id) {
    case INFO_DIALOG_ID:
      li = LayoutInflater.from(this);
      view = li.inflate(R.layout.aboutview, null);
      ((TextView)view.findViewById(R.id.aboutVersionCode)).setText(getVersionInfo());

      /*      
      String imId = Settings.Secure.getString(
          getContentResolver(), 
          Settings.Secure.DEFAULT_INPUT_METHOD
       );
      ((TextView)view.findViewById(R.id.debug)).setText(imId);
      */

      return new AlertDialog.Builder(this)
        .setTitle(getResources().getString(R.string.app_name) + " " + getResources().getString(R.string.menu_help))
        .setIcon(R.drawable.share)
        .setView(view)
        .setPositiveButton(R.string.button_website, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            viewWebSite();
          }
        })
        .setNegativeButton(android.R.string.ok, null).create();
    }
    return null;
  }
  

  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == ACTIVITY_TRANSFER)
      finish();
    else if (requestCode == ACTIVITY_PICK && intent != null) {
      String filename = intent.getDataString();
      if (filename != null) {
        // Get rid of URI prefix:
        if (filename.startsWith("file://")) {
          filename = filename.substring(7);
        }
        Intent i = new Intent(this, FtpTransfer.class);
        String target = mSelectedUri;
        i.setData(android.net.Uri.parse(target));
        i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filename)));
        startActivity(i);
      }
    }
  }
  @Override
  public void onBackPressed() {
    if (mEditedRow != 0L) {
      resetAddButton();   
    } else {
      super.onBackPressed();
    }
  }
  private void resetAddButton() {
    mUriText.setText(uriHint);
    mEditedRow = 0L;
    mAddButton.setText(R.string.button_add);
  }

  @Override
  protected void onDestroy() {
    datasource.close();
    super.onPause();
  }
  @Override
  protected void onSaveInstanceState(Bundle outState) {
   super.onSaveInstanceState(outState);
   outState.putLong("editedRow", mEditedRow);
   outState.putString("selectedUri", mSelectedUri);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
   super.onRestoreInstanceState(savedInstanceState);
   mEditedRow = savedInstanceState.getLong("editedRow");
   if (mEditedRow != 0L) {
     mAddButton.setText(R.string.button_change);
   }
   mSelectedUri = savedInstanceState.getString("selectedUri");
  }
  public String getVersionInfo() {
    String version = "";
    String versionname = "";
    try {
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = " (revision " + pi.versionCode + ") ";
      versionname = pi.versionName;
      //versiontime = ", " + R.string.installed + " " + sdf.format(new Date(pi.lastUpdateTime));
    } catch (Exception e) {
      Log.e("SendWithFtp", "Package info not found", e);
    }
    return versionname + version;
  }
}
