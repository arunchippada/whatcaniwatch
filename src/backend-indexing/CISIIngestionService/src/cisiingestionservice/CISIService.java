/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cisiingestionservice;

import Common.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.*;
import java.util.*;
import java.net.URLEncoder;
import org.jsoup.nodes.*;
import org.jsoup.select.*;



/**
 *
 * @author Administrator
 */
public class CISIService
{
    private static final String MOVIE_SEARCH_PATH = "search/movie";
    private static final String SERVICES_QUERY_PATH = "services/query";
    
    private static final int MAX_RETRY_CNT = 5;
    
    public static final String MOVIE_ATTRIB_ID = "movieid";
    public static final String MOVIE_ATTRIB_TITLE = "movietitle";
    public static final String MOVIE_ATTRIB_RELEASE_YEAR = "releaseyear";
    public static final String MOVIE_ATTRIB_STREAMING = "streaming";
    public static final String MOVIE_ATTRIB_RENTAL = "rental";
    public static final String MOVIE_ATTRIB_PURCHASE = "purchase";
    public static final String MOVIE_ATTRIB_DVD = "dvd";    
    public static final String MOVIE_ATTRIB_XFINITY = "xfinity";
    
    private String[] mediaTypes = {
        MOVIE_ATTRIB_STREAMING,
        MOVIE_ATTRIB_RENTAL,
        MOVIE_ATTRIB_PURCHASE,
        MOVIE_ATTRIB_DVD,
        MOVIE_ATTRIB_XFINITY
    };
    
    private CISIServerConnection cisiServerConnection = new CISIServerConnection();
    
    /*
     * gets the movie information, which includes basic movie properties such as 
     * the movieid, title, release year and the watch options (streaming, dvd etc), 
     * given the input movie title.
     * 
     * If movie with the title can't be found returns an empty Map object. 
     * Otherwise, atleast returns the basic movie info (movie id, title)
     * 
     * TODO: Also takes release year, so that multiple movies with the same title
     * can be uniquely identified
     * 
     */
    public Map<String,Object> getMovieInfo(String movieName)
    {
        Hashtable<String,Object> movieInfo = new Hashtable<>();
        
        boolean foundMovie = false;
        long retryCnt = 0;
        while(true)
        {
            try
            {
                if(!this.trySetMovieBasicInfo(movieName, movieInfo))
                {
                    Logger.logMessage(String.format("Movie not found for %s", movieName), "getMovieInfo");
                }
                else
                {
                    foundMovie = true;
                }
            }
            catch(IOException e)
            {
                Logger.logException("Exception while getting movie basic info for " + movieName, e);
                Logger.logError(String.format("retrycnt = %d", ++retryCnt)); 
                if(retryCnt <= MAX_RETRY_CNT)
                {
                    continue;
                }
                else
                {
                    Logger.logError(String.format("Max retry cnt reaced. Aborting movie basic info search for %s", movieName));
                }
            }
            catch(Exception ex)
            {
                Logger.logException("Exception while getting movie basic info", ex);
                Logger.logError(String.format("skipping movie name %s", movieName));
            }
            break;
        }
        
        if(foundMovie)
        {
            String movieID = (String) movieInfo.get(CISIService.MOVIE_ATTRIB_ID);
            if(movieID != null && !movieID.isEmpty())
            {
                this.setMovieWatchOptions(movieID, movieInfo);
            }
            else
            {
                Logger.logMessage(String.format("MovieId not found for movie matching %s", movieName), "getMovieInfo");
            }
        }        
        
        return movieInfo;
    }
    
