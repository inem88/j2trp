package com.heimore.j2trp.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class ReverseProxy extends HttpServlet {
	
	private static final Logger LOG = Logger.getLogger(ReverseProxy.class);
	
	private String targetHost;
	private int targetPort;
	private String targetBaseUri;
	private String baseUri;
	private int proxiedPort;
	private String proxiedHost;
	private String proxiedProtocol;

	
	private static final long serialVersionUID = 1L;
	private static final Charset ASCII = Charset.forName("US-ASCII");
	private static final byte[] CR_LF = new byte[] { (byte) 0x0d, (byte) 0x0a };
	private static final int[] WELL_KNOWN_PORT = new int[] { 80, 443 }; // Array must be sorted.
	private static final byte[] HEADER_END_MARKER = new byte[] { (byte) 0x0d, (byte) 0x0a, (byte) 0x0d, (byte) 0x0a };
	
    @SuppressWarnings("unchecked")
    protected static void copyHeaders (OutputStream ps, HttpServletRequest request) throws IOException {
    	for (Enumeration<String> headers = request.getHeaderNames(); headers.hasMoreElements(); ) {
    		String headerName = (String) headers.nextElement();
    		if (!headerName.equalsIgnoreCase("Host")) {
        		for (Enumeration<String> headerValues = request.getHeaders(headerName); headerValues.hasMoreElements(); ) {
        			print(ps, headerName);
        			print(ps, ": ");
        			print(ps, headerValues.nextElement());
        			crlf(ps);
        		}
    		}
    		
    	}
    }
    
    protected static void copyHeadersFromResponse (Map<String, String> headers, HttpServletResponse resp) throws IOException {
    	for (Map.Entry<String, String> header : headers.entrySet()) {
    		if (header.getKey().equalsIgnoreCase("Set-Cookie")) {
    			// resp.addCookie(cookie)
    		}
    		resp.addHeader(header.getKey(), header.getValue());
    	}
    }
    // EXA_LOGIN_URL_RETENTION_COOKIE=http%3A%2F%2Ftestproxy.skolverket.se%3A443%2F; Domain=.skolverket.se; Path=/
    // JSESSIONID=C1A92D003ED13E1686389BC194E3F6FC; Path=/AuthenticationManager
    protected static List<HttpCookie> parseCookies (String cookieHeader) {
    	
    	List<HttpCookie> cookies = HttpCookie.parse(cookieHeader);
    	
    	return cookies;
    }
    
    protected static void copyCookies (OutputStream ps, HttpServletRequest request) throws IOException {
    	Cookie[] cookies = request.getCookies();
    	
    	if (cookies != null) {
    		print(ps, "Cookie: ");
        	for (int i = 0; i < cookies.length; i++) {
        		print(ps, cookies[i].getName());
        		print(ps, "=");
        		print(ps, cookies[i].getValue());
        		if (i < cookies.length - 1) {
        			print(ps, "; ");
        		}
        		else {
        			crlf(ps);
        		}
        	}

    	}
    }
    
    static boolean isWellKnownPort (int port) {
    	return (Arrays.binarySearch(WELL_KNOWN_PORT, port) >= 0);
    }
    
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		execute(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		execute(req, resp);
	}

	static void print (OutputStream os, String data) throws IOException {
		os.write(data.getBytes(ASCII));
	}
	
	static void crlf (OutputStream os) throws IOException {
		os.write(CR_LF);
	}
	
	static HttpStatus parseHeaders (String headersIncCrlf, Map<String, String> parsedHeaders) {
		
		String[] headers = headersIncCrlf.split("\\x0d\\x0a");
		HttpStatus result = new HttpStatus(headers[0]);
		for (int i = 1 ; i < headers.length; i++) {
			String header = headers[i];
			int indexOfHeaderSeparator = header.indexOf(": ");
			if (indexOfHeaderSeparator == -1) {
				LOG.debug(String.format("Header without valid syntax, discarding... (%s)", header));
			}
			else {
				parsedHeaders.put(header.substring(0, indexOfHeaderSeparator), header.substring(indexOfHeaderSeparator + 2));
			}
		}
		return result;
	}
	
	public void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Socket socket = null;
		byte[] bodyBuffer = new byte[1024];
		try {
			socket = new Socket(targetHost, targetPort);
			ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
			// PrintStream ps = new PrintStream(socket.getOutputStream());
			print(headerBuffer, request.getMethod());
			print(headerBuffer, " ");
			print(headerBuffer, request.getRequestURI());
			if (request.getQueryString() != null) {
				print(headerBuffer, "?");
				print(headerBuffer, request.getQueryString());
			}
			print(headerBuffer, " HTTP/1.0");
			crlf(headerBuffer);
			copyHeaders(headerBuffer, request);
			copyCookies(headerBuffer, request);
			print(headerBuffer, "X-Forwarded-For: ");
			print(headerBuffer, request.getRemoteAddr());
			crlf(headerBuffer);
			print(headerBuffer, "Host: "); // Add port?
			print(headerBuffer, targetHost);
			if (!isWellKnownPort(targetPort)) {
				print(headerBuffer, ":");
				print(headerBuffer, String.valueOf(targetPort));
			}
			crlf(headerBuffer);
			crlf(headerBuffer);
			
			// If request is a POST, relay the body of the request.
			if (request.getMethod().equals("POST")) {
				InputStream is = request.getInputStream();
				int bytesRead = is.read(bodyBuffer);
				while (bytesRead != -1) {
					headerBuffer.write(bodyBuffer, 0, bytesRead);
					bytesRead = is.read(bodyBuffer);
				}
			}
			
			// Write output to target server.
			OutputStream targetOutputStream = socket.getOutputStream();
			byte[] output = headerBuffer.toByteArray();
			System.out.println(new String(output, ASCII));
			targetOutputStream.write(output);
			targetOutputStream.flush();

			// Read response from target server.
			InputStream proxiedInputSteam = socket.getInputStream();
			OutputStream clientsRespOs = response.getOutputStream();
			ByteArrayOutputStream bufferedHeadersFromTarget = new ByteArrayOutputStream();
			int bytesRead = proxiedInputSteam.read(bodyBuffer);
			int markerIndex = 0;
			int totalBytesRead = 0;
			boolean headerFound = false;
			int headerMarker = 0;
			Map<String, String> headersFromTargetMap = new HashMap<String, String>();
					
			while (bytesRead != -1) {
				
				totalBytesRead += bytesRead;
				if (!headerFound) {
					bufferedHeadersFromTarget.write(bodyBuffer, 0, bytesRead);
				}
				// Scan for header marker...
				for (int i = 0; !headerFound && i < bytesRead; i++) {
					if (bodyBuffer[i] == HEADER_END_MARKER[markerIndex++]) {
						if (markerIndex == 4) {
							// Found marker?
							System.out.println("Found it @ " + (totalBytesRead - bytesRead + i));
							markerIndex = 0;
							headerFound = true;
							headerMarker = totalBytesRead - bytesRead + i - 3;
						}
					}
					else {
						markerIndex = 0;
					}
				}
				
				
				clientsRespOs.write(bodyBuffer, 0, bytesRead);
				bytesRead = proxiedInputSteam.read(bodyBuffer);
			}
			
			String allHeaders = new String(bufferedHeadersFromTarget.toByteArray(), 0, headerMarker, "ISO8859-1");
			System.out.println("---->" + allHeaders + "<----");
			HttpStatus httpStatus = parseHeaders(allHeaders, headersFromTargetMap);
			System.out.println(String.format("Target returned code %d (%s)", httpStatus.getCode(), httpStatus.getStatus()));
			targetOutputStream.close();
			clientsRespOs.close();
			response.setStatus(httpStatus.getCode());
			if (httpStatus.getCode() == HttpServletResponse.SC_FOUND) {
				URL redirectedUrl = new URL(headersFromTargetMap.get("Location"));
				String targetPath = redirectedUrl.getPath();
				// if (targetPath) // Gobble up the target base URI...
				response.sendRedirect(proxiedProtocol + "://" + proxiedHost + ":" + proxiedPort + baseUri);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (socket != null) {
				try {
					socket.close();
				}
				catch (IOException e) {
					// Don't care.
				}
			}
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		targetHost = config.getInitParameter("TARGET_HOST");
		targetPort = Integer.parseInt(config.getInitParameter("TARGET_PORT"));
		baseUri = config.getInitParameter("PROXIED_BASE_URI");
		// TODO: Sensible defaults....
		proxiedPort = Integer.parseInt(config.getInitParameter("PROXIED_PORT"));
		proxiedHost = config.getInitParameter("PROXIED_HOST");
		proxiedProtocol = config.getInitParameter("PROXIED_PROTOCOL");
	}

	
}
