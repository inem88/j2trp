package com.heimore.j2trp.core;

import java.io.IOException;

import javax.ws.rs.GET;
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
		builder.entity("<html><head><title>MockTargetServer</title></head><body><h1>It works without query strings!</h1></body></html>");
		return builder.build();
	}
}
