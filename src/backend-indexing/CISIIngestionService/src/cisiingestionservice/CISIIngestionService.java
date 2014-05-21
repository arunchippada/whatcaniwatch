/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cisiingestionservice;

import Common.*;

/**
 *
 * @author Administrator
 */
public class CISIIngestionService
{        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        System.out.println("CISI ingestion started");

        try
        {
            CISIMovieDataExtractor cisiMovieDataExtractor = new CISIMovieDataExtractor();            
            cisiMovieDataExtractor.extractData();
        }
        catch(Exception e)
        {
            System.out.println(String.format("Exception while ingesting data %s", e.toString()));
        }        
        
        System.out.println("CISI ingestion completed");                        
    }
    
}
