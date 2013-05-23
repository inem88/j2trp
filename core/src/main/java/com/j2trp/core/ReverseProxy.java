package com.j2trp.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
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

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.j2trp.core.config.Setting;
import com.j2trp.core.config.Settings;

public class ReverseProxy extends HttpServlet {
	
	private static final int BUFFER_SIZE = 1024;
	private static final String CONNECTION_HDR = "Connection";
	private static final String COOKIE_HDR = "Cookie: ";
	private static final String LOCATION_HDR = "Location";
	private static final String SET_COOKIE2_HDR = "Set-Cookie2";
	private static final String SET_COOKIE_HDR = "Set-Cookie";
	private static final String HDR_SEPARATOR = ": ";
	private static final String HOST_HDR = "Host";
	private static final Logger LOG = Logger.getLogger(ReverseProxy.class);
	private static final String LS = System.getProperty("line.separator");
	private static final long serialVersionUID = 1L;
	private static final Charset ASCII = Charset.forName("US-ASCII");
	private static final byte[] CR_LF = new byte[] { (byte) 0x0d, (byte) 0x0a };
	private static final int[] WELL_KNOWN_PORT = new int[] { 80, 443 }; // Array must be sorted.
	private static final byte[] HEADER_END_MARKER = new byte[] { (byte) 0x0d, (byte) 0x0a, (byte) 0x0d, (byte) 0x0a };
	private static final String XFF_HEADER_NAME = "X-Forwarded-For";

	private transient Settings settings;
	
	/**
	 * The address of the upstream server.
	 */
	private String targetHost;

	/**
	 * The port of the upstream server.
	 */
	private int targetPort;

	/**
	 * The base URI of the upstream server.
	 */
	private String targetBaseUri;

	/**
	 * The base URI of this proxy.
	 */
	private String baseUri;

	/**
	 * Whether or not to use SSL for the upstream server or not.
	 */
	private boolean useSsl;

	/**
	 * How long to wait for the connection to the upstream server to become established. 
	 */
	private int socketTimeoutMs;
	
	/**
	 * This method copies headers from the incoming request to the request going to the upstream server.
	 * It also adds or appends the XFF header to the outgoing request as per specification. 
	 * @param outgoingHeaders the Map where the outgoing headers should be copied to.
	 * @param touchedHeaders a Map that keeps track of which headers have been touched (added/changed).
	 * @param ps The output stream of the upstream server.
	 * @param request The incoming request to the proxy.
	 * @throws IOException If there's an error while writing on the output stream.
	 */
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
    	
    	// If there's already a XFF header present, append this proxy's address to it. Otherwise, just add u 
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
    
    /**
     * Converts the incoming cookies to the outgoing the request. Any cookie path is also rewritten.
     * @param cookies The cookies to convert.
     * @param targetBaseUri The base URI for the upstream server.
     * @param baseUri The base URI for this proxy.
     * @return A list of converted cookies, ready to be sent to the upstream server.
     */
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
    
    /**
     * Adds the cookies coming back from the upstream server to the response object that is going back to the client.
     * @param rawCookieValues a List of raw cookie headers from the returning HTTP response.
     * @param resp The response object that goes back to the client.
     * @param targetBaseUri The base URI of the upstream server.
     * @param baseUri The base URI of this proxy.
     */
    protected static void copyCookiesFromResponse (List<String> rawCookieValues, HttpServletResponse resp, String targetBaseUri, String baseUri) {
    	List<HttpCookie> allCookies = new ArrayList<HttpCookie>();
    	for (String rawCookie : rawCookieValues) {
    		allCookies.addAll(HttpCookie.parse(rawCookie));
    	}
    	
    	for (Cookie cookie : convertCookies(allCookies, targetBaseUri, baseUri)) {
    		resp.addCookie(cookie);
    	}
    	
    }
    
    /**
     * Copies the headers coming back in the response from the upstream server. 
     * <p>
     * Note: the cookies are handled by {@linkplain #copyCookiesFromResponse(List, HttpServletResponse, String, String)}
     * </p>
     * <p>
     * The Location header is <b>not</b> copied since it requires special handling as it needs to be rewritten. 
     * </p>
     * @param headers The headers coming from the response from the upstream server.
     * @param resp The response object going back to the client.
     * @param targetBaseUri The base URI of the upstream server.
     * @param baseUri The base URI of this proxy.
     * @throws IOException If there's an error reading the response object.
     */
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

