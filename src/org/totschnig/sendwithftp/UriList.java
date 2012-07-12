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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
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

public class UriList extends ListActivity {
  private static final int DELETE_COMMAND_ID = 1;
  private static final int HELP_COMMAND_ID = 2;
  private static final int ACTIVITY_TRANSFER = 0;
  private static final int HELP_DIALOG_ID = 0;
  private UriDataSource datasource;
  private Cursor mUriCursor;
  private Button mAddButton;
  private EditText mUriText;
  private String uriPrefix = "ftp://";
  private String action;
  private Uri source;
  private Long mEditedRow = 0L;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.uri_list);
    
    Intent intent = getIntent();
    action = intent.getAction();
    if (Intent.ACTION_SEND.equals(action)) {
      setTitle(R.string.select_ftp_target);
      Bundle extras = intent.getExtras();
      source = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
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
    mUriText.setText(uriPrefix);
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
        mUriText.setText(uriPrefix);
      }
      mAddButton.setText(R.string.button_add);
    }
  }
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0, DELETE_COMMAND_ID, 0, R.string.menu_delete);
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
    }
    return super.onContextItemSelected(item);   
  }
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    if (Intent.ACTION_SEND.equals(action)) {
      Intent i = new Intent(this, FtpTransfer.class);
      mUriCursor.moveToPosition(position);
      String target = mUriCursor.getString(1);
      i.setData(android.net.Uri.parse(target));
      i.putExtra(Intent.EXTRA_STREAM, source);
      startActivityForResult(i, ACTIVITY_TRANSFER);
    } else {
      mUriText.setText(mUriCursor.getString(1));
      mEditedRow = id;
      mAddButton.setText(R.string.button_change);
    }
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(Menu.NONE,HELP_COMMAND_ID,Menu.NONE,R.string.menu_help)
        .setIcon(android.R.drawable.ic_menu_info_details);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case HELP_COMMAND_ID:
        showDialog(HELP_DIALOG_ID);
        return true;
      }
      return super.onOptionsItemSelected(item);
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    LayoutInflater li;
    View view;
    switch (id) {
    case HELP_DIALOG_ID:
      li = LayoutInflater.from(this);
      view = li.inflate(R.layout.aboutview, null);
      TextView tv;
      tv = (TextView)view.findViewById(R.id.help_project_home);
      tv.setMovementMethod(LinkMovementMethod.getInstance());
      tv = (TextView)view.findViewById(R.id.help_feedback);
      tv.setMovementMethod(LinkMovementMethod.getInstance());
      tv = (TextView)view.findViewById(R.id.help_licence_gpl);
      tv.setMovementMethod(LinkMovementMethod.getInstance());

      /*      
      String imId = Settings.Secure.getString(
          getContentResolver(), 
          Settings.Secure.DEFAULT_INPUT_METHOD
       );
      ((TextView)view.findViewById(R.id.debug)).setText(imId);
      */

      return new AlertDialog.Builder(this)
        .setTitle(getResources().getString(R.string.app_name) + " " + getResources().getString(R.string.menu_help))
        .setIcon(R.drawable.about)
        .setView(view)
        .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            dismissDialog(HELP_DIALOG_ID);
          }
        }).create();
    }
    return null;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == ACTIVITY_TRANSFER)
      finish();
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
    mUriText.setText(uriPrefix);
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
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
   super.onRestoreInstanceState(savedInstanceState);
   mEditedRow = savedInstanceState.getLong("editedRow");
   if (mEditedRow != 0L) {
     mAddButton.setText(R.string.button_change);
   }
  }

}
