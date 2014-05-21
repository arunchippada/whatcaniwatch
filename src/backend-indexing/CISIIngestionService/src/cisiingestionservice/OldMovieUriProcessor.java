/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cisiingestionservice;

import Common.*;
import java.util.*;
import org.openrdf.OpenRDFException;
import org.openrdf.model.*;
import org.openrdf.query.*;
import org.openrdf.repository.*;
import org.openrdf.rio.*;
import org.openrdf.rio.helpers.*;

/**
 *
 * @author Administrator
 */
public class OldMovieUriProcessor extends RDFHandlerBase
{
    private long processedCnt = 0;
    
    private RepositoryConnection visitedMovieUriConnection;    
    private RepositoryConnection addedTriplesConnection;
    private RepositoryConnection removedTriplesConnection;    
    private RepositoryConnection globalMoviesConnection;
    
    private MovieDetailFetcher movieDetailFetcher;
    
    private ValueFactory valueFactory;
    private URI lastVisitedPredicate;    
    private Value lastVisitedTimeValue;
    
    public OldMovieUriProcessor(
            RepositoryConnection visitedMovieUriConnection, 
            RepositoryConnection addedTriplesConnection,
            RepositoryConnection removedTriplesConnection,
            RepositoryConnection globalMoviesConnection) 
    {
        this.visitedMovieUriConnection = visitedMovieUriConnection;
        this.addedTriplesConnection = addedTriplesConnection;
        this.removedTriplesConnection = removedTriplesConnection;        
        this.globalMoviesConnection = globalMoviesConnection;
        
        this.movieDetailFetcher = new MovieDetailFetcher(this.globalMoviesConnection);
        this.valueFactory = visitedMovieUriConnection.getValueFactory();
        this.lastVisitedPredicate = this.valueFactory.createURI(PredicateConstants.LAST_VISITED_DATE);  
        this.lastVisitedTimeValue = this.valueFactory.createLiteral(new Date());
    }

    @Override
    public void startRDF() throws RDFHandlerException 
    {
        //TO DO: Do we need newMovieUriConnection.begin and visitedMovieUriConnection.begin
        super.startRDF();
    }

    @Override
    public void endRDF() throws RDFHandlerException 
    {
       //TO DO: newMovieUriConnection.commit and visitedMovieUriConnection.commit
        super.endRDF();
    }

    @Override
    public void handleStatement(Statement st) throws RDFHandlerException 
    {
        Resource movieUri = st.getSubject();        
        String movieUriString = movieUri.stringValue();        
                
        try
        {
            Collection<Statement> oldWatchOptions = this.getMovieOldWatchOptions(movieUriString);
            Collection<Statement> newWatchOptions = this.movieDetailFetcher.fetchData(movieUri);
              
            if(!newWatchOptions.isEmpty())
            {
                RdfChangeComputer changeComputer = new RdfChangeComputer(oldWatchOptions, newWatchOptions);
                Model addedStatements = changeComputer.getAddedStatements();
                Model removedStatements = changeComputer.getRemovedStatements();
                
                boolean added = !addedStatements.isEmpty();
                boolean removed = !removedStatements.isEmpty();
                
                if(added)
                {
                    this.addedTriplesConnection.add(addedStatements);             
                }
                
                if(removed)
                {
                    this.removedTriplesConnection.add(removedStatements);
                }
                
                if(added || removed)
                {
                    Logger.logMessage(String.format("Successfully processed change for movie uri %s. Added = %b. Removed = %b", 
                            movieUriString, added, removed), 
                            "OldMovieUriProcessor");
                }
                else
                {
                    Logger.logMessage(String.format("No change for movie uri %s", movieUriString), "OldMovieUriProcessor");
                }
            }
            else
            {
                Logger.logMessage(String.format("Empty new watch options. Retaining old values for movie uri %s", movieUriString), "OldMovieUriProcessor");
            }

            try
            {
                // remove the old visited statement before adding the new visited statement, 
                // to avoid a duplicate in case there is an exception
                this.visitedMovieUriConnection.remove(st);                
                this.visitedMovieUriConnection.add(this.prepareVisitedMovieUriStatement(movieUri));
                processedCnt++;
            }
            catch(OpenRDFException e)
            {
                Logger.logException("OpenRDFException while removing/adding on visited movieuri = " + movieUri, e);                
            }            
        }
        catch(OpenRDFException e)
        {                        
            Logger.logException("OpenRDFException while processing change for movieuri " + movieUriString, e);
        }                                        
    }
    
    public long getProcessedCount()
    {
        return processedCnt;
    }    

    /*
     * returns the existing (old) set of watch option statements for a movie
     * that are present in the global movies repository
     */
    private Collection<Statement> getMovieOldWatchOptions(String uri) throws OpenRDFException
    {
        String queryString = this.getMovieOldWatchOptionsQuery(uri);
        StatementCollector collector = new StatementCollector();
        
        GraphQuery query = this.globalMoviesConnection.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        query.evaluate(collector);
        return collector.getStatements();
    }
    
    /*
     * returns the query string for obtaining the movie's old (existing) watch
     * options data
     */
    private String getMovieOldWatchOptionsQuery(String uri)
    {        
        String queryString = String.format(
                "PREFIX cisi: <http://www.canistream.it/> " +
                "CONSTRUCT { <%s> cisi:watchoption ?watchoption } " +
                "WHERE { " + 
                "<%s> cisi:watchoption ?watchoption. " + 
                "}",
                uri,
                uri);
        
        return queryString;
    }    
    
    private Statement prepareVisitedMovieUriStatement(Resource movieUri)
    {        
        return this.valueFactory.createStatement(movieUri, this.lastVisitedPredicate, this.lastVisitedTimeValue);
    }    
}