    /*
     * extracts the movie basic info into the movieInfo structure.  
     * returns false, if a matching movie could not be found. else true
     */
    private boolean trySetMovieBasicInfo(String movieName, Map<String,Object> movieInfo) throws IllegalArgumentException, IOException
    {
        String normalizedName = movieName.toLowerCase().trim();
        String searchString;
        boolean setMovieInfo = false;
        
        try
        {
            searchString = URLEncoder.encode(normalizedName, "UTF-8");
        }
        catch(UnsupportedEncodingException e)
        {
            Logger.logException("Exception while encoding movie name " + normalizedName, e);
            throw new IllegalArgumentException("Unencodable movie name");
        }
        
        String movieSearchRelativeUrl = String.format("%s/%s", MOVIE_SEARCH_PATH, searchString);        
                
        Document doc;
        try
        {
            doc = this.cisiServerConnection.getResponse(movieSearchRelativeUrl).parse();
        }
        catch(IOException e)
        {
            Logger.logException("IOException when getting the response. url = " + movieSearchRelativeUrl, e);
            throw e;
        }
        
        Element matchResult = this.getMatchingSearchResult(doc, normalizedName);
        if(matchResult != null)
        {
            this.parseMovieBasicInfo(matchResult, movieInfo);
            setMovieInfo = true;
        }
        
        return setMovieInfo;
    }
    
    private Element getMatchingSearchResult(Document doc, String movieName)
    {
        Elements searchResults = doc.select("div.search-result");
        Element matchResult = null;
        
        for(Element result : searchResults)
        {
            // get movie title from result
            String title = result.attr("data1");
            if(title.toLowerCase().trim().equals(movieName))
            {
                matchResult = result;
                break;
            }
        }
        
        return matchResult;
    }
    
    private void parseMovieBasicInfo(Element searchResult, Map<String,Object> movieInfo)
    {
        movieInfo.put(MOVIE_ATTRIB_ID, searchResult.attr("rel"));
        movieInfo.put(MOVIE_ATTRIB_TITLE, searchResult.attr("data1"));
    }
    
    /*
     * extracts the movie watch options (for the various media types) into the movieInfo structure.      
     */    
    private void setMovieWatchOptions(String movieId, Map<String,Object> movieInfo)
    {
        for(String mediaType : mediaTypes)
        {
            long retryCnt = 0;
            while(true)
            {
                try
                {
                    Map<String, Object> watchOptions = this.getMovieWatchOptionsForMediaType(movieId, mediaType);
                    if(watchOptions != null)
                    {
                        movieInfo.put(mediaType, watchOptions);
                    }
                }
                catch(IOException e)
                {
                    Logger.logException(
                            String.format("Exception while getting %s options for movieId = %s", mediaType, movieId),
                            e);
                    Logger.logError(String.format("retrycnt = %d", ++retryCnt));  
                    if(retryCnt <= MAX_RETRY_CNT)
                    {
                        continue;
                    }
                    else
                    {
                        Logger.logError(String.format("Max retry cnt reaced. Aborting get watch options for movieid %s mediatype %s", mediaType, movieId));
                    }                    
                }
                break;
            }            
        }
    }
    
    private Map<String,Object> getMovieWatchOptionsForMediaType(String movieId, String mediaType) throws IOException
    {
        Map<String, Object> watchOptions = null;
        
        // build watchoptionsurl and get response from server
        String watchOptionsRelativeUrl = String.format("%s?movieId=%s&attributes=1&mediaType=%s", SERVICES_QUERY_PATH, movieId, mediaType);
        String responseBody;
        try
        {
            responseBody = this.cisiServerConnection.getResponse(watchOptionsRelativeUrl).body();
        }
        catch(IOException e)
        {
            Logger.logException("IOException when getting the response. url = " + watchOptionsRelativeUrl, e);
            throw e;
        }        
        
        // parse the json response into a Map object
        ObjectMapper mapper = new ObjectMapper();        
        // TO DO: need a better way to identify the top level json node is an array than allowing an exception to be thrown
        try
        {
            watchOptions = mapper.readValue(responseBody, Map.class);
        }
        catch(JsonMappingException e)
        {
            
        }
        return watchOptions;
    }
     
    public String[] getMediaTypes()
    {
        return this.mediaTypes;
    }
}
