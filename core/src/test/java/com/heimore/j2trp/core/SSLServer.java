package com.heimore.j2trp.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.SocketException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.springframework.http.converter.ByteArrayHttpMessageConverter;

public class SSLServer implements Runnable {

	private static final byte[] CR_LF = new byte[] { (byte) 0x0d, (byte) 0x0a };
	
	int port;
	SSLServerSocket server;
	Thread reader = new Thread(this, "SSL Server reader thread for unit tests.");
	volatile boolean shouldRun;
	
	SSLServer (int pPort) throws IOException {
		port = pPort;
		SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		server = (SSLServerSocket) factory.createServerSocket(port);
	}
	
	public void start() {
		shouldRun = true;
		reader.start();
	}
	
	public void stop() {
		try {
			shouldRun = false;
			server.close();
		}
		catch (IOException e) {
			// Swallow
		}
		
		try {
			System.out.println("Trying to get the accept thread to join.");
			reader.interrupt();
			reader.join(2000);
			System.out.println("Join completed, thread is: " + reader.getState());
		}
		catch (InterruptedException e) {
			// Do nothing.
		}
		
	}
	
	@Override
	public void run() {
		while (shouldRun) {
			try {
				SSLSocket socket = (SSLSocket) server.accept();
				System.out.println("Received SSL connection request using: " + socket.getSession().getCipherSuite());
				ResponseHandler rh = new ResponseHandler(socket);
				rh.process();
			}
			catch (SocketException e) {
				// stop() was probably called. Exit.
			}
			catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
		System.out.println("Shutting down SSL Server...");
	}
	
	
	private static class ResponseHandler {
		SSLSocket socket;
		
		ResponseHandler (SSLSocket socket) {
			this.socket = socket;
		}
		
		void process() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
				String line = br.readLine();
				// First line should be the HTTP verb, resource and version.
				System.out.println("line: " + line);
				
				if (line == null || line.contains("/fake_an_error")) {
					throw new IOException("Fake an I/O error.");
				}
				OutputStream os = socket.getOutputStream();
				ByteArrayOutputStream resp = new ByteArrayOutputStream();
				resp.write("HTTP/1.0 200 OK".getBytes("UTF-8"));
				resp.write(CR_LF);
				resp.write(CR_LF);
				os.write(resp.toByteArray());
				os.flush();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			finally {
				try {
					socket.close();
				}
				catch (IOException e) {
					// Do nothing.
				}
			}
		}
	}
 
}
