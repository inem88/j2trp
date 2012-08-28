package com.heimore.j2trp.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
    
    protected static List<Cookie> convertCookies (List<HttpCookie> cookies, String targetBaseUri, String baseUri) {
    	
    	List<Cookie> result = new ArrayList<Cookie>(cookies.size());
    	
    	for (HttpCookie cookie : cookies) {
    		
    		Cookie convertedCookie = new Cookie(cookie.getName(), cookie.getValue());
    		convertedCookie.setComment(cookie.getComment());
    		if (cookie.getDomain() != null) {
    			convertedCookie.setDomain(cookie.getDomain());
    		}
    		convertedCookie.setMaxAge((int) cookie.getMaxAge());
    		String cookiePath = cookie.getPath();
    		if (cookiePath != null && cookiePath.startsWith(targetBaseUri)) {
    			StringBuilder sb = new StringBuilder(cookiePath);
    			sb.delete(0, cookiePath.length());
    			sb.insert(0, baseUri);
    			cookiePath = sb.toString();
    		}
    		convertedCookie.setPath(cookiePath);
    		convertedCookie.setSecure(cookie.getSecure());
    		convertedCookie.setVersion(cookie.getVersion());
    		result.add(convertedCookie);
    	}
    	
    	return result;
    }
    
    protected static void copyCookiesFromResponse (List<String> rawCookieValues, HttpServletResponse resp, String targetBaseUri, String baseUri) {
    	List<HttpCookie> allCookies = new ArrayList<HttpCookie>();
    	for (String rawCookie : rawCookieValues) {
    		allCookies.addAll(HttpCookie.parse(rawCookie));
    	}
    	
    	for (Cookie cookie : convertCookies(allCookies, targetBaseUri, baseUri)) {
    		resp.addCookie(cookie);
    	}
    	
    }
    
    protected static void copyHeadersFromResponse (Map<String, List<String>> headers, HttpServletResponse resp, String targetBaseUri, String baseUri) throws IOException {
    	for (Map.Entry<String, List<String>> header : headers.entrySet()) {
    		if (header.getKey().equalsIgnoreCase("Set-Cookie")) {
    			copyCookiesFromResponse(header.getValue(), resp, targetBaseUri, baseUri);
    			continue;
    		}
    		if (header.getKey().equalsIgnoreCase("Set-Cookie2")) {
    			copyCookiesFromResponse(header.getValue(), resp, targetBaseUri, baseUri);
    			continue;
    		}
    		if (header.getKey().equalsIgnoreCase("Location")) {
    			System.out.println("Suppressing copying of Location header.");
    			continue;
    		}
    		for (String value : header.getValue()) {
    			resp.addHeader(header.getKey(), value);
    		}
    		
    	}
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
	
	static HttpStatus parseHeaders (String headersIncCrlf, Map<String, List<String>> parsedHeaders) {
		
		String[] headers = headersIncCrlf.split("\\x0d\\x0a");
		HttpStatus result = new HttpStatus(headers[0]);
		for (int i = 1 ; i < headers.length; i++) {
			String header = headers[i];
			int indexOfHeaderSeparator = header.indexOf(": ");
			if (indexOfHeaderSeparator == -1) {
				LOG.debug(String.format("Header without valid syntax, discarding... (%s)", header));
			}
			else {
				String headerKey = header.substring(0, indexOfHeaderSeparator);
				List<String> headerValue = parsedHeaders.get(headerKey);
				if (headerValue == null) {
					headerValue = new ArrayList<String>();
					parsedHeaders.put(headerKey, headerValue);
				}
				headerValue.add(header.substring(indexOfHeaderSeparator + 2));
			}
		}
		return result;
	}
	
	String getProxiedUri (String srcUri) {
		if (srcUri.startsWith(baseUri)) {
			StringBuilder sb = new StringBuilder(srcUri);
			sb.delete(0, baseUri.length());
			sb.insert(0, targetBaseUri);
			return sb.toString();
		}
		return srcUri;
	}
	
	public void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Socket socket = null;
		byte[] bodyBuffer = new byte[1024];
		try {
			// Connect to target.
			socket = new Socket(targetHost, targetPort);
			ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
			// Build HTTP verb and request URI.
			print(headerBuffer, request.getMethod());
			print(headerBuffer, " ");
			String requestUri = getProxiedUri(request.getRequestURI());
			print(headerBuffer, requestUri);
			if (request.getQueryString() != null) {
				print(headerBuffer, "?");
				print(headerBuffer, request.getQueryString());
			}
			print(headerBuffer, " HTTP/1.0");
			crlf(headerBuffer);
			// Ok, verb and request URI complete, proceed with the headers.
			copyHeaders(headerBuffer, request);
			copyCookies(headerBuffer, request);
			// Add XFF header. TODO: Merge with pre-existing ones.
			print(headerBuffer, "X-Forwarded-For: ");
			print(headerBuffer, request.getRemoteAddr());
			crlf(headerBuffer);
			// Add proxied Host header.
			print(headerBuffer, "Host: "); // Add port?
			print(headerBuffer, targetHost);
			// Special handling of known HTTP ports (basically ports other than 80 and 443)
			if (!isWellKnownPort(targetPort)) {
				print(headerBuffer, ":");
				print(headerBuffer, String.valueOf(targetPort));
			}
			// Ok, end header with two CRLFs
			crlf(headerBuffer);
			crlf(headerBuffer);
			// Header now complete.
			
			ByteArrayOutputStream httpBodyBuffer = new ByteArrayOutputStream();
			// If request is a POST, relay the body of the request.
			if (request.getMethod().equals("POST")) {
				InputStream is = request.getInputStream();
				int bytesRead = is.read(bodyBuffer);
				while (bytesRead != -1) {
					httpBodyBuffer.write(bodyBuffer, 0, bytesRead);
					bytesRead = is.read(bodyBuffer);
				}
			}
			
			// Write output to target server.
			OutputStream targetOutputStream = socket.getOutputStream();
			byte[] headerBufferAsBytes = headerBuffer.toByteArray();
			byte[] httpBodyBufferAsBytes = httpBodyBuffer.toByteArray();
			System.out.println("Header to target: " + new String(headerBufferAsBytes, ASCII));
			System.out.println("Body to target: " + new String(httpBodyBufferAsBytes, ASCII));
			targetOutputStream.write(headerBufferAsBytes);
			targetOutputStream.write(httpBodyBufferAsBytes);
			targetOutputStream.flush();
			// Ok, done with pushing out data to the target server.

			// Read response from target server.
			InputStream proxiedInputSteam = socket.getInputStream();
			OutputStream clientsRespOs = response.getOutputStream();
			ByteArrayOutputStream bufferedHeadersFromTarget = new ByteArrayOutputStream();
			int bytesRead = proxiedInputSteam.read(bodyBuffer);
			// META-DATA
			int markerIndex = 0;
			boolean headerFound = false;
			int bodyMarker = 0;
			Map<String, List<String>> headersFromTargetMap = new HashMap<String, List<String>>();
			HttpStatus httpStatus = null;	
			boolean headerAlreadyWritten = false;
			
			while (bytesRead != -1) {
				
				// Scan for header marker...
				for (int i = 0; !headerFound && i < bytesRead; i++) {
					if (bodyBuffer[i] == HEADER_END_MARKER[markerIndex++]) {
						if (markerIndex == 4) {
							// Found marker?
							headerFound = true;
							bufferedHeadersFromTarget.write(bodyBuffer, 0, i);
							
							String allHeaders = new String(bufferedHeadersFromTarget.toByteArray(), 0, bufferedHeadersFromTarget.size() - 3, "ISO8859-1");
							System.out.println("---->" + allHeaders + "<----");
							httpStatus = parseHeaders(allHeaders, headersFromTargetMap);
							System.out.println(String.format("Target returned code %d (%s)", httpStatus.getCode(), httpStatus.getStatus()));
							bodyMarker = i + 1;
						}
					}
					else {
						markerIndex = 0;
					}
				}
				// No header market found yet, copy all bytes to the header buffer.
				if (!headerFound) {
					bufferedHeadersFromTarget.write(bodyBuffer, 0, bytesRead);
				}
				
				// Ok, any manipulation of the header should occur BEFORE the body...
				if (headerFound) {
					if (!headerAlreadyWritten) {
						response.setStatus(httpStatus.getCode());
						copyHeadersFromResponse(headersFromTargetMap, response, targetBaseUri, baseUri);
						if (httpStatus.getCode() == HttpServletResponse.SC_FOUND) {
							URL redirectedUrl = new URL(headersFromTargetMap.get("Location").get(0));
							String targetPath = redirectedUrl.getFile();
							String translatedPath = targetPath;
							if (targetPath.startsWith(targetBaseUri)) {
								StringBuilder sb = new StringBuilder(targetPath);
								sb.delete(0, targetBaseUri.length());
								sb.insert(0, baseUri);
								translatedPath = sb.toString();
							}
							response.sendRedirect(proxiedProtocol + "://" + proxiedHost + ":" + proxiedPort + translatedPath);
						}
						headerAlreadyWritten = true;
					}
					clientsRespOs.write(bodyBuffer, bodyMarker, bytesRead - bodyMarker);
					bodyMarker = 0;
				}
				bytesRead = proxiedInputSteam.read(bodyBuffer);
			}
			
			
			targetOutputStream.close(); // TODO: Handle keep-alives.
			clientsRespOs.close();
			
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
		targetBaseUri = config.getInitParameter("TARGET_BASE_URI");
		proxiedHost = config.getInitParameter("PROXIED_HOST");
		proxiedProtocol = config.getInitParameter("PROXIED_PROTOCOL");
	}

	
}
