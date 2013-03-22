/* Group Project
 * David Kuhn, Colleen Patin, Donald Bartoli
 * Exploring the consiquences of executing code
 * from potentially untrusted sources.
 */

package com.se525.threeteam;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import jscheme.JScheme;
import jsint.JavaMethod;
import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class MainActivity extends Activity {
  final private static String machine = "m1";
  static String hostname = "107.22.111.118";
  static String username = "ubuntu";
  static int port = 22;
  static String keyFileName = "dmkey.pem" ;
  final static String algorithmName = "SHA1withRSA";
  final static String filename = "bks_keystore";
  final static char[] keyStorePassword = "teamthree".toCharArray ();
  final static char[] aliasPassword = "rsa1se525".toCharArray ();
  private HashMap<String, String> keyPolicies = new HashMap<String, String>();
  private static ArrayList<String> permissions = new ArrayList();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
  readPolicyFile();
    
    // Jscheme instance to be used by all methods
  //  JScheme.setPerms(permissions);

    // Create layout
    LinearLayout ll = new LinearLayout(this);
    final TextView tv = new TextView (this);
    ll.setOrientation(LinearLayout.VERTICAL);
    ll.addView(tv);

    // First button that loads local scheme file and interprets it
    Button loadB = new Button (this);
    loadB.setText ("Poll");
    ll.addView (loadB);

    final EditText f1name = new EditText(this);
    f1name.setText("");
    ll.addView(f1name);
    Button button1 = new Button(this);
    button1.setText("load file"); 
    ll.addView(button1);
    
    
    
    
    loadB.setOnClickListener (new OnClickListener ()
    {
      public void onClick (View v) {
        final String filename = "ghghghgh.scm";
        new Thread(new Runnable() {
          public void run() {
            try {
              // Load the file
              poll(machine);
            } catch (Exception e) {
              tv.setText ("exception: " + e);
            }
            // Call the main method and pass text View for printing message
            //js.call ("main", tv);
          }
        }).start();
      }
    });

    button1.setOnClickListener (new OnClickListener ()
    {

      public void onClick (View v) {
        new Thread(new Runnable() {
          public void run() {
            try {
              // Load the file
              putFile("david.scm", "m1");
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }).start();
      }
    });

    setContentView (ll);

    Thread t = new Thread(new Runnable() {
      public void run() {
        try {
          // Check for programs to run
          poll(machine);
        } catch (Exception e) {
          tv.setText ("exception: " + e);
        }
      }
    }) ;
    t.start();
  }
  


  public Object getFile(JScheme js, String schemeFile) {
    try {
      Session session = getSession();
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


  public void putFile(String filename, String targetMachine) {
    try {
       byte[] signatureData = signFile2(filename);
      Session session = getSession();

      final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp") ;
      channel.connect();
      System.out.println("Listing Directory") ;

      // Only try to update the file if the two args are passed
      AssetManager assetMgr = this.getAssets();
      
      InputStream fis2 = assetMgr.open(filename);
      
      ByteArrayOutputStream buffer2 = new ByteArrayOutputStream();

      int nRead2;
      byte[] inputFileBytes = new byte[16384];
      int totalNum2 = 0;

      while ((nRead2 = fis2.read(inputFileBytes, 0, inputFileBytes.length)) != -1) {
        totalNum2 += nRead2;
        buffer2.write(inputFileBytes, 0, nRead2);
      }
      
      byte[] newBytes = Arrays.copyOf(inputFileBytes, totalNum2);
      ByteArrayInputStream bais = new ByteArrayInputStream (newBytes);
      channel.put(bais, "/home/ubuntu/m1/" + filename, ChannelSftp.OVERWRITE);
      
      ByteArrayInputStream bas = new ByteArrayInputStream (signatureData);
     
      channel.put(bas, "/home/ubuntu/" + targetMachine +"/"+ filename.split("\\.")[0] + ".scm.sig", ChannelSftp.OVERWRITE);

      channel.disconnect ();
      session.disconnect ();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {

    }
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
    while ((nRead = fis.read(prvKey, 0, prvKey.length)) != -1)
    {        buffer.write(prvKey, 0, nRead);      }   
    jsch.addIdentity (keyFileName,  prvKey, new byte[0], "password".getBytes());

 //   jsch.addIdentity (keyFileName,  prvKey, new byte[0], "password".getBytes());
    Session session = jsch.getSession(username, hostname, port);
    session.connect();
    return session;

  }
  
  /**
   * Polls task server for new tasks.
   * Only accepts tasks if there is a corresponding .sig file.
   * 
   * @param machineName Directory to check for new jobs
   */
  private void poll(String machineName) {
    int i= 0;
    try {
      while(true) {
        Thread.sleep(3000);
        Session session = getSession();
        JScheme js = new JScheme();
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
          String result = "";
          InputStream in = channel.get(machineName + "/" + text);
          InputStream in2 = channel.get(machineName + "/" + text);
          
          if (text.contains(".scm") && !text.contains("_resp") && !text.contains(".sig")) {

            InputStreamReader isr = new InputStreamReader(in);
            StringBuilder sb=new StringBuilder();
            BufferedReader br = new BufferedReader(isr);
            String read = br.readLine();

            while(read != null) {
              //System.out.println(read);
              sb.append(read);
              read = br.readLine();

            }
            result = sb.toString();
            System.out.println(result);

            String[] tokens = result.split("!");
            String target_machine = tokens[0];

            System.out.println(target_machine);
            String scm_file  = tokens[1];
            for (int j = 2; j<tokens.length; j++) {
              scm_file +=  "!";
              scm_file +=  tokens[j];
            }


            System.out.println(scm_file);
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
              boolean valid = checkSig(in2, sigIn, channel, target_machine);
              if (valid) {
                System.out.println("VALID!");
            //    JavaMethod.setPerms(keyPolicies.get(target_machine));

                js.load (scm_file);
                //TextView tv = new TextView (this);
                //   
                ResultHolder rh = new ResultHolder("test");
                MachineHolder mh = new MachineHolder();
                js.call("main", rh, mh);
                Log.e ("CSP", rh.getResult());
                String returnFile = "" + machineName + "!(define (main tv)(.setText tv \"" + rh.getResult() + "\")(.println System.out$ \"" + rh.getResult() + "\"))" ;
                ByteArrayInputStream bais = new ByteArrayInputStream (returnFile.getBytes("us-ascii"));
                channel.put(bais, "/home/ubuntu/" + target_machine +"/"+ text.split("\\.")[0] + "_resp.scm".trim(), ChannelSftp.OVERWRITE);
                KeyStore ks = KeyStore.getInstance ("BKS");
                 AssetManager assetMgr = this.getAssets();
                InputStream fis = assetMgr.open(filename);
                ks.load (fis, keyStorePassword);
                fis.close ();
                Signature signature = Signature.getInstance(algorithmName);
                PrivateKey privKey = (PrivateKey) ks.getKey (machineName, (machineName + "password").toCharArray());
                signature.initSign (privKey);
                signature.update (returnFile.trim().getBytes());
                byte[] signatureData = signature.sign ();
                ByteArrayInputStream bas = new ByteArrayInputStream (signatureData);
            //    ByteArrayInputStream bas = signFile(machine, returnFile);
                channel.put(bas, "/home/ubuntu/" + target_machine +"/"+ text.split("\\.")[0] + "_resp.scm.sig", ChannelSftp.OVERWRITE);

              } else {
                System.out.println("NOT VALID!");
              }
            }
          }
            else if(text.contains("_resp.scm") && (!text.contains(".sig"))){
                 InputStreamReader isr = new InputStreamReader(in);
                 StringBuilder sb=new StringBuilder();
                 BufferedReader br = new BufferedReader(isr);
                 String read = br.readLine();

                 while(read != null) {
                   //System.out.println(read);
                   sb.append(read);
                   read = br.readLine();

                 }
                 result = sb.toString();
                 System.out.println(result);

                 StringTokenizer tokenizer = new StringTokenizer(result, "!");
                 String target_machine = tokenizer.nextToken().toString();
                 System.out.println(target_machine);
                 String scm_file  = (tokenizer.nextToken()).toString().trim();
                 System.out.println(scm_file);
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
                   boolean valid = checkSig(in, sigIn, channel, target_machine);
                   if (valid) {
                     System.out.println("VALID!");
              in = channel.get("/home/ubuntu/" + machineName + "/" + text);
              js.load (new java.io.BufferedReader (new InputStreamReader(in)));
                   }
                   else
                           System.out.println("INVALID!!!");
            }
            }
          
          // Only try to update the file if the two args are passed
          //ByteArrayInputStream bais = new ByteArrayInputStream ("file.txt".getBytes("us-ascii"));
          i++;
          }
        channel.disconnect ();
        session.disconnect ();
        
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {

    }

  }


  /*********************************************************************\
   * Loads the file and its signature, signs the file and checks that it *
   * matches the stored signature.                                       *
   *                                                                     *
   * @param inputFilename InputStream of file to check against Signature *
   * @param signatureFilename InputStream of Signature file              *
   * @param channel ChannelSftp                                          *
   * @return true if signatures match                                    *
   \*********************************************************************/
  public boolean checkSig(InputStream inputFilename, InputStream signatureFilename, ChannelSftp channel, String publicAlias) 
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
    byte[] inputFileBytes = new byte[16384];
    int totalNum2 = 0;

    while ((nRead2 = inputFilename.read(inputFileBytes, 0, inputFileBytes.length)) != -1) {
      totalNum2 += nRead2;
      buffer2.write(inputFileBytes, 0, nRead2);
    }
    
    byte[] newBytes = Arrays.copyOf(inputFileBytes, totalNum2);

    System.out.println("TotalNum " + totalNum + "TotalNum2 " + totalNum2);
    Signature signature = Signature.getInstance (algorithmName);

    PrivateKey privKey = (PrivateKey) ks.getKey (publicAlias, (publicAlias + "password").toCharArray());
    signature.initSign (privKey);
    signature.update (newBytes);
    byte[] signatureData = signature.sign ();
    ByteArrayInputStream bais = new ByteArrayInputStream (signatureData);
    try {
      channel.put(bais, "form1.scm.sig", ChannelSftp.OVERWRITE);
    } catch (Exception e) {
      System.out.println("DIDN't work");
    }

    Certificate cert = (Certificate) ks.getCertificate (publicAlias);
    PublicKey pubKey = cert.getPublicKey ();
    signature.initVerify (pubKey);

    signature.update (newBytes);

    return signature.verify (sigFileBytes);
  }
  
  public ByteArrayInputStream signFile(String machineName, String returnFile){
          KeyStore ks = null;
          InputStream fis = null;
          ByteArrayInputStream bas = null;
try{
                ks = KeyStore.getInstance ("BKS");

            AssetManager assetMgr = this.getAssets();
            

                                fis = assetMgr.open(filename);

                                ks.load (fis, keyStorePassword);

   
            Signature signature = null;

            PrivateKey privKey=null;

                                privKey = (PrivateKey) ks.getKey (machineName, (machineName + "password").toCharArray());

                                signature.initSign (privKey);

                                signature.update (returnFile.getBytes());

            byte[] signatureData = null;

                                signatureData = signature.sign ();

         bas = new ByteArrayInputStream (signatureData);
            
} catch(Exception e){e.printStackTrace();}
            
            return bas;
  }
  
  
  
  
  private void readPolicyFile() {
    AssetManager assetMgr = this.getAssets();
    try {
      InputStream fis = assetMgr.open("keyPolicies.txt");
      InputStreamReader isr = new InputStreamReader(fis);
      StringBuilder sb=new StringBuilder();
      BufferedReader br = new BufferedReader(isr);
      String read = br.readLine();

      while(read != null) {
        //System.out.println(read);
        sb.append(read);
        System.out.println(read);
        StringTokenizer tokenizer = new StringTokenizer(read, ";");
        String target_machine = tokenizer.nextToken().toString();
        String permission = tokenizer.nextToken().toString();
        keyPolicies.put(target_machine, permission);
        read = br.readLine();

      }
      
      
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public byte[] signFile2(String newfile){
    System.out.println("SIGNING FILE");
InputStream fis = null;
  byte[] signatureData = null;
  try{
  KeyStore ks = KeyStore.getInstance ("BKS");
  AssetManager assetMgr = this.getAssets();
  
  InputStream fis2 = assetMgr.open(newfile);
  
  ByteArrayOutputStream buffer2 = new ByteArrayOutputStream();

  int nRead2;
  byte[] inputFileBytes = new byte[16384];
  int totalNum2 = 0;

  while ((nRead2 = fis2.read(inputFileBytes, 0, inputFileBytes.length)) != -1) {
    totalNum2 += nRead2;
    buffer2.write(inputFileBytes, 0, nRead2);
  }
  
  byte[] newBytes = Arrays.copyOf(inputFileBytes, totalNum2);
  System.out.println("newBytes" + newBytes);
  fis = assetMgr.open(filename);

  ks.load (fis, keyStorePassword);
  fis.close ();

  Signature signature = Signature.getInstance (algorithmName);


   PrivateKey privKey = (PrivateKey) ks.getKey (machine, (machine + "password").toCharArray());
    signature.initSign (privKey);
  

  signature.update (newBytes);

 
  signatureData = signature.sign ();
    
  }catch(Exception e){e.printStackTrace();}
    
    return signatureData;
 

  
  }
}