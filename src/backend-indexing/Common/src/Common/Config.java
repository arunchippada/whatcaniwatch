/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Common;

import java.io.*;
import java.util.*;

/**
 *
 * @author Administrator
 */
public class Config
{  
    private static Config instance = null;
        
    public static Config getInstance()
    {
        if(instance == null)
        {
            instance = new Config();
        }
        return instance;
    } 

    private Properties properties = new Properties();
    
    private Config()
    {
        try
        {
            FileInputStream in = new FileInputStream("config.properties");
            this.properties.load(in);
            in.close();
        }
        catch(IOException e)
        {
            System.out.println("Exception while reading properties" + e.toString());
        }
    }
    
    public String getProperty(String key)
    {
        return this.properties.getProperty(key);
    }
 }