    /**
     * Utility method that copies the cookies from the incoming request and is to be relayed to the upstream server. 
     * @param outgoingHeaders The resulting headers that are copied by this method.  
     * @param ps The output stream of the upstream server.
     * @param request The incoming request object.
     * @throws IOException If there's an error while writing on the upstream server's socket.
     */
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
    
    /**
     * Is the supplied port a known one (80 or 443) or not.
     * @param port The port.
     * @return true of the port is "well-known", false otherwise.
     */
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

	/**
	 * Prints the data as ASCII to the supplied output stream.
	 * @param os The output stream
	 * @param data The data.
	 * @throws IOException If there's an error while writing on the output stream.
	 */
	static void print (OutputStream os, String data) throws IOException {
		os.write(data.getBytes(ASCII));
	}
	
	/**
	 * Prints a CRLF to the supplied output stream.
	 * @param os The output stream.
	 * @throws IOException If there's an error while writing on the output stream.
	 */
	static void crlf (OutputStream os) throws IOException {
		os.write(CR_LF);
	}
	
	/**
	 * Utility method for adding a header to the supplied Map.
	 * @param map The header map.
	 * @param key The header name.
	 * @param value The header value.
	 */
	static void addHeader (Map<String, List<String>> map, String key, String value) {
		List<String> headerValue = map.get(key);
		if (headerValue == null) {
			headerValue = new ArrayList<String>();
			map.put(key, headerValue);
		}
		headerValue.add(value);
	}
	
	/**
	 * Parses the response headers including the HTTP response code sent back from the upstream server.
	 * The first line (excluding the CRLF marker), contains the response code and the status message.
	 * @param headersIncCrlf The string including all the headers, including the CRLF marker. 
	 * @param parsedHeaders the Map that will contain the parsed headers (excluding the status code).
	 * @return The HTTP status
	 */
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
	
	/**
	 * Utility method for rewriting the proxied URI.
	 * @param srcUri The source URI which to base the rewrite on.
	 * @return The proxied URI.
	 */
	String getProxiedUri (String srcUri) {
		if (srcUri.startsWith(baseUri)) {
			StringBuilder sb = new StringBuilder(srcUri);
			sb.delete(0, baseUri.length());
			sb.insert(0, targetBaseUri);
			return sb.toString();
		}
		return srcUri;
	}
	
    /**
     * Utility method that pretty-prints an Enumeration.
     * @param bag The collection to pretty-print.
     * @return The values of the Enumeration as one string. 
     * An empty string ("") is returned if the Enumeration is empty.
     */
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
	
	/**
	 * Constructs a redirection URL from the Location header sent back from the upstream server. 
	 * @param req The request from the client.
	 * @param translatedPath The translated URI that should be sent back to the client.
	 * @return The complete URL that should go into the Location header back to the client.
	 */
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
	
	/**
	 * Utility method that connects a socket (plain-text or SSL) to the upstream server.
	 * @return A socket connected to the upstream server.
	 * @throws UnknownHostException If the upstream server address cannot be resolved or connected to. 
	 * @throws IOException If there's a low-level I/O error while creating the socket.
	 */
	protected Socket createSocket() throws UnknownHostException, IOException, SocketTimeoutException {
		
		Socket result;
		if (useSsl) {
			SSLSocket sslSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
			sslSocket.connect(new InetSocketAddress(targetHost, targetPort), socketTimeoutMs); 
			LOG.debug(String.format("Connected to %s:%d using SSL, cipher suite in session: %s", targetHost, targetPort, sslSocket.getSession().getCipherSuite()));
			result = sslSocket;
		}
		else {
			result = new Socket();
			result.connect(new InetSocketAddress(targetHost, targetPort), socketTimeoutMs); 
			LOG.debug(String.format("Connected to %s:%d using a regular socket.", targetHost, targetPort));
		}
		
		return result;
	}
	
