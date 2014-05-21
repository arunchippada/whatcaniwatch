/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cisiingestionservice;

import Common.*;
import java.io.*;
import org.openrdf.repository.*;
import org.openrdf.OpenRDFException;
import org.openrdf.query.*;

/**
 *
 * @author Administrator
 * Runs an ingestion cycle by extracting incremental change from the CISI server into the 
 * added and removed local repositories
 */
public class CISIMovieDataExtractor
{
    private String localRepositoryRootFolder;
    private String globalRepositoryRootFolder;
    private String updatesOutputFolder;
    
    private static final String NEW_MOVIE_URI_REPOSITORY = "cisi-newmovieuri";
    private static final String VISITED_MOVIE_URI_REPOSITORY = "cisi-visitedmovieuri";
    public static final String ADDED_TRIPLES_REPOSITORY = "cisi-added";
    public static final String REMOVED_TRIPLES_REPOSITORY = "cisi-removed";
    private static final String GLOBAL_MOVIES_REPOSITORY = "movies-global";    
    
    private RepositoryConnection newMovieUriConnection;
    private RepositoryConnection visitedMovieUriConnection;
    private RepositoryConnection addedTriplesConnection;
    private RepositoryConnection removedTriplesConnection;
    private RepositoryConnection globalMoviesConnection;

    // max number of triples/statements read into memory from a query
    private static final int QUERY_BATCH_SIZE = 1000;

    // number of batches of old movies
    private static final int OLD_MAX_BATCHES_TO_PROCESS = 10;
                
    public CISIMovieDataExtractor()
    {
        this.localRepositoryRootFolder = Config.getInstance().getProperty("localRepositoryRootFolder");
        this.globalRepositoryRootFolder = Config.getInstance().getProperty("globalRepositoryRootFolder");        
        Logger.logMessage(String.format("Local Repository Root = %s, Global Repository Root = %s", this.localRepositoryRootFolder, this.globalRepositoryRootFolder), "CISIMovieDataExtractor");         
        
        this.updatesOutputFolder = Config.getInstance().getProperty("updatesOutputFolder");
        Logger.logMessage(String.format("Updates Output Folder = %s", this.updatesOutputFolder), "CISIMovieDataExtractor");
    }

    /*
     * Main method for extracting data from CISI and updating the local repositories
     * TODO: do we need to set up threads for extracting data for each querybatch from CISI?        
     */
    public void extractData() throws IOException, OpenRDFException
    {
        this.initializeLocalRepositoryConnections();
        
        this.clearOldChanges();
        
        // from all movie uris in movies-global, extract the ones that have not been visited so far
        this.extractNewMovieUris();
        
        // process new movie uris to extract the movie details from CISI
        this.processNewMovieUris();

        // process old (visited) movie uris to extract the changes in movie details from CISI
        this.processOldMovieUris();
        
        // publish changes to updates output folder
        this.publishChanges();
        
        this.closeLocalRepositoryConnections();
    }

    private void initializeLocalRepositoryConnections() throws OpenRDFException
    {
        this.newMovieUriConnection = this.getLocalRepositoryConnection(this.localRepositoryRootFolder, NEW_MOVIE_URI_REPOSITORY);
        this.visitedMovieUriConnection = this.getLocalRepositoryConnection(this.localRepositoryRootFolder, VISITED_MOVIE_URI_REPOSITORY);
        this.addedTriplesConnection = this.getLocalRepositoryConnection(this.localRepositoryRootFolder, ADDED_TRIPLES_REPOSITORY);
        this.removedTriplesConnection = this.getLocalRepositoryConnection(this.localRepositoryRootFolder, REMOVED_TRIPLES_REPOSITORY);
        this.globalMoviesConnection = this.getLocalRepositoryConnection(this.globalRepositoryRootFolder, GLOBAL_MOVIES_REPOSITORY);
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
        this.globalMoviesConnection.close();
    }
    
    /*
     * clear the old changes that may be present in the added and removed local repositories,
       in preparation for computing the new changes.
     */
    private void clearOldChanges() throws OpenRDFException
    {
        this.addedTriplesConnection.clear();
        this.removedTriplesConnection.clear();
    }
    
