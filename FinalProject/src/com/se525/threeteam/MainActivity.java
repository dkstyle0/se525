package com.se525.threeteam;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

import jscheme.JS;
import jscheme.JScheme;
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
import com.jcraft.jsch.Session;

public class MainActivity extends Activity {

  static String hostname = "107.22.111.118";
  static String username = "ubuntu";
  static int port = 22;
  static String keyFileName = "new_rsa" ;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Jscheme instance to be used by all methods
    final JScheme js = new JScheme();

    // Create layout
    LinearLayout ll = new LinearLayout(this);
    final TextView tv = new TextView (this);
    ll.setOrientation(LinearLayout.VERTICAL);
    ll.addView(tv);
    
        // First button that loads local scheme file and interprets it
    Button loadB = new Button (this);
    loadB.setText ("Put test file");
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
              poll("m2");
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
            	  sendText("/mnt/sdcard/form3.txt");
              } catch (Exception e) {
                e.printStackTrace();
              }
              // Call the main method and pass text View for printing message
              //js.call ("main", tv);
            }
          }).start();
        }
    });
  
  
    setContentView (ll);

    Thread t = new Thread(new Runnable() {
      public void run() {
        try {
          // Check for programs to run
          poll("m2");
        } catch (Exception e) {
          tv.setText ("exception: " + e);
        }
        // Call the main method and pass text View for printing message
        //js.call ("main");
      }
    }) ;
    t.start();


  }
  
  public void createFile() {
	  
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
  
  public void sendText(String filename){

		    BufferedReader reader = null;
		    StringBuilder  sb = new StringBuilder();
			try {
				reader = new BufferedReader( new FileReader (filename));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    String         l = null;
		  
		

		    try {
				while( ( l = reader.readLine() ) != null ) {
				    sb.append( l );
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            String result = sb.toString();
            
            putFile(result);
		    
		
  }

  public void putFile(String filename) {
    try {
      Session session = getSession();

      final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp") ;
      channel.connect();
      System.out.println("Listing Directory") ;

      // Only try to update the file if the two args are passed
      ByteArrayInputStream bais = new ByteArrayInputStream (filename.getBytes("us-ascii"));
      channel.put(bais, "/home/ubuntu/m2/form3.scm", ChannelSftp.OVERWRITE);

      channel.disconnect ();
      session.disconnect ();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {

    }
  }
  
  public  void putString(String filetext){
	    try {
	        Session session = getSession();

	        final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp") ;
	        channel.connect();
	       

	  	  System.out.println("Uploading message to remote server");
	      ByteArrayInputStream bais = new ByteArrayInputStream(filetext.getBytes ("us-ascii"));
	      int mode = ChannelSftp.OVERWRITE;
	     // chan.cd("temp");
	      channel.put(bais, "form4.scm", mode);

	        channel.disconnect ();
	        session.disconnect ();
	      } catch (Exception e) {
	        e.printStackTrace();
	      } finally {

	      }
	  
  }
/*
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }*/

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

    jsch.addIdentity (keyFileName,  prvKey, new byte[0], "password".getBytes());
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
          if (text.contains(".scm") && !text.contains("_resp")) {
            String result = "";
            InputStream in = channel.get(machineName + "/" + text);
            InputStreamReader isr = new InputStreamReader(in);
            StringBuilder sb=new StringBuilder();
            BufferedReader br = new BufferedReader(isr);
            String read = br.readLine();

            while(read != null) {
                //System.out.println(read);
                sb.append(read);
                read =br.readLine();

            }
            result = sb.toString();
            System.out.println(result);
            
            StringTokenizer tokenizer = new StringTokenizer(result, "!");
            String target_machine = tokenizer.nextToken().toString();
            System.out.println(target_machine);
            String scm_file  = (tokenizer.nextToken()).toString().trim();
            System.out.println(scm_file);
            
            js.load (scm_file);
       //     TextView tv = new TextView (this);
            //   
            ResultHolder rh = new ResultHolder("test");
            MachineHolder mh = new MachineHolder();
            js.call("main", rh, mh);
            Log.e ("CSP", rh.getResult());
            String returnFile = "(define (main tv)(.setText tv \"" + rh.getResult() + "\")(.println System.out$ \"" + rh.getResult() + "\"))" ;
            ByteArrayInputStream bais = new ByteArrayInputStream (returnFile.getBytes("us-ascii"));
            channel.put(bais, "/home/ubuntu/" + target_machine +"/"+ text.split("\\.")[0] + "_resp.scm", ChannelSftp.OVERWRITE);
          }
          else if(text.contains("_resp.scm")){
          	InputStream in = channel.get("/home/ubuntu/" + machineName + "/" + text);
            js.load (new java.io.BufferedReader (new InputStreamReader(in)));
        	TextView tv = new TextView (this);
        	js.call("main",tv);
        	
        	  
        	  
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
}
