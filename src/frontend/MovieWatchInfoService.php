<?php

require_once 'config.php';
require_once 'php-sesame/phpSesame.php';

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Provides information on watch options for movies
 *
 * @author Administrator
 */
class MovieWatchInfoService 
{
    const REPOSITORY_ENDPOINT_URL = REPOSITORY_ENDPOINT_URL;
    const MOVIES_REPOSITORYID = 'movies-global';
    
    private $store;

    /**
    * constructor
    */
    public function __construct() 
    {
        $this->store = new phpSesame(self::REPOSITORY_ENDPOINT_URL, self::MOVIES_REPOSITORYID);
    }

    /**
    * returns a list of movie objects that match the provided search criteria
    * The criteria is expected to be a list of movie names, with an optional year
    * 
    * TODO: Format the movie names to be lower case and stripped of the year
    * @return array of movie objects mapped to thier uris
    * The attributes in each movie object consist of movie title, release year and watch options
    * If multiple movies match an input movie name, then all of the matches are returned
    */
    public function search($criteria) 
    {
        $searchResult = array();        
        // Validate that there is atleast one movie (with movie title)
        if(count($criteria)>0)
        {
            $query = $this->prepareRepositoryQuery($criteria);
            $result = $this->store->query($query, phpSesame::SPARQL_XML, "sparql", TRUE);

            $searchResult = $this->prepareSearchResult($result);
        }
        return $searchResult;
    }  

    private function prepareRepositoryQuery($criteria)
    {      
        $query = '
          PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
          PREFIX dbo: <http://dbpedia.org/ontology/>
          PREFIX foaf: <http://xmlns.com/foaf/0.1/>
          PREFIX cisi: <http://www.canistream.it/>
          SELECT distinct ?uri (str(?l) as ?title) ?watchoption
          WHERE
          {
            ?uri rdfs:label ?l.
            FILTER (langMatches(lang(?l), "en")).
            OPTIONAL {?uri cisi:watchoption ?watchoption.}
              ';

        $isFirst = TRUE;      
        foreach($criteria as $m)
        {
          if(!empty($m['title']))
          {
             if(!$isFirst)
             {
                 $query = $query . ' UNION ';                 
             }
             $query = sprintf('%s 
                    {?uri foaf:name "%s"@en.} UNION {?uri rdfs:label "%s"@en.}',
                     $query, $m['title'], $m['title']);
             $isFirst = FALSE;
          }
        }

        $query = $query . ' }';
        return $query;
    }

    /*
    * Prepare movies search result object from the query result
    */
    private function prepareSearchResult($result)
    {      
        $searchResult = array();
        
        foreach($result->getRows() as $row)
        {
            $uri = $row['uri'];

            // check to see if this uri was previously seen
            if(!array_key_exists($uri, $searchResult))
            {
                $searchResult[$uri] = array(
                    UserMoviesService::MOVIE_URI => $uri,
                    UserMoviesService::MOVIE_TITLE => $row['title']);
            }
            $movie = &$searchResult[$uri];
            
            // process the watch option value in this row.
            // if blank or "none", then there is no watch option available and we can skip processing the watch option value
            if(!empty($row['watchoption']) && !($row['watchoption'] == "none"))
            {       
                $watchOption = $row['watchoption'];                
                // extract the media type and provider from the watchoption
                list($mediaType, $provider) = explode("_", $watchOption, 2);
                
                // create the array for this media type, if not existing
                if(!array_key_exists($mediaType, $movie))
                {
                    $movie[$mediaType] = array();
                }
                $providers = &$movie[$mediaType];
                $providers[] = $provider;
            }            
        }
        
        return $searchResult;
    }

}

?>
