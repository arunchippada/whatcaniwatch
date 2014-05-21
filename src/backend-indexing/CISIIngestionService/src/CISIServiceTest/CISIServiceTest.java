/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CISIServiceTest;

import cisiingestionservice.CISIService;
    import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.util.*;
import java.io.*;

/**
 *
 * @author Administrator
 */
public class CISIServiceTest
{        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "8888");
        System.setProperty("https.proxyPort", "8888");
        
        CISIService cisiService = new CISIService();
        String movieName = "dead end drive-in";
        Map<String,Object> movieInfo = null;
        ObjectMapper mapper = new ObjectMapper();
        
        // get movie info
        try
        {
            movieInfo = cisiService.getMovieInfo(movieName);          
        }
        catch(Exception e)
        {
            System.out.println(String.format("Exception while getting movie info %s", e.toString()));
        }    
        if(movieInfo != null)
        {
            try
            {
                mapper.writeValue(System.out, movieInfo);
            }
            catch(Exception e1)
            {
                System.out.println(String.format("Exception while serializing movieinfo %s", e1.toString()));
            }
        }
    }
    
}
