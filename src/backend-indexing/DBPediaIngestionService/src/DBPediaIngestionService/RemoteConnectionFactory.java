/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DBPediaIngestionService;

import org.openrdf.OpenRDFException;
import org.openrdf.query.resultio.*;
import org.openrdf.repository.*;
import org.openrdf.repository.http.*;
import org.openrdf.rio.*;
import org.openrdf.rio.helpers.*;

/**
 *
 * @author Administrator
 */
public class RemoteConnectionFactory
{  
    private static RemoteConnectionFactory instance = null;
    
    private RemoteConnectionFactory()
    {        
    }
    
    public static RemoteConnectionFactory getInstance()
    {
        if(instance == null)
        {
            instance = new RemoteConnectionFactory();
        }
        return instance;
    } 
    
    public RepositoryConnection getConnection(String repositoryUrl) throws OpenRDFException
    {
        HTTPRepository repository = new HTTPRepository(repositoryUrl);
        this.initializeRepository(repository);
        return this.getConnection(repository);
    }

    public RepositoryConnection getConnection(String serverUrl, String repositoryID) throws OpenRDFException
    {
        HTTPRepository repository = new HTTPRepository(serverUrl, repositoryID);
        this.initializeRepository(repository);
        return this.getConnection(repository);
    }
    
    private void initializeRepository(HTTPRepository repository) throws OpenRDFException
    {
        repository.initialize();
        repository.setPreferredRDFFormat(RDFFormat.NTRIPLES);
        repository.setPreferredTupleQueryResultFormat(TupleQueryResultFormat.CSV);
    }
    
    private RepositoryConnection getConnection(HTTPRepository repository) throws OpenRDFException
    {                
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