	/**
	 * Main method of this proxy that does all the work.
	 * @param request The request object coming in from the client.
	 * @param response THe response object going back to the client.
	 * @throws ServletException If there's an error with the request and response object.
	 * @throws IOException If there's a low-level I/O error while interacting with the upstream server.
	 */
	public void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Socket socket = null;
		byte[] byteBuffer = new byte[BUFFER_SIZE];
		long start = System.currentTimeMillis();
		try {
			// Connect to target.
			try {
			   socket = createSocket();
			}
			catch (UnknownHostException e) {
				generateError(response, "Target server not reachable", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
				return;
			}
			catch (SocketTimeoutException e) {
			  generateError(response, "I/O timeout when connecting the target", HttpServletResponse.SC_GATEWAY_TIMEOUT, e);
			  return;
			}
			catch (IOException e) {
			  generateError(response, "I/O error when connecting the target", HttpServletResponse.SC_BAD_GATEWAY, e);
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
						if (httpStatus.getCode() == HttpServletResponse.SC_FOUND || 
						    httpStatus.getCode() == HttpServletResponse.SC_MOVED_PERMANENTLY || 
						    httpStatus.getCode() == HttpServletResponse.SC_SEE_OTHER ||
						    httpStatus.getCode() == HttpServletResponse.SC_TEMPORARY_REDIRECT) {
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
			if (httpStatus == null) {
				response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
			}
			LOG.info(String.format("Proxied request %s \"%s\" -> \"%s\" (%d)", request.getMethod(), request.getRequestURI(), requestUri, (httpStatus != null ? httpStatus.getCode() : 0)));
			
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
				dumpReturningHeadersToClient(httpStatus == null, redirectUrl, sb);
				LOG.trace(sb);
			}
					
			targetOutputStream.close();
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

  /**
   * @param resp
   * @param e
   * @throws IOException
   */
  private static void generateError(HttpServletResponse resp, String txt, int httpStatusCode, Exception e)
      throws IOException {
    String errorCode = UUID.randomUUID().toString();
    String msg = String.format(txt + ", your personal error code is %s, please contact support and provide this error code.", errorCode);
    String logMsg = String.format("Generated error code %s", errorCode);
    LOG.error(logMsg, e);
    resp.sendError(httpStatusCode, msg);
  }

	/**
	 * Internal method that dumps the returning headers from the upstream server that will ultimately go into the log.
	 * @param httpStatus The status from the upstream server. 
	 * @param headersFromTargetMap The headers from the upstream servers.
	 * @param sb The buffer that this method dumps the data into.
	 */
	private static void dumpReturningHeadersFromTarget(HttpStatus httpStatus, Map<String, List<String>> headersFromTargetMap, StringBuilder sb) {
		sb.append("----- <= J2TRP <= -----");
		sb.append(LS);
		sb.append("   ");
		sb.append("Return code: ");
		if (httpStatus != null) {
			sb.append(httpStatus.getCode());
			sb.append(" ");
			sb.append(httpStatus.getStatus());
		}
		else {
			sb.append("N/A");
		}
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
	
	/**
	 * Internal method that dumps the returning headers to the client that will ultimately go into the log.
	 * @param missingStatusCode If the status code from the upstream server is missing, for instance, due to the
	 * communication being aborted.
	 * @param redirectUrl The redirect URL, if any.
	 * @param sb The buffer that this method dumps the data into.
	 */
	private static void dumpReturningHeadersToClient(boolean missingStatusCode, String redirectUrl, StringBuilder sb) {
		sb.append("----- <= J2TRP -----");
		sb.append(LS);
		if (redirectUrl != null && !redirectUrl.isEmpty()) {
			sb.append("   ");
			sb.append("Location: ");
			sb.append(redirectUrl);
		}
		else if (missingStatusCode) {
			sb.append("   502 (Bad Gateway)");
		}
		else {
			sb.append("   None.");
		}
		sb.append(LS);
	}

	/**
	 * Internal method that dumps the headers going to the upstream server that will ultimately go into the log.
	 * @param httpVerb The HTTP verb, requested URI and protocol version.
	 * @param outgoingHeaders The headers as-is.
	 * @param touchedHeaders  The headers that have been added or changed.
	 * @param sb The buffer that this method dumps the data into.
	 */
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
	
	/**
	 * Internal method that dumps the headers coming from the client that will ultimately go into the log.
	 * @param request The request object from the client.
	 * @param sb The buffer that this method dumps the data into.
	 */
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
	
	/**
	 * Servlet initialization method for this proxy.
	 * @param config The configuration object coming from the Servlet container.
	 * @throws ServletException If there's an error while extracting the configuration.
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		File configFile = new File(config.getInitParameter("configFile"));
		
		if (!configFile.isAbsolute()) {
		  LOG.info("configFile is determined to be " + configFile);
		}
		if (!configFile.exists()) {
		  throw new ServletException("configFile doesn't appear to exist.");
		}
		
		try {
		  settings = new Settings(configFile);
		}
		catch (IOException e) {
		  throw new ServletException(e);
		}
		
		URL targetUrl = null;
		try {
			targetUrl = new URL(settings.getProperty(Setting.TARGET_URL));
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
		
		socketTimeoutMs = settings.getPropertyAsInt(Setting.TARGET_SOCKET_TIMEOUT_MS);
	}

	
	
}
