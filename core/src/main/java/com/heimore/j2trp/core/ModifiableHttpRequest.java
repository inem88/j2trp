package com.heimore.j2trp.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
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

	String authType;
	Cookie[] cookies;
	Long dateHeader;
	Map<String, String> headers = new HashMap<String, String>();
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
	
	
	public ModifiableHttpRequest(HttpServletRequest request) {
		super(request);
	}

	@Override
	public String getAuthType() {
		return Toolbox.getValue(super.getAuthType(), authType, String.class);
	}

	@Override
	public Cookie[] getCookies() {
		// TODO Auto-generated method stub
		return super.getCookies();
	}

	@Override
	public long getDateHeader(String name) {
		// TODO Auto-generated method stub
		return super.getDateHeader(name);
	}

	@Override
	public String getHeader(String name) {
		// TODO Auto-generated method stub
		return super.getHeader(name);
	}

	@Override
	public Enumeration getHeaders(String name) {
		// TODO Auto-generated method stub
		return super.getHeaders(name);
	}

	@Override
	public Enumeration getHeaderNames() {
		// TODO Auto-generated method stub
		return super.getHeaderNames();
	}

	@Override
	public int getIntHeader(String name) {
		// TODO Auto-generated method stub
		return super.getIntHeader(name);
	}

	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return super.getMethod();
	}

	@Override
	public String getPathInfo() {
		// TODO Auto-generated method stub
		return super.getPathInfo();
	}

	@Override
	public String getPathTranslated() {
		// TODO Auto-generated method stub
		return super.getPathTranslated();
	}

	@Override
	public String getContextPath() {
		// TODO Auto-generated method stub
		return super.getContextPath();
	}

	@Override
	public String getQueryString() {
		// TODO Auto-generated method stub
		return super.getQueryString();
	}

	@Override
	public String getRemoteUser() {
		// TODO Auto-generated method stub
		return super.getRemoteUser();
	}

	@Override
	public boolean isUserInRole(String role) {
		// TODO Auto-generated method stub
		return super.isUserInRole(role);
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return super.getUserPrincipal();
	}

	@Override
	public String getRequestedSessionId() {
		// TODO Auto-generated method stub
		return super.getRequestedSessionId();
	}

	@Override
	public String getRequestURI() {
		// TODO Auto-generated method stub
		return super.getRequestURI();
	}

	@Override
	public StringBuffer getRequestURL() {
		// TODO Auto-generated method stub
		return super.getRequestURL();
	}

	@Override
	public String getServletPath() {
		// TODO Auto-generated method stub
		return super.getServletPath();
	}

	@Override
	public HttpSession getSession(boolean create) {
		// TODO Auto-generated method stub
		return super.getSession(create);
	}

	@Override
	public HttpSession getSession() {
		// TODO Auto-generated method stub
		return super.getSession();
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		// TODO Auto-generated method stub
		return super.isRequestedSessionIdValid();
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		// TODO Auto-generated method stub
		return super.isRequestedSessionIdFromCookie();
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		// TODO Auto-generated method stub
		return super.isRequestedSessionIdFromURL();
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		// TODO Auto-generated method stub
		return super.isRequestedSessionIdFromUrl();
	}

	@Override
	public ServletRequest getRequest() {
		// TODO Auto-generated method stub
		return super.getRequest();
	}

	@Override
	public void setRequest(ServletRequest request) {
		// TODO Auto-generated method stub
		super.setRequest(request);
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
