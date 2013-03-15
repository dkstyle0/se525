package com.se525.threeteam;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Vector;

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
import com.jcraft.jsch.SftpException;

public class MainActivity extends Activity {

  static String hostname = "107.22.111.118";
  static String username = "ubuntu";
  static int port = 22;
  static String keyFileName = "dmkey.pem" ;
  final static String algorithmName = "SHA1withRSA";
  final static String filename = "bks_keystore";
  final static char[] keyStorePassword = "teamthree".toCharArray ();
  final static char[] aliasPassword = "rsa1se525".toCharArray ();

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
              poll("m1");
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

    Thread t = new Thread(new Runnable() {
      public void run() {
        try {
          // Check for programs to run
          poll("m1");
        } catch (Exception e) {
          tv.setText ("exception: " + e);
        }
        // Call the main method and pass text View for printing message
        //js.call ("main", tv);
      }
    }) ;
    t.start();


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

  private void poll(String machineName) {
    int i= 0;
    try {
      while(!Thread.currentThread().isInterrupted() && i < 1) {
        Thread.sleep(3000);
        Session session = getSession();

        final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp") ;
        channel.connect();
        System.out.println("Listing Directory") ;
        Vector v = channel.ls("/home/ubuntu/" + machineName);
        for (Object o : v) {
          String text;
          if (o instanceof com.jcraft.jsch.ChannelSftp.LsEntry) {
            text = ((com.jcraft.jsch.ChannelSftp.LsEntry) o).getFilename();
          } else {
            text = o.toString ();
          }
          System.out.println(text);
          if (text.contains(".scm") && !text.contains(".sig")) {
            final JScheme js = new JScheme();
            InputStream in = channel.get(machineName + "/" + text);
            InputStream sigIn = null;
            boolean sigFound = false;
            try {
              sigIn = channel.get(machineName + "/" + text + ".sig");
              sigFound = true;
            } catch (SftpException se) {
              se.printStackTrace();
              System.out.println("SIG FILE NOT FOUND: " + text);
            }
            if (sigFound) {
              boolean valid = checkSig(in, sigIn, channel);
              if (valid) {
                System.out.println("VALID!");
                Object result = js.load (new java.io.BufferedReader (new InputStreamReader(in)));
                TextView tv = new TextView (this);
                //   
                ResultHolder rh = new ResultHolder("test");
                js.call("main", rh);
                Log.e ("CSP", rh.getResult());
                String returnFile = "(define (main tv)(.setText tv \"" + rh.getResult() + "\"))" ;
                ByteArrayInputStream bais = new ByteArrayInputStream (returnFile.getBytes("us-ascii"));
                channel.put(bais, text.split("\\.")[0] + "_resp.scm", ChannelSftp.OVERWRITE);
              } else {
                System.out.println("NOT VALID!");
              }
            }
          }
        }
        // Only try to update the file if the two args are passed
        //ByteArrayInputStream bais = new ByteArrayInputStream ("file.txt".getBytes("us-ascii"));
        i++;

        channel.disconnect ();
        session.disconnect ();
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {

    }

  }
  
  public boolean checkSig(InputStream inputFilename, InputStream signatureFilename, ChannelSftp channel) 
    throws GeneralSecurityException, IOException {
    KeyStore ks = KeyStore.getInstance ("BKS");
    AssetManager assetMgr = this.getAssets();
    InputStream fis = assetMgr.open(filename);
    //FileInputStream fis = new FileInputStream (filename);
    ks.load (fis, keyStorePassword);
    fis.close ();
    
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    int nRead;
    int totalNum = 0;
    byte[] sigFileBytes = new byte[256];

    while ((nRead = signatureFilename.read(sigFileBytes, 0, sigFileBytes.length)) != -1) {
      totalNum += nRead;
      buffer.write(sigFileBytes, 0, nRead);
    }
    
    ByteArrayOutputStream buffer2 = new ByteArrayOutputStream();

    int nRead2;
    byte[] inputFileBytes = new byte[157];
    int totalNum2 = 0;
    
    while ((nRead2 = inputFilename.read(inputFileBytes, 0, inputFileBytes.length)) != -1) {
      totalNum2 += nRead2;
      buffer2.write(inputFileBytes, 0, nRead2);
    }
    System.out.println("TotalNum " + totalNum + "TotalNum2 " + totalNum2);
    Signature signature = Signature.getInstance (algorithmName);
    
    PrivateKey privKey = (PrivateKey) ks.getKey ("myproject2", aliasPassword);
    signature.initSign (privKey);
    signature.update (inputFileBytes);
    byte[] signatureData = signature.sign ();
    ByteArrayInputStream bais = new ByteArrayInputStream (signatureData);
    try {
    channel.put(bais, "form1.scm.sig", ChannelSftp.OVERWRITE);
    } catch (Exception e) {
      System.out.println("DIDN't work");
    }
    
    Certificate cert = (Certificate) ks.getCertificate ("myproject2");
    PublicKey pubKey = cert.getPublicKey ();
    signature.initVerify (pubKey);

    signature.update (inputFileBytes);

    return signature.verify (sigFileBytes);
  }
  
  
}
