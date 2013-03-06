package com.se525.threeteam;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import jscheme.JScheme;
import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class MainActivity extends Activity {

  static String hostname = "107.22.111.118";
  static String username = "ubuntu";
  static int port = 22;
  static String keyFileName = "dmkey.pem" ;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Create layout
    LinearLayout ll = new LinearLayout(this);
    final TextView tv = new TextView (this);
    ll.setOrientation(LinearLayout.VERTICAL);
    ll.addView(tv);

    // Jscheme instance to be used by all methods
    final JScheme js = new JScheme();

    // First button that loads local scheme file and interprets it
    Button loadB = new Button (this);
    loadB.setText ("Put test file");

    loadB.setOnClickListener (new OnClickListener ()
    {
      public void onClick (View v) {
        final String filename = "test1.scm";
        new Thread(new Runnable() {
          public void run() {
            try {
              // Load the file
              putFile(filename);
            } catch (Exception e) {
              tv.setText ("exception: " + e);
            }
            // Call the main method and pass text View for printing message
            //js.call ("main", tv);
          }
        }).start();
      }
    });
    ll.addView (loadB);
    setContentView (ll);

  }

  public Object getFile(JScheme js, String schemeFile) {
    try {
      JSch jsch = new JSch() ;
      JSch.setConfig("StrictHostKeyChecking" , "no");
      AssetManager assetMgr = this.getAssets();
      InputStream fis = assetMgr.open(keyFileName);
      //InputStream fis = getResources().openRawResource(R.raw.android_rsa);
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      int nRead;
      byte[] prvKey = new byte[16384];

      while ((nRead = fis.read(prvKey, 0, prvKey.length)) != -1) {
        buffer.write(prvKey, 0, nRead);
      }

      jsch.addIdentity (keyFileName,  prvKey, new byte[0], new byte[0]);
      Session session = jsch.getSession(username, hostname, port);
      session.connect();

      final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp") ;
      channel.connect();

      InputStream in = channel.get(schemeFile);
      Object result = js.load (new java.io.BufferedReader (new InputStreamReader(in)));
      TextView tv = new TextView (this);
      js.call("main", tv);
      return result;
    } catch (Exception e) {
      e.printStackTrace();
      return "ERROR";
    }
  }

  public void putFile(String filename) {
    try {
      Session session = getSession();

      final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp") ;
      channel.connect();
      System.out.println("Listing Directory") ;

      // Only try to update the file if the two args are passed
      ByteArrayInputStream bais = new ByteArrayInputStream ("file.txt".getBytes("us-ascii"));
      channel.put(bais, filename, ChannelSftp.OVERWRITE);

      channel.disconnect ();
      session.disconnect ();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {

    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }

  private Session getSession() throws Exception {
    JSch jsch = new JSch() ;
    JSch.setConfig("StrictHostKeyChecking" , "no");
    AssetManager assetMgr = this.getAssets();
    InputStream fis = assetMgr.open(keyFileName);
    //InputStream fis = getResources().openRawResource(R.raw.android_rsa);
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    int nRead;
    byte[] prvKey = new byte[16384];

    while ((nRead = fis.read(prvKey, 0, prvKey.length)) != -1) {
      buffer.write(prvKey, 0, nRead);
    }

    jsch.addIdentity (keyFileName,  prvKey, new byte[0], new byte[0]);
    Session session = jsch.getSession(username, hostname, port);
    session.connect();
    return session;

  }
}
