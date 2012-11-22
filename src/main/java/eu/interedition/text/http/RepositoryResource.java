package eu.interedition.text.http;

import java.io.IOException;
import java.io.StringReader;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.base.Joiner;
import com.google.common.io.Closeables;
import com.google.inject.Inject;

import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.TextConstants;
import eu.interedition.text.h2.H2TextRepository;
import eu.interedition.text.h2.LayerRelation;
import eu.interedition.text.lisp.Expression;
import eu.interedition.text.lisp.LispParser;
import eu.interedition.text.lisp.LispParserException;
import eu.interedition.text.simple.KeyValues;

@Path("/")
public class RepositoryResource {


	
	private String query(@Context Request request, String q) throws LispParserException, IOException {
		final LispParser parser = new LispParser(q);
		Expression expression = parser.expression();
		//TODO: translate the query
		return String.format("You have asked me %s. A very good question indeed!", q);
	}

}
