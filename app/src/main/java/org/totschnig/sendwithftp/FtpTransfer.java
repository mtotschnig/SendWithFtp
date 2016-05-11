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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URI;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;



public class FtpTransfer extends Activity {
  private ProgressDialog mProgressDialog;
  private FtpAsyncTask task=null;
  private Uri target;
  private InputStream is;
  private String fileName;
  private int fileType = FTP.BINARY_FILE_TYPE;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent = getIntent();
    target = intent.getData();
    task=(FtpAsyncTask)getLastNonConfigurationInstance();
    if (task!=null) {
      showProgressDialog();
      task.attach(this);
      if (task.getStatus() == AsyncTask.Status.FINISHED) {
        markAsDone();
      }
    } else {
      String type = intent.getType();
      is = null;
      fileName = null;
      if (type != null) {
        Log.i("FtpTransfer",type);
        if ("text/plain".equals(type)) {
          handleSendText(intent); // Handle text being sent
        } else if ("text/x-vcard".equals(type)) {
          handleSendVcard(intent); // Handle vcard being sent
        } else if (type.startsWith("image/")) {
          handleSendImage(intent); // Handle single image being sent
        }
      }
      if (is == null) {
        handleFallBack(intent);
      }
      if (is != null) {
        showProgressDialog();
        Log.i("FtpTransfer","fileType: "+fileType);
        task = new FtpAsyncTask(this, is, target,fileName,fileType);
        task.execute();
      } else {
        Toast.makeText(getBaseContext(), "Cannot handle shared data", Toast.LENGTH_LONG).show();
        finish();
      }
    }
  }
  private void handleSendVcard(Intent intent) {
    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
    if (uri != null) {
      ContentResolver cr = getContentResolver();
      try {
          //the following would only have a chance to work if FtpTransfer would request permission READ_CONTACTS
          //we would then also need to find out a meaningful filename
          is = cr.openInputStream(uri);
          fileType = FTP.ASCII_FILE_TYPE;
          fileName = "contact.vcard";
      } catch (FileNotFoundException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
      } catch (SecurityException e) {
        Toast.makeText(getBaseContext(), "No permission to read contacts", Toast.LENGTH_SHORT).show();;
      }
    }
  }
  private void handleFallBack(Intent intent) {
    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
    if (uri != null) {
      File source = new File(uri.getPath());
      if (source.exists()) {
        fileName = source.getName();
        try {
          is = new FileInputStream(source);
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
      }
    }
    if (is == null) {
      try {
        is = getContentResolver().openInputStream(uri);
        fileName = getDisplayName(uri);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  private void handleSendText(Intent intent) {
    String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
    if (sharedText != null) {
      is = new StringBufferInputStream(sharedText);
      fileName = intent.getStringExtra(Intent.EXTRA_SUBJECT);
      fileType = FTP.ASCII_FILE_TYPE;
    }
  }
  private void handleSendImage(Intent intent) {
    Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
    if (imageUri != null) {
      // Get resource path
      String fileuri = parseImageUriTofileName(imageUri);
      File source = new File (fileuri);
      fileName = source.getName();
      try {
        is = new FileInputStream(source);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
  }
  private void showProgressDialog() {
    mProgressDialog = ProgressDialog.show(this, "",
        getString(R.string.ftp_uploading_wait,target.getHost()), true, true, new OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
            if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
              task.cancel(true);
              markAsDone();
            }
          }
    });
  }
  public String parseImageUriTofileName(Uri uri) {
    String selectedImagePath = null;

    String[] projection = { MediaStore.Images.Media.DATA };
    Cursor cursor = managedQuery(uri, projection, null, null, null);

    if (cursor != null) {
      // Here you will get a null pointer if cursor is null
      // This can be if you used OI file manager for picking the media
      int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      selectedImagePath = cursor.getString(column_index);
    }
    return selectedImagePath;
  }

  void markAsDone() {
    String ftp_result;
    mProgressDialog.dismiss();
    if (task.isCancelled()) {
      ftp_result = getString(R.string.ftp_cancelled,target.getHost());
    } else {
      Result result = task.getResult();
      if (result.message == R.string.ftp_failure)
        ftp_result = getString(result.message,target.getHost(),result.extra[0]);
      else
        ftp_result = getString(result.message,target.getHost());
    }
    Toast.makeText(this,ftp_result, Toast.LENGTH_LONG).show();
    task = null;
    finish();
  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    if (task != null)
      task.detach();
    return(task);
  }
  @Override
  public void onStop() {
    super.onStop();
    mProgressDialog.dismiss();
  }

  static class FtpAsyncTask extends AsyncTask<Void, Void, Void> {
      private FtpTransfer activity;
      private Uri target;
      private String fileName;
      private InputStream is;
      private int fileType;
      private Result result;
      private ProgressDialog mProgressDialog;

      public FtpAsyncTask(FtpTransfer activity,InputStream is,Uri target2, String fileName,int fileType) {
        attach(activity);
        this.target = target2;
        this.is = is;
        this.fileName = fileName;
        this.fileType = fileType;
      }
      @Override
      protected Void doInBackground(Void... params) {
        boolean result;
        //malformed:
        //String ftpTarget = "bad.uri";
        //bad password:
        //String ftpTarget = "ftp://michael:foo@10.0.0.2/";
        //bad directory:
        //String ftpTarget = "ftp://michael:foo@10.0.0.2/foobar/";
        FTPClient mFTP;
        if (target.getScheme().equals("ftps")) {
          mFTP = new FTPSClient(false);
          ((FTPSClient) mFTP).setTrustManager(null);
        } else
          mFTP = new FTPClient();
        String host = target.getHost();
        if (host == null)
          setResult(new Result(false,R.string.ftp_uri_malformed));
        String username = target.getUserInfo();
        String password = "";
        String path = target.getPath();
        if (username != null)
          {
          int ci = username.indexOf(':');
            if (ci != -1) {
              password = username.substring(ci + 1);
              username = username.substring(0, ci);
            }
          }
        else {
          username = "anonymous";
        }
        try {
            // Connect to FTP Server
            mFTP.connect(host);
            int reply = mFTP.getReplyCode();
            if(!FTPReply.isPositiveCompletion(reply)) {
              setResult(new Result(false, R.string.ftp_connection_failure));
              return(null);
            }
            if (isCancelled()) {
              return(null);
            }
            if (!mFTP.login(username,password)) {
              setResult(new Result(false, R.string.ftp_login_failure));
              return(null);
            }
            if (isCancelled()) {
              return(null);
            }
            if (!mFTP.setFileType(fileType)) {
              setResult(new Result(false, R.string.ftp_setFileType_failure));
              return(null);
            }
            if (isCancelled()) {
              return(null);
            }
            mFTP.enterLocalPassiveMode();
            if (!path.equals("")) {
              if (!mFTP.changeWorkingDirectory(path)) {
                setResult(new Result(false, R.string.ftp_changeWorkingDirectory_failure));
                return(null);
              }
            }
            if (isCancelled()) {
              return(null);
            }
            //check if file exists
            if (mFTP.listFiles(fileName).length >0) {
              setResult(new Result(false, R.string.ftp_fileExists_failure));
              return(null);
            }
            // Upload file to FTP Server
            if (mFTP.storeFile(fileName,is)) {
              setResult(new Result(true, R.string.ftp_success));
            } else {
              setResult(new Result(false, R.string.ftp_failure,mFTP.getReplyString()));
            }
        } catch (ConnectException e) {
          Log.d("DEBUG",e.getClass().getName()+" "+e.getMessage());
          setResult(new Result(false, R.string.ftp_connection_refused));
        } catch (SocketException e) {
          Log.d("DEBUG",e.getClass().getName()+" "+e.getMessage());
          setResult(new Result(false, R.string.ftp_socket_exception));
        } catch (FTPConnectionClosedException e) {
          Log.d("DEBUG",e.getClass().getName()+" "+e.getMessage());
          setResult(new Result(false, R.string.ftp_connection_refused));
        } catch (IOException e) {
          Log.d("DEBUG",e.getClass().getName()+" "+e.getMessage());
          setResult(new Result(false,R.string.ftp_io_exception));
        }  finally {
          if(mFTP.isConnected()) {
            try {
              mFTP.disconnect();
            } catch(IOException ioe) {
              // do nothing
            }
          }
        }
        return(null);
      }
      protected void onPostExecute(Void unused) {
        if (activity==null) {
          Log.w("FtpAsyncTask", "onPostExecute() skipped -- no activity");
        }
        else {
          activity.markAsDone();
        }
      }
      void attach(FtpTransfer activity) {
        this.activity=activity;
      }
      void detach() {
        activity=null;
      }
      public Result getResult() {
        return result;
      }
      public void setResult(Result result) {
        this.result = result;
      }
    }
  /**
   * represents a tuple of success flag, and message as an R id
   * @author Michael Totschnig
   *
   */
  public static class Result {
    /**
     * true represents success, false failure
     */
    public boolean success;
    /**
     * a string id from {@link R} for i18n and joining with an argument
     */
    public int message;

    /**
     * optional argument to be passed to getString when resolving message id
     */
    public Object[] extra;

    public Result(boolean success) {
      this.success = success;
    }

    public Result(boolean success,int message) {
      this.success = success;
      this.message = message;
    }

    public Result(boolean success,int message,Object... extra) {
      this.success = success;
      this.message = message;
      this.extra = extra;
    }
  }

  private String getDisplayName(Uri uri) {

    if (!"file".equalsIgnoreCase(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      // The query, since it only applies to a single document, will only return
      // one row. There's no need to filter, sort, or select fields, since we want
      // all fields for one document.
      try {
        Cursor cursor = getContentResolver()
            .query(uri, null, null, null, null, null);

        if (cursor != null) {
          try {
            if (cursor.moveToFirst()) {
              // Note it's called "Display Name".  This is
              // provider-specific, and might not necessarily be the file name.
              int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
              if (columnIndex != -1) {
                String displayName = cursor.getString(columnIndex);
                if (displayName != null) {
                  return displayName;
                }
              }
            }
          } catch (Exception e) {}
          finally {
            cursor.close();
          }
        }
      } catch (SecurityException e) {
        //this can happen if the user has restored a backup and
        //we do not have a persistable permision
        //return null;
      }
    }
    List<String> filePathSegments = uri.getPathSegments();
    if (filePathSegments.size()>0) {
      return filePathSegments.get(filePathSegments.size()-1);
    } else {
      return "UNKNOWN";
    }
  }
}
