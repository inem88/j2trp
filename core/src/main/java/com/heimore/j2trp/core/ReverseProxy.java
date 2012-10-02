package com.heimore.j2trp.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.MalformedURLException;
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

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class ReverseProxy extends HttpServlet {
	
	private static final String CONNECTION_HDR = "Connection";
	private static final String COOKIE_HDR = "Cookie: ";
	private static final String LOCATION_HDR = "Location";
	private static final String SET_COOKIE2_HDR = "Set-Cookie2";
	private static final String SET_COOKIE_HDR = "Set-Cookie";
	private static final String HDR_SEPARATOR = ": ";
	private static final String HOST_HDR = "Host";
	private static final Logger LOG = Logger.getLogger(ReverseProxy.class);
	private static final String LS = System.getProperty("line.separator");
	
	private String targetHost;
	private int targetPort;
	private String targetBaseUri;
	private String baseUri;
	private boolean useSsl;

	
	private static final long serialVersionUID = 1L;
	private static final Charset ASCII = Charset.forName("US-ASCII");
	private static final byte[] CR_LF = new byte[] { (byte) 0x0d, (byte) 0x0a };
	private static final int[] WELL_KNOWN_PORT = new int[] { 80, 443 }; // Array must be sorted.
	private static final byte[] HEADER_END_MARKER = new byte[] { (byte) 0x0d, (byte) 0x0a, (byte) 0x0d, (byte) 0x0a };
	private static final String XFF_HEADER_NAME = "X-Forwarded-For";
	
    @SuppressWarnings("unchecked")
    protected static void copyHeaders (Map<String, List<String>> outgoingHeaders, Map<String, TouchedHeader> touchedHeaders, OutputStream ps, HttpServletRequest request) throws IOException {
    	
    	boolean foundXFFHeaders = false;
    	boolean foundConnectionHeader = false;
    	final String CONNECTION_VALUE = "close";
    	
    	for (Enumeration<String> headers = request.getHeaderNames(); headers.hasMoreElements(); ) {
    		String headerName = (String) headers.nextElement();
    		if (headerName.equalsIgnoreCase(XFF_HEADER_NAME)) {
    			foundXFFHeaders = true;
    			continue;
    		}
    		if (headerName.equalsIgnoreCase(CONNECTION_HDR)) {
    			foundConnectionHeader = true;
    			continue;
    		}
    		if (!headerName.equalsIgnoreCase(HOST_HDR)) {
        		for (Enumeration<String> headerValues = request.getHeaders(headerName); headerValues.hasMoreElements(); ) {
        			StringBuilder headerBuffer = new StringBuilder();
        			headerBuffer.append(headerName);
        			headerBuffer.append(HDR_SEPARATOR);
        			String headerValue = headerValues.nextElement();
        			headerBuffer.append(headerValue);
        			print(ps, headerBuffer.toString());
        			crlf(ps);
        			
        			// Record outgoing headers
        			addHeader(outgoingHeaders, headerName, headerValue);
        		}
    		}
    		
    		
    		
    	}
    	
    	StringBuilder xffHeader = new StringBuilder();
    	print(ps, XFF_HEADER_NAME);
    	print(ps, HDR_SEPARATOR);
    	
    	if (foundXFFHeaders) {
    		for (Enumeration<String> headerValues = request.getHeaders(XFF_HEADER_NAME); headerValues.hasMoreElements(); ) {
    			xffHeader.append(headerValues.nextElement());
    			xffHeader.append(", ");
    		}
    		touchedHeaders.put(XFF_HEADER_NAME, TouchedHeader.CHANGED);
    	}
    	else {
    		touchedHeaders.put(XFF_HEADER_NAME, TouchedHeader.ADDED);
    	}
    	xffHeader.append(request.getRemoteAddr());
    	print(ps, xffHeader.toString());
    	crlf(ps);
 
    	// Add Connection header.
    	print(ps, CONNECTION_HDR);
		print(ps, HDR_SEPARATOR);
		print(ps, CONNECTION_VALUE);
		crlf(ps);
		
    	addHeader(outgoingHeaders, XFF_HEADER_NAME, xffHeader.toString());
    	addHeader(outgoingHeaders, CONNECTION_HDR, CONNECTION_VALUE);
    	
    	if (foundConnectionHeader) {
    		touchedHeaders.put(CONNECTION_HDR, TouchedHeader.CHANGED);
    	}
    	else {
    		touchedHeaders.put(CONNECTION_HDR, TouchedHeader.ADDED);
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
    		if (header.getKey().equalsIgnoreCase(SET_COOKIE_HDR)) {
    			copyCookiesFromResponse(header.getValue(), resp, targetBaseUri, baseUri);
    			continue;
    		}
    		if (header.getKey().equalsIgnoreCase(SET_COOKIE2_HDR)) {
    			copyCookiesFromResponse(header.getValue(), resp, targetBaseUri, baseUri);
    			continue;
    		}
    		if (header.getKey().equalsIgnoreCase(LOCATION_HDR)) {
    			System.out.println("Suppressing copying of Location header.");
    			continue;
    		}
    		for (String value : header.getValue()) {
    			resp.addHeader(header.getKey(), value);
    		}
    		
    	}
    }

    protected static void copyCookies (Map<String, List<String>> outgoingHeaders, OutputStream ps, HttpServletRequest request) throws IOException {
    	Cookie[] cookies = request.getCookies();
    	
    	if (cookies != null) {
    		print(ps, COOKIE_HDR);
    		StringBuilder cookieBuffer = new StringBuilder();
        	for (int i = 0; i < cookies.length; i++) {
        		cookieBuffer.append(cookies[i].getName());
        		cookieBuffer.append("=");
        		cookieBuffer.append(cookies[i].getValue());
        		if (i < cookies.length - 1) {
        			cookieBuffer.append("; ");
        		}
        		else {
        			print(ps, cookieBuffer.toString());
        			crlf(ps);
        		}
        	}
        	addHeader(outgoingHeaders, COOKIE_HDR, cookieBuffer.toString());
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
			int indexOfHeaderSeparator = header.indexOf(HDR_SEPARATOR);
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
	
    static String dumpEnumeration (Enumeration<String> bag) {
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
	
	protected String buildRedirectUrl (HttpServletRequest req, String translatedPath) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(req.getScheme());
		sb.append("://");
		
		sb.append(req.getServerName());
		
		if (!isWellKnownPort(req.getServerPort())) {
			sb.append(":");
			sb.append(req.getServerPort());
		}
		sb.append(translatedPath);
			
		return sb.toString();
	}
	
	private Socket createSocket() throws UnknownHostException, IOException {
		
		Socket result;
		if (useSsl) {
			result = SSLSocketFactory.getDefault().createSocket(targetHost, targetPort);
		}
		else {
			result = new Socket(targetHost, targetPort);
		}
		
		return result;
	}
	
	public void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Socket socket = null;
		byte[] byteBuffer = new byte[1024];
		long start = System.currentTimeMillis();
		try {
			// Connect to target.
			try {
			   socket = createSocket();
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
			long connectStamp = System.currentTimeMillis();
			Map<String, TouchedHeader> touchedHeaders = new HashMap<String, TouchedHeader>();
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
			httpVerb.append(" HTTP/1.1");
			print(headerBuffer, httpVerb.toString());
			crlf(headerBuffer);
			// Ok, verb and request URI complete, proceed with the headers.
			Map<String, List<String>> outgoingHeaders = new HashMap<String, List<String>>();
			copyHeaders(outgoingHeaders, touchedHeaders, headerBuffer, request);
			copyCookies(outgoingHeaders, headerBuffer, request);
			// Add proxied Host header.
			StringBuilder hostHeader = new StringBuilder();
			print(headerBuffer, "Host: "); // Add port?
			hostHeader.append(targetHost);
			// Special handling of known HTTP ports (basically ports other than 80 and 443)
			if (!isWellKnownPort(targetPort)) {
				hostHeader.append(":");
				hostHeader.append(targetPort);
			}
			print(headerBuffer, hostHeader.toString());
			// Ok, end header with two CRLFs
			crlf(headerBuffer);
			crlf(headerBuffer);
			addHeader(outgoingHeaders, HOST_HDR, hostHeader.toString());
			touchedHeaders.put(HOST_HDR, TouchedHeader.CHANGED);
			long headerAssemblyStamp = System.currentTimeMillis();
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
			long socketWrite = System.currentTimeMillis();
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
							httpStatus = parseHeaders(allHeaders, headersFromTargetMap);
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
							URL redirectedUrl = new URL(headersFromTargetMap.get(LOCATION_HDR).get(0));
							String targetPath = redirectedUrl.getFile();
							String translatedPath = targetPath;
							if (targetPath.startsWith(targetBaseUri)) {
								StringBuilder sb = new StringBuilder(targetPath);
								sb.delete(0, targetBaseUri.length());
								sb.insert(0, baseUri);
								translatedPath = sb.toString();
							}
							redirectUrl = response.encodeRedirectURL(buildRedirectUrl(request, translatedPath));
							response.sendRedirect(redirectUrl);
						}
						headerAlreadyWritten = true;
					}
					clientsRespOs.write(byteBuffer, bodyMarker, bytesRead - bodyMarker);
					bodyMarker = 0;
				}
				bytesRead = proxiedInputSteam.read(byteBuffer);
			}
			long end = System.currentTimeMillis();
			LOG.info(String.format("Proxied request %s \"%s\" -> \"%s\" (%d)", request.getMethod(), request.getRequestURI(), requestUri, httpStatus.getCode()));
			
			if (LOG.isTraceEnabled()) {
				 
				StringBuilder sb = new StringBuilder(1024);
				sb.append(LS);
				sb.append(String.format("Time: connect: %d, header assembly: %d, socket write: %d, process response: %d, total: %d",
						connectStamp - start, headerAssemblyStamp - connectStamp, socketWrite - headerAssemblyStamp, 
						end - socketWrite, end - start));
				sb.append(LS);
				dumpIncomingHeaders(request, sb);
				dumpOutgoingHeaders(httpVerb.toString(), outgoingHeaders, touchedHeaders, sb);
				dumpReturningHeadersFromTarget(httpStatus, headersFromTargetMap, sb);
				dumpReturningHeadersToClient(redirectUrl, sb);
				LOG.trace(sb);
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

	private static void dumpReturningHeadersFromTarget(HttpStatus httpStatus, Map<String, List<String>> headersFromTargetMap, StringBuilder sb) {
		sb.append("----- <= J2TRP <= -----");
		sb.append(LS);
		sb.append("   ");
		sb.append("Return code: ");
		sb.append(httpStatus.getCode());
		sb.append(" ");
		sb.append(httpStatus.getStatus());
		sb.append(LS);
		for (Map.Entry<String, List<String>> headers : headersFromTargetMap.entrySet()) {
			sb.append("   ");
			sb.append(headers.getKey());
			sb.append(HDR_SEPARATOR);
			List<String> values = headers.getValue();
			if (values.size() > 1) {
				sb.append("[");
			}
			for (int i = 0; i < values.size(); i++) {
				sb.append(values.get(i));
				if (i < values.size() - 1) {
					sb.append(" ");
				}
			}
			if (values.size() > 1) {
				sb.append("]");
			}
			sb.append(LS);
		}
	}
	
	private static void dumpReturningHeadersToClient(String redirectUrl, StringBuilder sb) {
		sb.append("----- <= J2TRP -----");
		sb.append(LS);
		if (redirectUrl != null && !redirectUrl.isEmpty()) {
			sb.append("   ");
			sb.append("Location: ");
			sb.append(redirectUrl);
		}
		else {
			sb.append("   None.");
		}
		sb.append(LS);
	}

	private static void dumpOutgoingHeaders(String httpVerb, Map<String, List<String>> outgoingHeaders, Map<String, TouchedHeader> touchedHeaders, StringBuilder sb) {
		sb.append("---- J2TRP => -----");
		sb.append(LS);
		sb.append("   ");
		sb.append(httpVerb);
		sb.append(LS);
		for (Map.Entry<String, List<String>> headers : outgoingHeaders.entrySet()) {
			sb.append(" ");
			TouchedHeader touchedHeader = touchedHeaders.get(headers.getKey());
			if (touchedHeader != null) {
				sb.append(touchedHeader);
				sb.append(" ");
			}
			else {
				sb.append("  ");
			}
			sb.append(headers.getKey());
			sb.append(HDR_SEPARATOR);
			List<String> values = headers.getValue();
			
			if (values.size() > 1) {
				sb.append(values);
			}
			else {
				sb.append(values.get(0));
			}
			sb.append(LS);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void dumpIncomingHeaders(HttpServletRequest request, StringBuilder sb) {
		sb.append("----- => J2TRP -----");
		sb.append(LS);
		sb.append("   ");
		sb.append(request.getMethod());
		sb.append(" ");
		sb.append(request.getRequestURI());
		if (request.getQueryString() != null) {
			sb.append("?");
			sb.append(request.getQueryString());
		}
		sb.append(" ");
		sb.append(request.getProtocol());
		sb.append(LS);
		for (Enumeration<String> headers = request.getHeaderNames(); headers.hasMoreElements(); ) {
			String headerName = headers.nextElement();
		
			sb.append("   ");
			sb.append(headerName);
			sb.append(HDR_SEPARATOR);
			sb.append(dumpEnumeration(request.getHeaders(headerName)));
			sb.append(LS);
		}
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				sb.append("   ");
				sb.append(COOKIE_HDR);
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
		
		URL targetUrl = null;
		try {
			LOG.info("Servlet param TARGET_URL is: " + config.getInitParameter("TARGET_URL"));
			targetUrl = new URL(config.getInitParameter("TARGET_URL"));
		}
		catch (MalformedURLException e) {
			throw new ServletException(e);
		}
		
		targetHost = targetUrl.getHost();
		targetPort = targetUrl.getPort();
		useSsl = targetUrl.getProtocol().startsWith("https");
		
		if (targetPort == -1) {
			targetPort = targetUrl.getDefaultPort();
		}
		
		targetBaseUri = targetUrl.getPath();
		baseUri = config.getServletContext().getContextPath();
	}

	
	
}
