/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cisiingestionservice;

import Common.*;
import java.util.*;
import org.openrdf.OpenRDFException;
import org.openrdf.model.*;
import org.openrdf.repository.*;
import org.openrdf.rio.*;
import org.openrdf.rio.helpers.*;

/**
 *
 * @author Administrator
 */
public class NewMovieUriProcessor extends RDFHandlerBase
{
    private long processedCnt = 0;
    
    private RepositoryConnection newMovieUriConnection;
    private RepositoryConnection visitedMovieUriConnection;    
    private RepositoryConnection addedTriplesConnection;
    private RepositoryConnection globalMoviesConnection;
    
    private MovieDetailFetcher movieDetailFetcher;
    
    private ValueFactory valueFactory;
    private URI lastVisitedPredicate;    
    private Value lastVisitedTimeValue;
    
    public NewMovieUriProcessor(
            RepositoryConnection newMovieUriConnection, 
            RepositoryConnection visitedMovieUriConnection, 
            RepositoryConnection addedTriplesConnection,
            RepositoryConnection globalMoviesConnection) 
    {
        this.newMovieUriConnection = newMovieUriConnection;
        this.visitedMovieUriConnection = visitedMovieUriConnection;
        this.addedTriplesConnection = addedTriplesConnection;
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
            this.addedTriplesConnection.add(this.movieDetailFetcher.fetchData(movieUri));            
            Logger.logMessage(String.format("Successfully processed statements for movie uri %s", movieUriString), "NewMovieUriProcessor");
            
            try
            {
                // remove from newmovieuri before adding to visited, so that in case there is an exception, 
                // it will prevent from trying to add a duplicated visited record for this movie uri in future
                this.newMovieUriConnection.remove(st);                
                this.visitedMovieUriConnection.add(this.prepareVisitedMovieUriStatement(movieUri));
                processedCnt++;
            }
            catch(OpenRDFException e)
            {
                Logger.logException("OpenRDFException while removing from newmovieuri or adding to visited movieuri = " + movieUri, e);                
            }            
        }
        catch(OpenRDFException e)
        {                        
            Logger.logException("OpenRDFException while fetching movie detail for movieuri " + movieUriString, e);
        }                                        
    }
    
    public long getProcessedCount()
    {
        return processedCnt;
    }    
    
    private Statement prepareVisitedMovieUriStatement(Resource movieUri)
    {        
        return this.valueFactory.createStatement(movieUri, this.lastVisitedPredicate, this.lastVisitedTimeValue);
    }    
}

