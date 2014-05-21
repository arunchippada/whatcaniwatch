/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DBPediaIngestionService;

import java.io.*;
import java.util.Date;

/**
 *
 * @author Administrator
 */
public class Logger
{
    public static File errorFile = new File("errors.log");
    
    public static void logMessage(String m, String component)
    {
        System.out.println(
                String.format("%s %s: %s", 
                new Date().toString(),
                component,
                m));
    }    
    
    public static void logError(String m)
    {
        try
        {
            PrintStream out = new PrintStream(new FileOutputStream(errorFile, true));
            out.println(new Date().toString() + " " + m);
        }
        catch(IOException e)
        {
            System.out.println("Exception while logging error " + e.toString());
        }
    }            
    
    public static void logException(String m, Exception ex)
    {        
        try
        {
            PrintStream out = new PrintStream(new FileOutputStream(errorFile, true));
            out.println(
                    String.format(
                    "%s %s %s", 
                    new Date().toString(),
                    m, 
                    ex.toString()
                    ));
            
            ex.printStackTrace(out);                    
        }
        catch(IOException e)
        {
            System.out.println("Exception while logging error " + e.toString());
        }
    }
}
