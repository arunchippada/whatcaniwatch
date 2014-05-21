/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RepositoryConsoleUtil;

import Common.*;
import java.util.Date;
import org.openrdf.repository.*;
import org.openrdf.model.*;
import org.openrdf.rio.*;
import org.openrdf.repository.util.*;

// TODO: Move ChunkCommitter to Common
/**
 *
 * @author arunch
 * Handles rdf statements using underlying rdfhandler, 
 * intermittently committing the changes 
 * 
 */
public class ChunkCommitter implements RDFHandler 
{         
        // underlying rdfHandler
        private RDFHandler handler;
        private RepositoryConnection conn;
         
        private long count = 0L;
 
        // do intermittent commit every 50,000 triples
        private long chunksize = 50000L;
         
        public ChunkCommitter(RepositoryConnection conn, RDFHandler handler) 
        {
            this.handler = handler;
            this.conn = conn;
        }
 
        @Override
        public void startRDF() throws RDFHandlerException 
        {
            try 
            {
                 this.conn.begin();
                 this.handler.startRDF();
            } 
            catch (RepositoryException e) 
            {
                 throw new RDFHandlerException(e);
            }
        }
 
        @Override
        public void endRDF() throws RDFHandlerException 
        {
            try 
            {
                 this.conn.commit();
                 this.handler.endRDF();                 
            } 
            catch (RepositoryException e) 
            {
                 throw new RDFHandlerException(e);
            }            
        }
 
        @Override
        public void handleNamespace(String prefix, String uri)
                throws RDFHandlerException 
        {
            this.handler.handleNamespace(prefix, uri);
        }
 
        @Override
        public void handleStatement(Statement st) throws RDFHandlerException 
        {
            this.handler.handleStatement(st);
            count++;
            // do an intermittent commit whenever the number of triples
            // has reached a multiple of the chunk size
            if (count % chunksize == 0) 
            { 
                try 
                {
                    this.conn.commit();
                    Logger.logMessage(String.format("committed %d triples. current count = %d", chunksize, count), "ChunkCommitter");
                    
                    this.conn.begin();
                } 
                catch (RepositoryException e) 
                {
                    throw new RDFHandlerException(e);
                }
            }
        }
 
        @Override
        public void handleComment(String comment) throws RDFHandlerException 
        {
            this.handler.handleComment(comment);
        }        
}
