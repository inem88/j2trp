package com.heimore.j2trp.core;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

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
	
	@POST
	@Produces("text/html")
	@Consumes("application/x-www-form-urlencoded")
	@Path("/someFile.html")
	public Response executePost (@Context Request req, String message) throws IOException {
		
		System.out.println("Got here: " + message);
		ResponseBuilder builder = Response.ok();
		builder.entity("<html><head><title>MockTargetServer</title></head><body><pre>" + message + "</pre></body></html>");
		return builder.build();
	}
}
