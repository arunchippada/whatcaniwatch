/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DBPediaIngestionService;

import org.openrdf.OpenRDFException;
import org.openrdf.query.*;
import org.openrdf.model.*;
import org.openrdf.repository.*;
import org.openrdf.repository.util.*;
import org.openrdf.rio.*;

/**
 *
 * @author Administrator
 */
public class NewMovieUriDistiller implements RDFHandler
{
    private long processedCnt = 0;
    private RDFInserter inserter;
    private RepositoryConnection newMovieUriConnection;
    private RepositoryConnection visitedMovieUriConnection;

    public NewMovieUriDistiller(RepositoryConnection newMovieUriConnection, RepositoryConnection visitedMovieUriConnection) 
    {
        this.newMovieUriConnection = newMovieUriConnection;
        inserter = new RDFInserter(newMovieUriConnection);
        this.visitedMovieUriConnection = visitedMovieUriConnection;
    }

    @Override
    public void startRDF() throws RDFHandlerException 
    {
        try 
        {
             this.newMovieUriConnection.begin();
        } 
        catch (RepositoryException e) 
        {
             throw new RDFHandlerException(e);
        }
        inserter.startRDF();
    }

    @Override
    public void endRDF() throws RDFHandlerException 
    {
        try 
        {
             this.newMovieUriConnection.commit();
        } 
        catch (RepositoryException e) 
        {
             throw new RDFHandlerException(e);
        }        
        inserter.endRDF();
    }

    @Override
    public void handleNamespace(String prefix, String uri)
            throws RDFHandlerException 
    {
        inserter.handleNamespace(prefix, uri);
    }

    @Override
    public void handleStatement(Statement st) throws RDFHandlerException 
    {
        String movieUri = st.getSubject().stringValue(); 
        boolean isVisited;
        try
        {
            isVisited = isVisitedUri(movieUri);
        }
        catch (OpenRDFException e) 
        {
             throw new RDFHandlerException(e);
        }        
        
        if(!isVisited)
        {
            inserter.handleStatement(st);
        }
        processedCnt++;
    }

    @Override
    public void handleComment(String comment) throws RDFHandlerException 
    {
        inserter.handleComment(comment);
    }        
    
    public long getProcessedCount()
    {
        return processedCnt;
    }
    
    private boolean isVisitedUri(String movieUri) throws OpenRDFException
    {
        String checkForVisitedQuery = this.getCheckForVisitedQuery(movieUri);
        BooleanQuery query = this.visitedMovieUriConnection.prepareBooleanQuery(QueryLanguage.SPARQL, checkForVisitedQuery);
        return query.evaluate();
    }
    
    private String getCheckForVisitedQuery(String movieUri)
    {
        String queryString = String.format(
        "ASK WHERE { <%s> ?p ?o }",
        movieUri);
     
        return queryString;
    }
}
