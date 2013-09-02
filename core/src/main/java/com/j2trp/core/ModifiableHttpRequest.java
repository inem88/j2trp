package com.j2trp.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;


public class ModifiableHttpRequest extends HttpServletRequestWrapper {

	private String authType;
	private Cookie[] cookies;
	private Map<String, List<String>> headers = new HashMap<String, List<String>>();
	private String method;
	private String pathInfo;
	private String pathTranslated;
	private String contextPath;
	private String queryString;
	private String remoteUser;
	private Set<String> userInRole = new HashSet<String>();
	private Principal principal;
	private String protocol;
	private String scheme;
	private Integer port;
	private String serverName;
	private String characterEncoding;
	private int contentLength;
	private ServletInputStream is;
	private String localAddr;
	private Locale locale;
	private String localName;
	private int localPort;
	private Map<String, String[]> parameterMap;
	private BufferedReader reader;
	private String realPath;
	private String remoteAddr;
	private String remoteHost;
	private int remotePort;
	private ServletRequest servletRequest;
	private RequestDispatcher requestDispatcher;
	private String requestedSessionId;
	private String requestURI;
	private StringBuffer requestURL;
	private int serverPort;
	private String servletPath;
	private HttpSession session;
	private boolean secure;
	private boolean requestedSessionIdValue;
	private boolean requestedSessionIdFromCookie;
	private boolean requestedSessionIdFromCookieUrl;
	private boolean requestedSessionIdFromCookieURL;
	private Map<String, Object> attributes = new HashMap<String, Object>();
	private String contentType;
	
	public ModifiableHttpRequest(HttpServletRequest request) {
		super(request);
	}

	@Override
	public String getAuthType() {
		return Toolbox.getValue(super.getAuthType(), authType, String.class);
	}

	@Override
	public Cookie[] getCookies() {
		return Toolbox.merge(super.getCookies(), cookies, Cookie[].class);
	}

	@Override
	public long getDateHeader(String name) {
		return Toolbox.getValue(super.getDateHeader(name), Long.parseLong(headers.get(name).get(0)), -1L, Long.class);
	}