    /*
     * extract new movie uris that so far have never been visited on cisi
     */
    private void extractNewMovieUris() throws OpenRDFException
    {        
        String movieUriQuery = this.getNoWatchOptionMovieUriQuery();
        
        // set up handler to distill the new movie uris (movies that have not been visited so far) from the global movie uris
        NewMovieUriDistiller newMovieUriDistiller = new NewMovieUriDistiller(this.newMovieUriConnection, this.visitedMovieUriConnection);        

        long offset;
        
        boolean endOfExecution = false;
        // iterate and prepare batch query (for different offset) and execute the query
        while(!endOfExecution)
        {                     
            offset = newMovieUriDistiller.getProcessedCount();
            Logger.logMessage(String.format("current offset = %d", offset), "extractNewMovieUris");            
            String queryBatch = this.prepareQueryBatch(movieUriQuery, offset, QUERY_BATCH_SIZE);

            GraphQuery query = this.globalMoviesConnection.prepareGraphQuery(QueryLanguage.SPARQL, queryBatch);
            query.evaluate(newMovieUriDistiller);

            if(newMovieUriDistiller.getProcessedCount() <= offset)
            {
                endOfExecution = true;
            }                                                            
        }  
    }
    
    /*
     * visit the new movie uris on CISI to extract the movie details
     * and add the movie uri to visited
     */
    private void processNewMovieUris() throws OpenRDFException
    {        
        String movieUriQuery = this.getMovieUriQuery();
        
        // set up handler for processing new movie uris
        NewMovieUriProcessor newMovieUriProcessor = new NewMovieUriProcessor(
                this.newMovieUriConnection,
                this.visitedMovieUriConnection,
                this.addedTriplesConnection,
                this.globalMoviesConnection
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

    /*
     * revisit old (previously visited) movie uris on CISI to extract change (added and removed)
     * and update the lastvisiteddate for the movie uri in visited
     */
    private void processOldMovieUris() throws OpenRDFException
    {        
        String movieUriQuery = this.getVisitedMovieUriQuery();
        
        // set up handler for processing visited movie uris
        OldMovieUriProcessor oldMovieUriProcessor = new OldMovieUriProcessor(
                this.visitedMovieUriConnection,
                this.addedTriplesConnection,
                this.removedTriplesConnection,
                this.globalMoviesConnection
                );       
        
        long cntMovieUrisProcessed;        
        // iterate in batches and process movie uris 
        for(int i = 0; i < OLD_MAX_BATCHES_TO_PROCESS; ++i)
        {                            
            String queryBatch = this.prepareQueryBatch(movieUriQuery, 0, QUERY_BATCH_SIZE);            
            GraphQuery query = this.visitedMovieUriConnection.prepareGraphQuery(QueryLanguage.SPARQL, queryBatch);
            query.evaluate(oldMovieUriProcessor);
            
            cntMovieUrisProcessed = oldMovieUriProcessor.getProcessedCount();
            Logger.logMessage(String.format("Iteration %d. Old movie uris processed = %d", 
                    i, cntMovieUrisProcessed), 
                    "processOldMovieUris");            
        }  
    }        
    
    private String getNoWatchOptionMovieUriQuery()
    {
        String queryString = 
                "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX cisi: <http://www.canistream.it/> " +
                "CONSTRUCT { ?s rdf:type dbo:Film. } " +
                "WHERE { " + 
                "?s rdf:type dbo:Film. " +
                "OPTIONAL {?s cisi:watchoption ?watchoption.} FILTER (!bound(?watchoption))" +
                "}";
        
        return queryString;
    }
    
    private String getMovieUriQuery()
    {
        String queryString = 
                "PREFIX dbo: <http://dbpedia.org/ontology/> " + 
                "CONSTRUCT { ?s rdf:type dbo:Film. } " +
                "WHERE { " + 
                "?s rdf:type dbo:Film. " +
                "} ";
        
        return queryString;
    }
    
    /*
     * returns the query string for obtaining visited movie uris 
     * ordered by earliest visited time
     */
    private String getVisitedMovieUriQuery()
    {
        String queryString = 
                "PREFIX wwp: <http://whatcaniwatch.com/private/> " +
                "CONSTRUCT { ?s wwp:lastvisiteddate ?date. } " +
                "WHERE { " + 
                "?s wwp:lastvisiteddate ?date. " +
                "} ORDER BY ?date";
        
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
    
    /*
     * publishes changes in the added and removed repositories to ntriple files 
     * in the updatesOutputFolder
     */
    private void publishChanges() throws IOException, OpenRDFException
    {
        String updateFilePrefix = Config.getInstance().getProperty("updateFilePrefix");
        String addedFileName = String.format("%s\\%s_%s", this.updatesOutputFolder, updateFilePrefix, "added");
        String removedFileName = String.format("%s\\%s_%s", this.updatesOutputFolder, updateFilePrefix, "removed");
        
        RepositoryPublisher publisher = new RepositoryPublisher();
        publisher.publish(this.addedTriplesConnection, addedFileName);
        publisher.publish(this.removedTriplesConnection, removedFileName);
    }
}
