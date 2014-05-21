/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RepositoryConsoleUtil;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryConnection;
import java.io.*;
import org.openrdf.repository.util.*;
import org.openrdf.rio.helpers.BasicParserSettings;
import org.openrdf.rio.*;

// TODO: Move DumpUtil to Common
/**
 *
 * @author arunch
 * Process triples from a dump file by performing the specified operation
 * (adding or removing triples) on a repository
 */
public class DumpUtil
{
    private File dumpFile;
    
    public DumpUtil(String dumpFileName)
    {
        this.dumpFile = new File(dumpFileName);
    }
    
    // adds triples from dumpfile to repository
    public void addTriplesToRepository(RepositoryConnection con) throws OpenRDFException, IOException
    {
        ChunkCommitter chunkCommitter = new ChunkCommitter(con, new RDFInserter(con));
        this.processDumpFile(chunkCommitter);
    }

    // removes triples specified in the dumpfile from repository
    public void removeTriplesFromRepository(RepositoryConnection con) throws OpenRDFException, IOException
    {
        ChunkCommitter chunkCommitter = new ChunkCommitter(con, new RDFRemover(con));
        this.processDumpFile(chunkCommitter);
    }
    
    // processes the dump file by invoking the handler for each statement
    private void processDumpFile(RDFHandler handler) throws OpenRDFException, IOException
    {                
        FileInputStream is = new FileInputStream(this.dumpFile);

        RDFParser parser = Rio.createParser(RDFFormat.NTRIPLES);

        ParserConfig parserConfig = new ParserConfig();
        parserConfig.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false);
        parserConfig.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false);
        parserConfig.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);
        parserConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
        parser.setParserConfig(parserConfig);

        // add our own custom RDFHandler to the parser. This handler takes care of adding
        // triples to our repository and doing intermittent commits
        parser.setRDFHandler(handler);

        parser.parse(is, "file://" + this.dumpFile.getCanonicalPath());                   
    }    
}
