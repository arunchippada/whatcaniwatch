/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DBPediaIngestionService;

import java.util.*;
import org.openrdf.OpenRDFException;
import org.openrdf.model.*;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.*;
import org.openrdf.repository.util.RDFInserter;
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
    
    private MovieDetailFetcher movieDetailFetcher;
    
    private ValueFactory valueFactory;
    private URI lastVisitedPredicate;    
    private Value lastVisitedTimeValue;
    
    public NewMovieUriProcessor(RepositoryConnection newMovieUriConnection, RepositoryConnection visitedMovieUriConnection, RepositoryConnection addedTriplesConnection) 
    {
        this.newMovieUriConnection = newMovieUriConnection;
        this.visitedMovieUriConnection = visitedMovieUriConnection;
        this.addedTriplesConnection = addedTriplesConnection;
        
        this.movieDetailFetcher = new MovieDetailFetcher();
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
        RDFInserter inserter = new RDFInserter(this.addedTriplesConnection);
        boolean handledMovieUri = false;
                
        try
        {
            this.movieDetailFetcher.fetchData(movieUriString, inserter);
            Logger.logMessage(String.format("Successfully processed statements for movie uri %s", movieUriString), "NewMovieUriProcessor");
            handledMovieUri = true;
        }
        catch(QueryEvaluationException qee)
        {
            Logger.logMessage(String.format("QueryEvaluationException for movie uri %s", movieUriString), "NewMovieUriProcessor");
            Logger.logException("QueryEvaluationException while fetching movie detail for movieuri " + movieUriString, qee);
            handledMovieUri = true;
        }
        catch(OpenRDFException e)
        {                        
            Logger.logMessage(String.format("OpenRDFException for movie uri %s", movieUriString), "NewMovieUriProcessor");
            Logger.logException("OpenRDFException while fetching movie detail for movieuri " + movieUriString, e);
        }                                
        
        if(handledMovieUri)
        {
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

