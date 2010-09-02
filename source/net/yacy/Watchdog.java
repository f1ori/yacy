package net.yacy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import net.yacy.kelondro.util.OS;

import de.anomic.tools.tarTools;

public class Watchdog {
	
	private boolean restart = false;
	private boolean deployRelease = false;
	private File releaseFile = null;
	
	public static void sendShutdown() {
		System.out.println("Watchdog command: STOP");
	}
	
	public static void sendRestart() {
		System.out.println("Watchdog command: RESTART");
	}
	
	public static void sendDeploy(File releaseFile) {
		System.out.println("Watchdog command: DEPLOY " + releaseFile.getPath());
	}
	
	public void deployYacyRelease() {
		
	}
	
	public void runYaCy() {
		// load OS class, so we have it during update
		new OS();
		do {
			restart = true;
			deployRelease = false;
			try {
				String strClassPath = System.getProperty("java.class.path");
				System.out.println("Classpath is " + strClassPath);
				
				String[] arCmd = {"java", "-cp",  strClassPath, "net.yacy.yacy"};
				Process yacy = Runtime.getRuntime().exec(arCmd);
				
				// redirect stderr of yacy to this process' stderr
				CopyStreamsThread t = new CopyStreamsThread(yacy.getErrorStream(), System.err);
				t.start();
				
				// write stdout of yacy to our stdout
				BufferedReader yacyOutput = new BufferedReader(new InputStreamReader(yacy.getInputStream()));
				String line;
				while((line = yacyOutput.readLine()) != null) {
					System.out.println(line);
					if(line.contains("Watchdog command: RESTART")) {
						restart = true;
					} else if(line.contains("Watchdog command: STOP")) {
						restart = false;
					} else if(line.contains("Watchdog command: DEPLOY")) {
						deployRelease = true;
						String[] tokens = line.split(" ");
						releaseFile = new File(tokens[tokens.length - 1]);
					}
				}
				System.out.println("Watchdog Finished");
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(deployRelease) {
				if(!releaseFile.exists()) {
					// TODO: write to logfile
					System.err.println("release file " + releaseFile + " does not exist");
					System.err.println("restart YaCy without deploying release");
					continue;
				}
				try {
					tarTools.unTar(tarTools.getInputStream(releaseFile), ".", "yacy/");
					if(OS.canExecUnix) {
						// update file permissions, untar doesn't support file permissions
						Process p = Runtime.getRuntime().exec("chmod 755 *.sh bin/*.sh");
						p.waitFor();
					}
					System.err.println("deploying successful");
				} catch (Exception e) {
					System.err.println("installing release " + releaseFile + " failed");
					e.printStackTrace();
				}
			}
		} while(restart);
		
	}
	
	static public class CopyStreamsThread extends Thread {
		private InputStream input;
		private OutputStream output;
		
		public CopyStreamsThread(InputStream input, OutputStream output) {
			super();
			this.input = input;
			this.output = output;
			this.setDaemon(true);
		}
		
		public void run() {
			byte[] buffer = new byte[1024];
		    int bytesRead;
		    try {
		    	while ((bytesRead = input.read(buffer)) != -1)
		    	{
		    		output.write(buffer, 0, bytesRead);
		    	}
		    } catch(IOException e) {
		    }
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Watchdog watchdog = new Watchdog();
		watchdog.runYaCy();

	}

}
