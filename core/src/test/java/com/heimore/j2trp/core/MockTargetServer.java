package com.heimore.j2trp.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.springframework.web.servlet.view.RedirectView;

@Path("/")
public class MockTargetServer {
	
	@GET
	@Produces("text/html")
	@Path("/someFile.html")
	public Response executeGet (@Context Request req, @QueryParam("k1") String k1, @QueryParam("k2") String k2) throws IOException {
		
		System.out.println("Got here!");
		ResponseBuilder builder = Response.ok();
		builder.entity("<html><head><title>MockTargetServer</title></head><body><h1>It works " + (k1 != null && k2 != null ? "with" : "without") + " query strings!</h1></body></html>");
		return builder.build();
	}
	
	@GET
//	@Produces("text/html")
	@Path("/redirect.html")
	public Response executeRedirect (@Context Request req) throws IOException, URISyntaxException {
		
		System.out.println("Got here!");
		ResponseBuilder builder = Response.status(302);
		builder.header("Location", new URI("other_location.html?q1=v1"));
		return builder.build();
	}
	
	@GET
	@Produces("text/html")
	@Path("/someFileWithCookie.html")
	public Response executeGetWithCookie (@Context Request req, @CookieParam("TEST_COOKIE") String cookie, @CookieParam("TEST_COOKIE2") String cookie2) throws IOException {
		
		System.out.println("Got here with cookie: " + cookie);
		ResponseBuilder builder = Response.ok();
		String responseMessage = cookie + (cookie2 != null ? ":" + cookie2 : "");
		builder.entity("<html><head><title>MockTargetServer</title></head><body><h1>It works with cookie: " + responseMessage + "</h1></body></html>");
		return builder.build();
	}
	
	@POST
	@Produces("text/html")
	@Consumes("application/x-www-form-urlencoded")
	@Path("/someFile.html")
	public Response executePost (@Context Request req, @FormParam("userid") String username, @FormParam("password") String password) throws IOException {
		
		System.out.println("Got here: " + username);
		ResponseBuilder builder = Response.ok();
		builder.entity(String.format("<html><head><title>MockTargetServer</title></head><body>%s:%s</body></html>", username, password));
		return builder.build();
	}
}
