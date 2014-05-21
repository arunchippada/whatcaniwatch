/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DBPediaIngestionService;

import java.io.*;
import java.util.*;
import org.openrdf.OpenRDFException;
import org.openrdf.query.resultio.*;
import org.openrdf.repository.*;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.rio.*;
import org.openrdf.rio.helpers.*;
import org.openrdf.sail.nativerdf.config.NativeStoreConfig;

/**
 *
 * @author Administrator
 */
public class LocalConnectionFactory
{  
    private static LocalConnectionFactory instance = null;
    private Hashtable<String,LocalRepositoryManager> managers = new Hashtable<String,LocalRepositoryManager>();
    
    private LocalConnectionFactory()
    {        
    }
    
    public static LocalConnectionFactory getInstance()
    {
        if(instance == null)
        {
            instance = new LocalConnectionFactory();
        }
        return instance;
    } 
    
    /*
     * gets connection to local repository under the given repositoryRootFolder
     * creates a new repository if not existing
     */
    public RepositoryConnection getConnection(String repositoryRootFolder, String repositoryID) throws OpenRDFException
    {
        LocalRepositoryManager repositoryManager = null;
        
        if(managers.containsKey(repositoryRootFolder))
        {
            repositoryManager = managers.get(repositoryRootFolder);
        }
        else
        {
            repositoryManager = new LocalRepositoryManager(new File(repositoryRootFolder));
            repositoryManager.initialize();
            managers.put(repositoryRootFolder, repositoryManager);
        }
            
        return this.getConnection(repositoryManager, repositoryID);
    }
    
   /* 
     * get connection to local Repository (create new if not existing)    
     */    
    // TODO: Move to common project in a RepositoryUtil class
    private RepositoryConnection getConnection(LocalRepositoryManager repositoryManager, String repositoryID) throws OpenRDFException
    {    
        // Check existing and create new if not existing
        if(!repositoryManager.hasRepositoryConfig(repositoryID))
        {
            SailRepositoryConfig sailConfig = new SailRepositoryConfig( new NativeStoreConfig());                   
            repositoryManager.addRepositoryConfig(new RepositoryConfig(repositoryID, sailConfig));                
        }

        Repository repository = repositoryManager.getRepository(repositoryID);
        repository.initialize();                     
        RepositoryConnection connection = repository.getConnection();

        ParserConfig parserConfig = new ParserConfig();
        parserConfig.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false);
        parserConfig.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false);
        parserConfig.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);
        parserConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);

        connection.setParserConfig(parserConfig);
        
        return connection;
    }  
 }
