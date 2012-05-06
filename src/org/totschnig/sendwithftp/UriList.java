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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class UriList extends ListActivity {
  private static final int DELETE_COMMAND_ID = 1;
  private static final int EDIT_COMMAND_ID = 2;
  private static final int ACTIVITY_TRANSFER = 0;
  private UriDataSource datasource;
  private Cursor mUriCursor;
  private Button mAddButton;
  private EditText mUriText;
  private String uriPrefix = "ftp://";
  private String action;
  private Uri source;

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
        createUri(mUriText.getText().toString());
      }
    });
    registerForContextMenu(getListView());
  }
  protected void createUri(String uri) {
    if (datasource.createUri(uri) > -1) {
      mUriCursor.requery();
      mUriText.setText(uriPrefix);
    }
  }
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0, DELETE_COMMAND_ID, 0, R.string.menu_delete);
    menu.add(0, EDIT_COMMAND_ID,0, R.string.menu_edit);
  } 
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case DELETE_COMMAND_ID:
      datasource.deleteUri(info.id);
      mUriCursor.requery();
      return true;
    case EDIT_COMMAND_ID:
      mUriText.setText(mUriCursor.getString(1));
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
    }
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == ACTIVITY_TRANSFER)
      finish();
  }

  @Override
  protected void onDestroy() {
    datasource.close();
    super.onPause();
  }
}
