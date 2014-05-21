/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RepositoryConsoleUtil;

// TODO: RepositoryPublisher in common can be renamed to RepositoryServicer
// and extended to include the operations supported by the RepositoryConsoleUtil
// RepositoryConsoleUtil will then simply call into the RepositoryServicer lib object for its operations
import Common.*;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryConnection;

/**
 *
 * @author Administrator
 */
public class RepositoryConsoleUtil
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        System.out.println("RepositoryConsoleUtil started");

        String operation = args[0];
        String repoDirName = args[1];
        String repoName = args[2];
        String dumpFileName = args[3];

        System.out.println("Operation = " + operation);         
        System.out.println("Repository Dir = " + repoDirName); 
        System.out.println("Repository Name = " + repoName);         
        System.out.println("Dump file " + dumpFileName);  
        
        RepositoryConnection connection = null;
        try
        {
            connection = getLocalRepositoryConnection(repoDirName, repoName);
        }
        catch(Exception e)
        {
            System.out.println("Exception while opening repostory connection. Exiting application");
            System.exit(1);
        }               
        
        System.out.println(String.format("Performing operation %s",operation));
                
        DumpUtil dumpUtil = new DumpUtil(dumpFileName);
        try
        {
            System.out.println(String.format("Triples in repository before %s = %d", operation, connection.size()));
            if(operation.equals("addTriples"))
            {
                dumpUtil.addTriplesToRepository(connection);                
            }
            else if(operation.equals("removeTriples"))
            {
                dumpUtil.removeTriplesFromRepository(connection);
            }
            System.out.println(String.format("Triples in repository after %s = %d", operation, connection.size()));
        }
        catch(Exception e)
        {
            System.out.println(String.format("Exception while performing operation %s",operation));
        }
        
        System.out.println("RepositoryConsoleUtil completed");                
    }    
    
    private static RepositoryConnection getLocalRepositoryConnection(String repositoryRootFolder, String repositoryID) throws OpenRDFException
    {        
        try
        {              
            return LocalConnectionFactory.getInstance().getConnection(repositoryRootFolder, repositoryID);
        }
        catch(OpenRDFException e)
        {
            Logger.logMessage("Exception while getting connection to repository " + repositoryID, "getLocalRepositoryConnection");
            throw e;
        }        
    }

}
