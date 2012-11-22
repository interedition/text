package eu.interedition.text.http;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;

import com.google.common.base.Joiner;
import com.google.common.io.Closeables;
import com.google.inject.Inject;

import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.TextRepository;
import eu.interedition.text.h2.H2TextRepository;
import eu.interedition.text.lisp.Expression;
import eu.interedition.text.lisp.LispParser;
import eu.interedition.text.lisp.LispParserException;
import eu.interedition.text.lisp.QueryParser;
import eu.interedition.text.simple.KeyValues;

@Path("/")
public class RepositoryResource {

	private TextRepository<JsonNode> textRepository;
	
	@Inject
	public RepositoryResource(H2TextRepository<JsonNode> textRepository) {
		this.textRepository = textRepository;
	}
	
	@Path("")
	@GET
	public QueryResult<JsonNode> query(@Context Request request, @QueryParam("q") String q) throws LispParserException, IOException {
		final QueryParser<JsonNode> parser = new QueryParser<JsonNode>(textRepository);
		final Query query = parser.parse(q);
		
		return textRepository.query(query);
		
	}

}
