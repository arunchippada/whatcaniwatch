/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Common;

import java.io.*;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.*;
import org.openrdf.rio.ntriples.*;

/**
 *
 * @author Administrator
 */
public class RepositoryPublisher
{                  
    /*
     * publishes the statements in the repository to a file in ntriples format
     */
    public void publish(RepositoryConnection connection, String fileName) throws IOException, OpenRDFException
    {
        FileOutputStream out = new FileOutputStream(fileName, false);
        NTriplesWriter writer = new NTriplesWriter(out);
        
        connection.export(writer);
    }
 }
