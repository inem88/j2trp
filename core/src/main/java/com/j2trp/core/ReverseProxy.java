/*
   Copyright 2015 Daniel Roig

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j2trp.core.config.Setting;
import com.j2trp.core.config.Settings;

public class ReverseProxy extends HttpServlet {
	private static final Logger LOG = LoggerFactory.getLogger(ReverseProxyByDomain.class);
	Map<String, ReverseProxyByDomain> domainProxies = new HashMap();
	ServletConfig config;
	
	private transient Settings settings;
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String domain = req.getServerName();
		ReverseProxyByDomain rp = getReverseProxy(domain, config.getServletContext().getContextPath());
		rp.execute(req, resp);
	}
	
	/**
	 * Servlet initialization method for this proxy.
	 * @param config The configuration object coming from the Servlet container.
	 * @throws ServletException If there's an error while extracting the configuration.
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		this.config = config;
		File configFile = new File("/"+config.getInitParameter("configFile"));
		
		if (!configFile.isAbsolute()) {
		  LOG.info("configFile is determined to be " + configFile);
		}
		if (!configFile.exists()) {
		  throw new ServletException("configFile doesn't appear to exist @ " + configFile);
		}
		
		try {
		  settings = new Settings(configFile);
		}
		catch (IOException e) {
		  throw new ServletException(e);
		}
		
	}

	
	public ReverseProxyByDomain getReverseProxy(String domain, String baseUri) throws IOException {
		if(domainProxies.get(domain)==null) {
			synchronized (domainProxies) {
				ReverseProxyByDomain rp = new ReverseProxyByDomain(settings, domain, baseUri);
				domainProxies.put(domain, rp);
			}
		}
		return domainProxies.get(domain);
	}
	
}
