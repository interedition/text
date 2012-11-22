package eu.interedition.text.http;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import com.google.common.base.Joiner;
import com.google.common.io.Closeables;

import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.lisp.Expression;
import eu.interedition.text.lisp.LispParser;
import eu.interedition.text.lisp.LispParserException;
import eu.interedition.text.simple.KeyValues;

@Path("/")
public class RepositoryResource {


	
	@Path("")
	@GET
	public String query(@Context Request request, @QueryParam("q") String q) throws LispParserException, IOException {
		final LispParser parser = new LispParser(q);
		Expression expression = parser.expression();
		//TODO: translate the query
		return String.format("You have asked me %s. A very good question indeed!", q);
	}

}
