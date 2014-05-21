/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cisiingestionservice;

import Common.*;
import java.util.*;
import org.openrdf.OpenRDFException;
import org.openrdf.model.*;
import org.openrdf.model.impl.*;
import org.openrdf.query.*;
import org.openrdf.repository.*;
import org.openrdf.rio.*;

/**
 *
 * @author Administrator
 */
public class MovieDetailFetcher
{
    private RepositoryConnection globalMoviesConnection;
    private CISIService cisiService;
    private ValueFactory valueFactory;
    private URI watchOptionPredicate;
    
    private static final String NO_WATCH_OPTION_VALUE = "none";    

    public MovieDetailFetcher(RepositoryConnection globalMoviesConnection) 
    {
        this.globalMoviesConnection = globalMoviesConnection;
        this.cisiService = new CISIService();
        this.valueFactory = ValueFactoryImpl.getInstance();
        this.watchOptionPredicate = this.valueFactory.createURI(PredicateConstants.WATCH_OPTION);          
    }

    /*
     * fetches the movie data (watchoptions) for the given movieUri from cisi
     */
    public Model fetchData(Resource movieUri) throws OpenRDFException
    {
        Model model = new LinkedHashModel();
        
        List<String> movieNames;
        try
        {
           movieNames = this.getMovieNames(movieUri);            
        }
        catch(OpenRDFException e)
        {            
            Logger.logException("Exception while getting movie names for movieuri " + movieUri, e);
            throw e;
        }   
        
        // iterate on the movie names until a cisi movie is found or we exhaust all names
        boolean foundCISIMovie = false;
        for(String name : movieNames)
        {
            Map<String,Object> movieInfo = this.cisiService.getMovieInfo(name);
            // check if any movieInfo is returned
            if((movieInfo != null) && (movieInfo.size() > 0))
            {
                foundCISIMovie = true;
                try
                {
                    model = this.parseWatchOptionStatements(movieInfo, movieUri);
                }
                catch(OpenRDFException e)
                {            
                    Logger.logException("Exception while preparing watch option statements for movieuri " + movieUri, e);
                    throw e;
                }                   
                break;
            }
        }
        
        if(!foundCISIMovie)
        {
            Logger.logMessage(String.format("CISI movie not found for movie uri : %s", movieUri), "MovieDetailFetcher");
        }
        
        return model;
    }
                
    /*
     * gets the movie names for this movie uri
     */
    private List<String> getMovieNames(Resource movieUri) throws OpenRDFException
    {
        List<String> names = new ArrayList<String>();
        
        // prepare and execute tuple query
        String movieNameQuery = this.getMovieNameQuery(movieUri);
        TupleQuery query = this.globalMoviesConnection.prepareTupleQuery(QueryLanguage.SPARQL, movieNameQuery);
        TupleQueryResult result = query.evaluate();
        
        while(result.hasNext())
        {
            BindingSet solution = result.next();
            Value v = solution.getValue("name");
            if(v != null)
            {
                names.add(v.stringValue());
            }
        }
        
        return names;
    }
    
    private String getMovieNameQuery(Resource uri)
    {
        String uriStr = uri.stringValue();
        String queryString = String.format(
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX dbo: <http://dbpedia.org/ontology/> " + 
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +                 
                "SELECT DISTINCT (str(?n) as ?name) " +
                "WHERE { " + 
                "{ <%s> foaf:name ?n. } " + 
                "UNION " + 
                "{ <%s> rdfs:label ?n. } " +
                "}",
                uriStr,
                uriStr);
     
        return queryString;
    }  
    
    /*
     * parses the watch option statements from the movieInfo object
     * if no watch options exist in movieInfo, then a single watch option statment 
     * with "None" value is returned in the Model 
     */
    private Model parseWatchOptionStatements(Map<String,Object> movieInfo, Resource movieUri) throws OpenRDFException
    {        
        LinkedHashModel model = new LinkedHashModel();
        List<String> watchOptions = this.parseWatchOptions(movieInfo);
        
        if(watchOptions.size() > 0)
        {      
            for(String option : watchOptions)
            {
                Statement st = this.prepareWatchOptionStatement(movieUri, option);
                model.add(st);
            }
        }       
        else
        {
            // add "none"
            Statement st = this.prepareWatchOptionStatement(movieUri, NO_WATCH_OPTION_VALUE);
            model.add(st);
        }
        
        return model;
    }
    
    /*
     * prepare a flat list of watch options from the movie info object
     * TODO: return a dictionary, so that multiple statements could be generated for each watch option 
     */
    private List<String> parseWatchOptions(Map<String,Object> movieInfo)
    {
        List<String> watchOptions = new ArrayList<String>();
        
        for(String mediaType : this.cisiService.getMediaTypes())
        {
            Map<String,Object> optionsWithMediaType = (Map<String,Object>) movieInfo.get(mediaType);
            if(optionsWithMediaType != null)
            {
                for(String provider : optionsWithMediaType.keySet())
                {
                    watchOptions.add(String.format("%s_%s", mediaType, provider));                
                }
            }
        }        
        
        return  watchOptions;
    }
    
    private Statement prepareWatchOptionStatement(Resource movieUri, String watchOptionValue)
    {
        Value object = this.valueFactory.createLiteral(watchOptionValue);
        return this.valueFactory.createStatement(movieUri, this.watchOptionPredicate, object);
    }
}

