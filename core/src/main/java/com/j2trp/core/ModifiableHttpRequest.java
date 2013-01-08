package com.j2trp.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
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

import com.heimore.exa.sso.UserPrincipalKeys;

public class ModifiableHttpRequest extends HttpServletRequestWrapper {

	String authType;
	Cookie[] cookies;
	Map<String, List<String>> headers = new HashMap<String, List<String>>();
	String method;
	String pathInfo;
	String pathTranslated;
	String contextPath;
	String queryString;
	String remoteUser;
	Set<String> userInRole = new HashSet<String>();
	Principal principal;
	String protocol;
	String scheme;
	Integer port;
	String serverName;
	String characterEncoding;
	int contentLength;
	ServletInputStream is;
	String localAddr;
	Locale locale;
	String localName;
	int localPort;
	Map<String, String> parameterMap;
	BufferedReader reader;
	String realPath;
	String remoteAddr;
	String remoteHost;
	int remotePort;
	ServletRequest servletRequest;
	RequestDispatcher requestDispatcher;
	String requestedSessionId;
	String requestURI;
	StringBuffer requestURL;
	int serverPort;
	String servletPath;
	HttpSession session;
	boolean secure;
	boolean requestedSessionIdValue;
	boolean requestedSessionIdFromCookie;
	boolean requestedSessionIdFromCookieUrl;
	boolean requestedSessionIdFromCookieURL;
	
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
		return Toolbox.mergeCollection(super.getHeaders(name), Collections.enumeration(headers.get(name)));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<String> getHeaderNames() {
		return Toolbox.mergeCollection(super.getHeaderNames(), Collections.enumeration(headers.keySet()));
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
		// TODO Auto-generated method stub
		return super.getAttribute(name);
	}

	@Override
	public Enumeration getAttributeNames() {
		// TODO Auto-generated method stub
		return super.getAttributeNames();
	}

	@Override
	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return super.getCharacterEncoding();
	}

	@Override
	public void setCharacterEncoding(String enc)
			throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		super.setCharacterEncoding(enc);
	}

	@Override
	public int getContentLength() {
		// TODO Auto-generated method stub
		return super.getContentLength();
	}

	@Override
	public String getContentType() {
		// TODO Auto-generated method stub
		return super.getContentType();
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return super.getInputStream();
	}

	@Override
	public String getParameter(String name) {
		// TODO Auto-generated method stub
		return super.getParameter(name);
	}

	@Override
	public Map getParameterMap() {
		// TODO Auto-generated method stub
		return super.getParameterMap();
	}

	@Override
	public Enumeration getParameterNames() {
		// TODO Auto-generated method stub
		return super.getParameterNames();
	}

	@Override
	public String[] getParameterValues(String name) {
		// TODO Auto-generated method stub
		return super.getParameterValues(name);
	}

	@Override
	public String getProtocol() {
		// TODO Auto-generated method stub
		return super.getProtocol();
	}

	@Override
	public String getScheme() {
		// TODO Auto-generated method stub
		return super.getScheme();
	}

	@Override
	public String getServerName() {
		// TODO Auto-generated method stub
		return super.getServerName();
	}

	@Override
	public int getServerPort() {
		// TODO Auto-generated method stub
		return super.getServerPort();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		// TODO Auto-generated method stub
		return super.getReader();
	}

	@Override
	public String getRemoteAddr() {
		// TODO Auto-generated method stub
		return super.getRemoteAddr();
	}

	@Override
	public String getRemoteHost() {
		// TODO Auto-generated method stub
		return super.getRemoteHost();
	}

	@Override
	public void setAttribute(String name, Object o) {
		// TODO Auto-generated method stub
		super.setAttribute(name, o);
	}

	@Override
	public void removeAttribute(String name) {
		// TODO Auto-generated method stub
		super.removeAttribute(name);
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return super.getLocale();
	}

	@Override
	public Enumeration getLocales() {
		// TODO Auto-generated method stub
		return super.getLocales();
	}

	@Override
	public boolean isSecure() {
		// TODO Auto-generated method stub
		return super.isSecure();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		// TODO Auto-generated method stub
		return super.getRequestDispatcher(path);
	}

	@Override
	public String getRealPath(String path) {
		// TODO Auto-generated method stub
		return super.getRealPath(path);
	}

	@Override
	public int getRemotePort() {
		// TODO Auto-generated method stub
		return super.getRemotePort();
	}

	@Override
	public String getLocalName() {
		// TODO Auto-generated method stub
		return super.getLocalName();
	}

	@Override
	public String getLocalAddr() {
		// TODO Auto-generated method stub
		return super.getLocalAddr();
	}

	@Override
	public int getLocalPort() {
		// TODO Auto-generated method stub
		return super.getLocalPort();
	}
	
	
	
}
