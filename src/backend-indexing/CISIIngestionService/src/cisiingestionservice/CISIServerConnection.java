/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cisiingestionservice;

import java.io.*;
import org.jsoup.*;

/**
 *
 * @author Administrator
 */
public class CISIServerConnection
{  
    private static final String CISI_URI_ROOT = "http://www.canistream.it";
    
    /*
     * returns the response from the request made to the server for the provided relative url
     */
    public Connection.Response getResponse(String relativeUrl) throws IOException
    {
        String url = String.format("%s/%s", CISI_URI_ROOT, relativeUrl);
        return Jsoup.connect(url)
                    .header("Accept","*/*")
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:5.0) Gecko/20100101 Firefox/5.0")
                    .timeout(10000)
                    .ignoreContentType(true)
                    .execute();
    }    
 }