	@Override
	public String getHeader(String name) {
		return Toolbox.getValue(super.getHeader(name), headers.get(name), String.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<String> getHeaders(String name) {
		return Toolbox.mergeCollection(super.getHeaders(name), Toolbox.safeEnumerator(headers.get(name)));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<String> getHeaderNames() {
		return Toolbox.mergeCollection(super.getHeaderNames(), Toolbox.safeEnumerator(headers.keySet()));
	}

	@Override
	public int getIntHeader(String name) {
		return Toolbox.getValue(super.getIntHeader(name), Integer.parseInt(headers.get(name).get(0)), -1, Integer.class);
	}

	@Override
	public String getMethod() {
		return Toolbox.getValue(super.getMethod(), method, String.class);
	}

	@Override
	public String getPathInfo() {
		return Toolbox.getValue(super.getPathInfo(), pathInfo, String.class);
	}

	@Override
	public String getPathTranslated() {
		return Toolbox.getValue(super.getPathTranslated(), pathTranslated, String.class);
	}

	@Override
	public String getContextPath() {
		return Toolbox.getValue(super.getContextPath(), contextPath, String.class);
	}

	@Override
	public String getQueryString() {
		return Toolbox.getValue(super.getQueryString(), queryString, String.class);
	}

	@Override
	public String getRemoteUser() {
		return Toolbox.getValue(super.getRemoteUser(), remoteUser, String.class);
	}

	@Override
	public boolean isUserInRole(String role) {
		return Toolbox.getValue(super.isUserInRole(role), userInRole.contains(role), Boolean.class);
	}

	@Override
	public Principal getUserPrincipal() {
		return Toolbox.getValue(super.getUserPrincipal(), principal, Principal.class);
	}

	@Override
	public String getRequestedSessionId() {
		return Toolbox.getValue(super.getRequestedSessionId(), requestedSessionId, String.class);
	}

	@Override
	public String getRequestURI() {
		return Toolbox.getValue(super.getRequestURI(), requestURI, String.class);
	}

	@Override
	public StringBuffer getRequestURL() {
		return Toolbox.getValue(super.getRequestURL(), requestURL, StringBuffer.class);
	}

	@Override
	public String getServletPath() {
		return Toolbox.getValue(super.getServletPath(), servletPath, String.class);
	}

	@Override
	public HttpSession getSession(boolean create) {
		if (session != null) {
			return session;
		}
		return super.getSession(create);
	}

	@Override
	public HttpSession getSession() {
		return Toolbox.getValue(super.getSession(), session, HttpSession.class);
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return Toolbox.getValue(super.isRequestedSessionIdValid(), requestedSessionIdValue, Boolean.class); 
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return Toolbox.getValue(super.isRequestedSessionIdFromCookie(), requestedSessionIdFromCookie, Boolean.class);
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return Toolbox.getValue(super.isRequestedSessionIdFromURL(), requestedSessionIdFromCookieURL, Boolean.class);
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return Toolbox.getValue(super.isRequestedSessionIdFromUrl(), requestedSessionIdFromCookieUrl, Boolean.class);
	}

	@Override
	public ServletRequest getRequest() {
		return Toolbox.getValue(super.getRequest(), servletRequest, ServletRequest.class);
	}

	@Override
	public void setRequest(ServletRequest request) {
		servletRequest = request;
	}

	@Override
	public Object getAttribute(String name) {
		return Toolbox.getValue(super.getAttribute(name), attributes.get(name), Object.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<String> getAttributeNames() {
		return Toolbox.mergeCollection(super.getAttributeNames(), Collections.enumeration(attributes.keySet()));
	}

	@Override
	public String getCharacterEncoding() {
		return Toolbox.getValue(super.getCharacterEncoding(), characterEncoding, String.class);
	}

	@Override
	public void setCharacterEncoding(String enc)
			throws UnsupportedEncodingException {
		characterEncoding = enc;
	}

	@Override
	public int getContentLength() {
		return Toolbox.getValue(super.getContentLength(), contentLength, -1, Integer.class);
	}

	@Override
	public String getContentType() {
		return Toolbox.getValue(super.getContentType(), contentType, String.class);
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return super.getInputStream();
	}

	@Override
	public String getParameter(String name) {
		return Toolbox.getValue(super.getParameter(name), parameterMap.get(name), String.class);
	}

	@Override
	public Map getParameterMap() {
		return super.getParameterMap();
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return Toolbox.mergeCollection(super.getParameterNames(), Collections.enumeration(parameterMap.keySet()));
	}

	@Override
	public String[] getParameterValues(String name) {
		return Toolbox.merge(super.getParameterValues(name), parameterMap.get(name), String[].class);
	}

	@Override
	public String getProtocol() {
		return Toolbox.getValue(super.getProtocol(), protocol, String.class);
	}

	@Override
	public String getScheme() {
		return Toolbox.getValue(super.getScheme(), scheme, String.class);
	}

	@Override
	public String getServerName() {
		return Toolbox.getValue(super.getServerName(), serverName, String.class);
	}

	@Override
	public int getServerPort() {
		return Toolbox.getValue(super.getServerPort(), serverPort, Integer.class);
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return super.getReader();
	}

	@Override
	public String getRemoteAddr() {
		return Toolbox.getValue(super.getRemoteAddr(), remoteAddr, String.class);
	}

	@Override
	public String getRemoteHost() {
		return Toolbox.getValue(super.getRemoteHost(), remoteHost, String.class);
	}

	@Override
	public void setAttribute(String name, Object o) {
		 attributes.put(name, o);
	}

	@Override
	public void removeAttribute(String name) {
		super.removeAttribute(name);
	}

	@Override
	public Locale getLocale() {
		return Toolbox.getValue(super.getLocale(), locale, Locale.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<Locale> getLocales() {
		return Toolbox.mergeCollection(super.getLocales(), new SingleValueEnumeration<Locale>(locale));
	}

	@Override
	public boolean isSecure() {
		return Toolbox.getValue(super.isSecure(), secure, Boolean.class);
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return Toolbox.getValue(super.getRequestDispatcher(path), requestDispatcher, RequestDispatcher.class);
	}

	@Override
	public String getRealPath(String path) {
		return Toolbox.getValue(super.getRealPath(path), realPath, String.class);
	}

	@Override
	public int getRemotePort() {
		return Toolbox.getValue(super.getRemotePort(), remotePort, Integer.class);
	}

	@Override
	public String getLocalName() {
		return Toolbox.getValue(super.getLocalName(), localName, String.class);
	}

	@Override
	public String getLocalAddr() {
		return Toolbox.getValue(super.getLocalAddr(), localAddr, String.class);
	}

	@Override
	public int getLocalPort() {
		return Toolbox.getValue(super.getLocalPort(), localPort, Integer.class);
	}

	public void setAuthType(String authType) {
		this.authType = authType;
	}

	public void setCookies(Cookie[] cookies) {
	  if (cookies == null) {
	    this.cookies = null;
	    return;
	  }
	  this.cookies = new Cookie[cookies.length];
	  System.arraycopy(cookies, 0, this.cookies, 0, this.cookies.length);
	}

	public void addHeader(String header, String value) {
		List<String> values = headers.get(header);
		
		if (values == null) {
			values = new ArrayList<String>();
			headers.put(header, values);
		}
		values.add(value);
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	public void setPathTranslated(String pathTranslated) {
		this.pathTranslated = pathTranslated;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public void setRemoteUser(String remoteUser) {
		this.remoteUser = remoteUser;
	}

	public void setPrincipal(Principal principal) {
		this.principal = principal;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	public void setIs(ServletInputStream is) {
		this.is = is;
	}

	public void setLocalAddr(String localAddr) {
		this.localAddr = localAddr;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setLocalName(String localName) {
		this.localName = localName;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	public void setParameterMap(Map<String, String[]> parameterMap) {
		this.parameterMap = parameterMap;
	}

	public void setReader(BufferedReader reader) {
		this.reader = reader;
	}

	public void setRealPath(String realPath) {
		this.realPath = realPath;
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	public void setServletRequest(ServletRequest servletRequest) {
		this.servletRequest = servletRequest;
	}

	public void setRequestDispatcher(RequestDispatcher requestDispatcher) {
		this.requestDispatcher = requestDispatcher;
	}

	public void setRequestedSessionId(String requestedSessionId) {
		this.requestedSessionId = requestedSessionId;
	}

	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}

	public void setRequestURL(StringBuffer requestURL) {
		this.requestURL = requestURL;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	public void setSession(HttpSession session) {
		this.session = session;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public void setRequestedSessionIdValue(boolean requestedSessionIdValue) {
		this.requestedSessionIdValue = requestedSessionIdValue;
	}

	public void setRequestedSessionIdFromCookie(boolean requestedSessionIdFromCookie) {
		this.requestedSessionIdFromCookie = requestedSessionIdFromCookie;
	}

	public void setRequestedSessionIdFromCookieUrl(
			boolean requestedSessionIdFromCookieUrl) {
		this.requestedSessionIdFromCookieUrl = requestedSessionIdFromCookieUrl;
	}

	public void setRequestedSessionIdFromCookieURL(
			boolean requestedSessionIdFromCookieURL) {
		this.requestedSessionIdFromCookieURL = requestedSessionIdFromCookieURL;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	
	
}
