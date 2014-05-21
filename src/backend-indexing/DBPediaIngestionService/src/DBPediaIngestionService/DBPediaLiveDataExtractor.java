/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DBPediaIngestionService;

import java.io.*;
import java.util.*;
import org.openrdf.repository.*;
import org.openrdf.model.*;
import org.openrdf.rio.*;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.sparql.*;
import org.openrdf.query.*;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.sail.nativerdf.config.NativeStoreConfig;
import org.openrdf.rio.helpers.BasicParserSettings;

/**
 *
 * @author arunch
 * Runs an ingestion cycle by extracting incremental change from the dbpedia server into the 
 * added and removed local repositories
 */
public class DBPediaLiveDataExtractor
{
    private String endpointName;
    private String repositoryRootFolder;
    
    private static final String NEW_MOVIE_URI_REPOSITORY = "dbpedia-newmovieuri";
    private static final String VISITED_MOVIE_URI_REPOSITORY = "dbpedia-visitedmovieuri";
    public static final String ADDED_TRIPLES_REPOSITORY = "dbpedia-added";
    public static final String REMOVED_TRIPLES_REPOSITORY = "dbpedia-removed";
    
    private RepositoryConnection newMovieUriConnection;
    private RepositoryConnection visitedMovieUriConnection;
    private RepositoryConnection addedTriplesConnection;
    private RepositoryConnection removedTriplesConnection;
    
    private static final int QUERY_BATCH_SIZE = 1000;
    
    
    public DBPediaLiveDataExtractor() 
    {
        this.endpointName = Config.getInstance().getProperty("dbpediaEndPoint");
        this.repositoryRootFolder = Config.getInstance().getProperty("localRepositoryRootFolder");
        Logger.logMessage(String.format("DBPedia endpoint = %s , Local Repository Root = %s", this.endpointName, this.repositoryRootFolder), "DBPediaLiveDataExtractor"); 
    }
    
    /*
     * Runs an ingestion cycle for extracting data from dbpedia and updating the local repositories
     */
    public void extractData() throws OpenRDFException
    {
        this.initializeLocalRepositoryConnections();
        
        // from all movie uris in dbpedia, extract the ones that have not been visited so far
        //this.extractNewMovieUris();
        
        // process new movie uris to extract the movie details
        this.processNewMovieUris();
        
        this.closeLocalRepositoryConnections();
    }
    
    private void initializeLocalRepositoryConnections() throws OpenRDFException
    {
        this.newMovieUriConnection = this.getLocalRepositoryConnection(this.repositoryRootFolder, NEW_MOVIE_URI_REPOSITORY);
        this.visitedMovieUriConnection = this.getLocalRepositoryConnection(this.repositoryRootFolder, VISITED_MOVIE_URI_REPOSITORY);
        this.addedTriplesConnection = this.getLocalRepositoryConnection(this.repositoryRootFolder, ADDED_TRIPLES_REPOSITORY);
        this.removedTriplesConnection = this.getLocalRepositoryConnection(this.repositoryRootFolder, REMOVED_TRIPLES_REPOSITORY);
    }
    
    private RepositoryConnection getLocalRepositoryConnection(String repositoryRootFolder, String repositoryID) throws OpenRDFException
    {        
        try
        {              
            return LocalConnectionFactory.getInstance().getConnection(repositoryRootFolder, repositoryID);
        }
        catch(OpenRDFException e)
        {
            Logger.logMessage("Exception while getting connection to repository " + repositoryID, "getLocalRepositoryConnection");
            throw e;
        }        
    }
    
    private void closeLocalRepositoryConnections() throws OpenRDFException
    {
        this.newMovieUriConnection.close();
        this.visitedMovieUriConnection.close();
        this.addedTriplesConnection.close();
        this.removedTriplesConnection.close();
    }
    
    /*
     * extract new movie uris that so far have never been visited on dbpedia
     */
    private void extractNewMovieUris() throws OpenRDFException
    {        
        String movieUriQuery = this.getMovieUriQuery();
        
        // set up handler to distill the new movie uris (movies that have not been visited so far) from the dbpedia movie uris
        NewMovieUriDistiller newMovieUriDistiller = new NewMovieUriDistiller(this.newMovieUriConnection, this.visitedMovieUriConnection);        

        int retryCnt = 0;
        long offset;
        
        boolean endOfExecution = false;
        // iterate and prepare batch query (for different offset) and execute the query
        while(!endOfExecution)
        {                     
            offset = newMovieUriDistiller.getProcessedCount();
            Logger.logMessage(String.format("current offset = %d", offset), "extractNewMovieUris");            
            String queryBatch = this.prepareQueryBatch(movieUriQuery, offset, QUERY_BATCH_SIZE);
            
            try
            {  
                RepositoryConnection dbpediaConnection = RemoteConnectionFactory.getInstance().getConnection(this.endpointName);
                GraphQuery query = dbpediaConnection.prepareGraphQuery(QueryLanguage.SPARQL, queryBatch);
                query.evaluate(newMovieUriDistiller);
                dbpediaConnection.close();
                
                if(newMovieUriDistiller.getProcessedCount() <= offset)
                {
                    endOfExecution = true;
                }                                                            
            }
            catch(OpenRDFException e)
            {
                Logger.logMessage(String.format("Exception while executing queryBatch. Retrycnt = %d", ++retryCnt), "extractNewMovieUris");                
                Logger.logException("Exception while executing queryBatch",e);
            }                        
        }  
    }

    /*
     * visit the new movie uris on dbpedia to extract the movie details
     * and add the movie uri to visited
     */
    private void processNewMovieUris() throws OpenRDFException
    {        
        String movieUriQuery = this.getMovieUriQuery();
        
        // set up handler for processing new movie uris
        NewMovieUriProcessor newMovieUriProcessor = new NewMovieUriProcessor(
                this.newMovieUriConnection,
                this.visitedMovieUriConnection,
                this.addedTriplesConnection
                );       
        
        long iterationCnt = 0;
        
        // no of uris left to process
        long cntMovieUrisToProcess = this.newMovieUriConnection.size();
        long cntMovieUrisProcessed;
        
        // iterate in batches and process movie uris 
        while(cntMovieUrisToProcess > 0)
        {                     
            cntMovieUrisProcessed = newMovieUriProcessor.getProcessedCount();
            Logger.logMessage(String.format("Iteration cnt = %d. New movie uris to process = %d, processed = %d", 
                    ++iterationCnt, cntMovieUrisToProcess, cntMovieUrisProcessed), 
                    "processNewMovieUris");
            
            String queryBatch = this.prepareQueryBatch(movieUriQuery, 0, QUERY_BATCH_SIZE);
            
            GraphQuery query = this.newMovieUriConnection.prepareGraphQuery(QueryLanguage.SPARQL, queryBatch);
            query.evaluate(newMovieUriProcessor);
            
            cntMovieUrisToProcess = this.newMovieUriConnection.size();            
        }  
    }        
            
    private String getMovieUriQuery()
    {
        String queryString = 
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX dbo: <http://dbpedia.org/ontology/> " + 
                "CONSTRUCT { ?s rdf:type dbo:Film. } " +
                "WHERE { " + 
                "?s rdf:type dbo:Film. " +
                "} ";
        
        return queryString;
    }
    
    private String prepareQueryBatch(String query, long offset, int limit)
    {
        String queryBatch = String.format(
                "%s limit %d offset %d", 
                query,
                limit,
                offset);
        return queryBatch;
    }        
}
