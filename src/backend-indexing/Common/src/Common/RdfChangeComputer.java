/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Common;

import java.util.*;
import org.openrdf.model.*;
import org.openrdf.model.impl.*;

/**
 *
 * @author Administrator
 */
public class RdfChangeComputer
{      
    private Collection<Statement> oldStatements;
    private Collection<Statement> newStatements;
    
    public RdfChangeComputer(Collection<Statement> oldStatements, Collection<Statement> newStatements)
    {        
        this.oldStatements = oldStatements;
        this.newStatements = newStatements;
    }
        
    /*
     * returns the statements present in the newStatements but not in old
     */
    public Model getAddedStatements()
    {
        Model model = new LinkedHashModel();
        
        for(Statement stmt : this.newStatements)
        {
            if(!this.oldStatements.contains(stmt))
            {
                model.add(stmt);
            }
        }
        
        return model;
    }

    /*
     * returns the statements present in the oldStatements but not in new
     */
    public Model getRemovedStatements()
    {
        Model model = new LinkedHashModel();
        
        for(Statement stmt : this.oldStatements)
        {
            if(!this.newStatements.contains(stmt))
            {
                model.add(stmt);
            }
        }
        
        return model;
    }    
 }
