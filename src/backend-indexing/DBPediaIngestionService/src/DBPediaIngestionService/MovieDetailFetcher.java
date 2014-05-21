/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DBPediaIngestionService;

import org.openrdf.OpenRDFException;
import org.openrdf.query.*;
import org.openrdf.repository.*;
import org.openrdf.rio.*;

/**
 *
 * @author Administrator
 */
public class MovieDetailFetcher
{
    private String endpointName;

    public MovieDetailFetcher() 
    {
        this.endpointName = Config.getInstance().getProperty("dbpediaEndPoint");
    }

    public void fetchData(String movieUri, RDFHandler handler) throws OpenRDFException, QueryEvaluationException 
    {
        RepositoryConnection dbpediaConnection = this.getDBPediaConnection();
        GraphQuery query = dbpediaConnection.prepareGraphQuery(QueryLanguage.SPARQL, this.getMovieDetailQueryString(movieUri));
        query.evaluate(handler);
        dbpediaConnection.close();
    }

    public GraphQueryResult fetchData(String movieUri) throws OpenRDFException, QueryEvaluationException 
    {
        RepositoryConnection dbpediaConnection = this.getDBPediaConnection();
        GraphQuery query = dbpediaConnection.prepareGraphQuery(QueryLanguage.SPARQL, this.getMovieDetailQueryString(movieUri));
        GraphQueryResult result = query.evaluate();
        dbpediaConnection.close();
        return result;
    }
    
    private RepositoryConnection getDBPediaConnection() throws OpenRDFException
    {
        return RemoteConnectionFactory.getInstance().getConnection(this.endpointName);
    }
            
    private String getMovieDetailQueryString(String uri)
    {
        String queryString = String.format(
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX dbo: <http://dbpedia.org/ontology/> " + 
                "CONSTRUCT { <%s> ?p ?o } " +
                "WHERE { " + 
                "<%s> ?p ?o. " + 
                "FILTER (regex(str(?p), 'type|sameas|screenplay|story|artist|cinematography|country|director|distributor|editing|label|language|runtime|lyrics|music|name|producer|release|starring|length|writer','i')). " +
                "}",
                uri,
                uri);
     
        return queryString;
    }
}

