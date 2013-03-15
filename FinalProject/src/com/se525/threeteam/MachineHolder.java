package com.se525.threeteam;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import android.R;
import android.content.res.AssetManager;
import android.widget.TextView;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class MachineHolder {
	
public MachineHolder(){
	machine = "";
	file = "";
}
  
  String machine;
  String file;
  
  public MachineHolder(String init) {
    machine = init;
  }
  
  public String getMachine() {
    return machine;
  }
  
  public void setMachine(String result) {
    this.machine = result;
  }
  
  public String getFile() {
	    return machine;
	  }
  
public void setFile(String result) {
	    this.machine = result;
	  }
  
  public void createFile() {
	  
	            
            String result = "m2!" + "(define (main result, machine)(.setResult result \"A NEW STRING\"))";
            
            setFile(result);
		    
			   

    
	  
  }
  
}

