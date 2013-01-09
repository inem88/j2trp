package com.j2trp.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;

import org.springframework.web.servlet.view.RedirectView;

@Path("/")
public class MockTargetServer {
	
	@GET
	@Produces("text/html")
	@Path("/someFile.html")
	public Response executeGet (@Context Request req, @HeaderParam("X-Forwarded-For") String xff, @QueryParam("k1") String k1, @QueryParam("k2") String k2) throws IOException {
		
		ResponseBuilder builder = Response.ok();
		builder.header("X-Forwarded-For", xff);
		builder.entity("<html><head><title>MockTargetServer</title></head><body><h1>It works " + (k1 != null && k2 != null ? "with" : "without") + " query strings!</h1></body></html>");
		return builder.build();
	}
	
	@GET
//	@Produces("text/html")
	@Path("/redirect.html")
	public Response executeRedirect (@Context Request req) throws IOException, URISyntaxException {
		
		ResponseBuilder builder = Response.status(302);
		builder.header("Location", new URI("other_location.html?q1=v1"));
		return builder.build();
	}
	
	@GET
	@Produces("text/html")
	@Path("/someFileWithCookie.html")
	public Response executeGetWithCookie (@Context Request req, @CookieParam("TEST_COOKIE") String cookie, @CookieParam("TEST_COOKIE2") String cookie2) throws IOException {
		
		ResponseBuilder builder = Response.ok();
		NewCookie simpleCookie = new NewCookie("SIMPLE", "COOKIE", null, null, 0, null, -1, false);
		NewCookie domainCookie = new NewCookie("COMPLETE", "VALUE2", "/sfibonusadmin", ".example.org", "Some comment.", 0, true);
		builder.cookie(simpleCookie, domainCookie);
		String responseMessage = cookie + (cookie2 != null ? ":" + cookie2 : "");
		builder.entity("<html><head><title>MockTargetServer</title></head><body><h1>It works with cookie: " + responseMessage + "</h1></body></html>");
		return builder.build();
	}
	
	@POST
	@Produces("text/html")
	@Consumes("application/x-www-form-urlencoded")
	@Path("/someFile.html")
	public Response executePost (@HeaderParam("X-Auth-User") String userHeader, @Context Request req, @FormParam("userid") String username, @FormParam("password") String password) throws IOException {
		
		System.out.println("And the remote user is: " + userHeader);
		ResponseBuilder builder = Response.ok();
		builder.entity(String.format("<html><head><title>MockTargetServer</title></head><body>%s:%s</body></html>", username, password));
		return builder.build();
	}
	
	@GET
	@Produces("text/html")
	@Path("/500")
	public Response execute500Response (@Context Request req) throws IOException {
		
		throw new IOException();
	}
	
	@GET
	@Produces("text/html")
	@Path("interrupt")
	public Response executeInterruptedResponse (@Context Request req) throws IOException {
		
		ResponseBuilder builder = Response.serverError();
		builder.entity(null);
		return builder.build();
	}
}
