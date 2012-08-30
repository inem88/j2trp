package com.heimore.j2trp.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class ReverseProxy extends HttpServlet {
	
	private static final Logger LOG = Logger.getLogger(ReverseProxy.class);
	private static final String LS = System.getProperty("line.separator");
	
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
	private static final String HTTP_VERB = "HTTP VERB";
	
    @SuppressWarnings("unchecked")
    protected static void copyHeaders (OutputStream ps, HttpServletRequest request) throws IOException {
    	
    	boolean foundXFFHeaders = false;
    	final String XFF = "X-Forwarded-For";
    	
    	for (Enumeration<String> headers = request.getHeaderNames(); headers.hasMoreElements(); ) {
    		String headerName = (String) headers.nextElement();
    		if (headerName.equalsIgnoreCase(XFF)) {
    			foundXFFHeaders = true;
    			continue;
    		}
    		if (!headerName.equalsIgnoreCase("Host")) {
        		for (Enumeration<String> headerValues = request.getHeaders(headerName); headerValues.hasMoreElements(); ) {
        			StringBuilder headerBuffer = new StringBuilder();
        			headerBuffer.append(headerName);
        			headerBuffer.append(": ");
        			String headerValue = headerValues.nextElement();
        			headerBuffer.append(headerValue);
        			print(ps, headerBuffer.toString());
        			crlf(ps);
        		}
    		}
    		
    		
    	}
    	
    	print(ps, XFF);
    	print(ps, ": ");
    	if (foundXFFHeaders) {
    		for (Enumeration<String> headerValues = request.getHeaders(XFF); headerValues.hasMoreElements(); ) {
    			print(ps, headerValues.nextElement());
    			print(ps, ", ");
    		}
    	}
    	print(ps, request.getRemoteAddr());
    	crlf(ps);
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
	
	static void addHeader (Map<String, List<String>> map, String key, String value) {
		List<String> headerValue = map.get(key);
		if (headerValue == null) {
			headerValue = new ArrayList<String>();
			map.put(key, headerValue);
		}
		headerValue.add(value);
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
				addHeader(parsedHeaders, headerKey, header.substring(indexOfHeaderSeparator + 2));
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
	
	private static String dumpEnumeration (Enumeration<String> bag) {
		StringBuilder sb = new StringBuilder();
		
		int nrOfElements = 0;
		while (bag.hasMoreElements()) {
			sb.append(bag.nextElement());
			if (bag.hasMoreElements()) {
				sb.append(", ");
			}
			nrOfElements++;
		}
		
		if (nrOfElements > 1) {
			sb.insert(0, "[");
			sb.append("]");
		}
		
		return sb.toString();
	}
	
	public void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Socket socket = null;
		byte[] byteBuffer = new byte[1024];
		try {
			// Connect to target.
			try {
			 socket = new Socket(targetHost, targetPort);
			}
			catch (UnknownHostException e) {
				String errorCode = UUID.randomUUID().toString();
				String msg = String.format("Target server not reachable, your personal error code is %s, please contact support and provide this error code.", errorCode);
				String logMsg = String.format("Generated error code %s", errorCode);
				LOG.error(logMsg, e);
				response.sendError(HttpServletResponse.SC_BAD_GATEWAY, msg);
				return;
			}
			catch (IOException e) {
				String errorCode = UUID.randomUUID().toString();
				String msg = String.format("I/O error to the target, your personal error code is %s, please contact support and provide this error code.", errorCode);
				String logMsg = String.format("Generated error code %s", errorCode);
				LOG.error(logMsg, e);
				response.sendError(HttpServletResponse.SC_BAD_GATEWAY, msg);
				return;
			}
			ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
			// Build HTTP verb and request URI.
			StringBuilder httpVerb = new StringBuilder();
			httpVerb.append(request.getMethod());
			httpVerb.append(" ");
			String requestUri = getProxiedUri(request.getRequestURI());
			httpVerb.append(requestUri);
			if (request.getQueryString() != null) {
				httpVerb.append("?");
				httpVerb.append(request.getQueryString());
			}
			httpVerb.append(" HTTP/1.0");
			print(headerBuffer, httpVerb.toString());
			crlf(headerBuffer);
			// Ok, verb and request URI complete, proceed with the headers.
			copyHeaders(headerBuffer, request);
			copyCookies(headerBuffer, request);
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
				int bytesRead = is.read(byteBuffer);
				while (bytesRead != -1) {
					httpBodyBuffer.write(byteBuffer, 0, bytesRead);
					bytesRead = is.read(byteBuffer);
				}
			}
			
			// Write output to target server.
			OutputStream targetOutputStream = socket.getOutputStream();
			byte[] headerBufferAsBytes = headerBuffer.toByteArray();
			byte[] httpBodyBufferAsBytes = httpBodyBuffer.toByteArray();
			targetOutputStream.write(headerBufferAsBytes);
			targetOutputStream.write(httpBodyBufferAsBytes);
			targetOutputStream.flush();
			// Ok, done with pushing out data to the target server.

			// Read response from target server.
			InputStream proxiedInputSteam = socket.getInputStream();
			OutputStream clientsRespOs = response.getOutputStream();
			ByteArrayOutputStream bufferedHeadersFromTarget = new ByteArrayOutputStream();
			int bytesRead = proxiedInputSteam.read(byteBuffer);
			// META-DATA
			int markerIndex = 0;
			boolean headerFound = false;
			int bodyMarker = 0;
			Map<String, List<String>> headersFromTargetMap = new HashMap<String, List<String>>();
			HttpStatus httpStatus = null;	
			boolean headerAlreadyWritten = false;
			String redirectUrl = "";
			
			while (bytesRead != -1) {
				
				// Scan for header marker...
				for (int i = 0; !headerFound && i < bytesRead; i++) {
					if (byteBuffer[i] == HEADER_END_MARKER[markerIndex++]) {
						if (markerIndex == 4) {
							// Found marker?
							headerFound = true;
							bufferedHeadersFromTarget.write(byteBuffer, 0, i);
							
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
				// No header marker found yet, copy all bytes to the header buffer.
				if (!headerFound) {
					bufferedHeadersFromTarget.write(byteBuffer, 0, bytesRead);
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
							redirectUrl = response.encodeRedirectURL(proxiedProtocol + "://" + proxiedHost + ":" + proxiedPort + translatedPath);
							response.sendRedirect(redirectUrl);
						}
						headerAlreadyWritten = true;
					}
					clientsRespOs.write(byteBuffer, bodyMarker, bytesRead - bodyMarker);
					bodyMarker = 0;
				}
				bytesRead = proxiedInputSteam.read(byteBuffer);
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format("Proxied request \"%s\" -> \"%s\" (%d) \"%s\"", request.getRequestURI(), requestUri, httpStatus.getCode(), redirectUrl)); 
				StringBuffer sb = new StringBuffer(1024);
				sb.append(LS);
				dumpIncomingHeaders(request, sb);
				dumpReturningHeader(headersFromTargetMap, sb);
				LOG.debug(sb);
			}
					
			targetOutputStream.close(); // TODO: Handle keep-alives.
			clientsRespOs.close();
			
		}
		catch (IOException e) {
			String errorCode = UUID.randomUUID().toString();
			String msg = String.format("Error while handling I/O to the target server, your personal error code is %s, please contact support and provide this error code.", errorCode);
			String logMsg = String.format("Generated error code %s", errorCode);
			LOG.error(logMsg, e);
			response.sendError(HttpServletResponse.SC_BAD_GATEWAY, msg);
			return;
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

	private static void dumpReturningHeader(Map<String, List<String>> headersFromTargetMap, StringBuffer sb) {
		sb.append("----- Headers from target -----");
		sb.append(LS);
		
		for (Map.Entry<String, List<String>> headers : headersFromTargetMap.entrySet()) {
			sb.append("   ");
			sb.append(headers.getKey());
			sb.append(": ");
			List<String> values = headers.getValue();
			if (values.size() > 1) {
				sb.append("[");
			}
			for (int i = 0; i < values.size(); i++) {
				sb.append(values.get(i));
			}
			if (values.size() > 1) {
				sb.append("]");
			}
			sb.append(LS);
		}
	}

	private void dumpIncomingHeaders(HttpServletRequest request, StringBuffer sb) {
		sb.append("----- Incoming Request -----");
		sb.append(LS);
		sb.append("   ");
		sb.append(request.getMethod());
		sb.append(" ");
		sb.append(request.getRequestURI());
		if (request.getQueryString() != null) {
			sb.append("?");
			sb.append(request.getQueryString());
		}
		sb.append(LS);
		for (Enumeration<String> headers = request.getHeaderNames(); headers.hasMoreElements(); ) {
			String headerName = headers.nextElement();
		
			sb.append("   ");
			sb.append(headerName);
			sb.append(": ");
			sb.append(dumpEnumeration(request.getHeaders(headerName)));
			sb.append(LS);
		}
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				sb.append("   ");
				sb.append("Cookie: ");
				sb.append(cookies[i].getName());
				sb.append("=");
				sb.append(cookies[i].getValue());
				sb.append(LS);
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
