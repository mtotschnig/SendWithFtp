SendWithFtp
===========

SendWithFtp is an Android App that provides intents for sharing files through FTP

SendWithFtp uses FTP library from http://commons.apache.org/net/

SendWithFtp responds to two intents

1) android.intent.action.SEND:
  The user manages a list of FTP URIs and selects one as the target for the file being shared.

2) android.intent.action.SENDTO if the URI in the data is of scheme ftp:
  The application provides the FTP uri and SendWithFtp immediately triggers the upload
