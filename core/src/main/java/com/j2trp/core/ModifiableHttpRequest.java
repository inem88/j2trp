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
	Map<String, Object> attributes = new HashMap<String, Object>();
	String contentType;
	
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
		// TODO Auto-generated method stub
		return super.getParameterValues(name);
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
		// TODO Auto-generated method stub
		super.setAttribute(name, o);
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
	public Enumeration getLocales() {
		// TODO: Added enumeration for locales
		return super.getLocales();
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
	
	
	
}
