/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DBPediaIngestionService;

/**
 *
 * @author arunch
 */
public class DBPediaIngestionService 
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        System.out.println("DBPedia ingestion started");
                
        try
        {
            DBPediaLiveDataExtractor dbpediaExtractor = new DBPediaLiveDataExtractor();            
            dbpediaExtractor.extractData();
        }
        catch(Exception e)
        {
            System.out.println(String.format("Exception while ingesting data from dbpedia %s", e.toString()));
        }
        
        System.out.println("DBPedia ingestion completed");                
    }
}
