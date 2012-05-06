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
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URI;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;



public class FtpTransfer extends Activity {
  ProgressDialog mProgressDialog;
  private FtpAsyncTask task=null;
  Uri target;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    target = getIntent().getData();
    Bundle extras = getIntent().getExtras();
    File source = new File (((Uri) extras.getParcelable(Intent.EXTRA_STREAM)).getPath());
    
    task=(FtpAsyncTask)getLastNonConfigurationInstance();
    
    mProgressDialog = ProgressDialog.show(this, "",
        getString(R.string.ftp_uploading_wait,target.getHost()), true, true, new OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
            if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
              task.cancel(true);
              markAsDone();
            }
          }
    });
    if (task!=null) {
      task.attach(this);      
      if (task.getStatus() == AsyncTask.Status.FINISHED) {
        markAsDone();
      }
    } else {
      task = new FtpAsyncTask(this, source, target);
      task.execute();
    }
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
      private File file;
      Result result;
      ProgressDialog mProgressDialog;
      
      public FtpAsyncTask(FtpTransfer activity,File file,Uri target2) {
        attach(activity);
        this.target = target2;
        this.file = file;
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
        android.os.Debug.waitForDebugger();
        FTPClient mFTP = new FTPClient();
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
            if (!mFTP.setFileType(FTP.ASCII_FILE_TYPE)) {
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
            // Prepare file to be uploaded to FTP Server
            FileInputStream ifile = new FileInputStream(file);
            
            // Upload file to FTP Server
            if (mFTP.storeFile(file.getName(),ifile)) {
              setResult(new Result(true, R.string.ftp_success));
            } else {
              setResult(new Result(false, R.string.ftp_failure,mFTP.getReplyString()));
            }
        } catch (ConnectException e) {
          setResult(new Result(false, R.string.ftp_connection_refused));
        } catch (SocketException e) {
          setResult(new Result(false, R.string.ftp_socket_exception));
        } catch (FTPConnectionClosedException e) {
          setResult(new Result(false, R.string.ftp_connection_refused));
        } catch (IOException e) {
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
}
