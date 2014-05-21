<?php

require_once 'FacebookHelper.php';
require_once 'MovieWatchInfoService.php';

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Provides information on a user's movie lists
 *
 * @author Administrator
 */
class UserMoviesService 
{    
    // movie property name constants
    const MOVIE_URI = "uri";        
    const MOVIE_TITLE = "title";
    const MOVIE_FBID = "fbid";    
    const MOVIE_FBURL = "fburl";        
    const MEDIATYPE_STREAMING = "streaming";
    const MEDIATYPE_RENTAL = "rental";
    const MEDIATYPE_PURCHASE = "purchase";
    const MEDIATYPE_DVD = "dvd";    
    const MEDIATYPE_XFINITY = "xfinity";
    
    /**
    * The object representing facebook (the user's facebook)
    */
    private $fbHelper;
    private $watchInfoService;

    /**
    * constructor
    */
    public function __construct($fb) 
    {
        $this->fbHelper = new FacebookHelper($fb);
        $this->watchInfoService = new MovieWatchInfoService();
    }

    /**
    * returns the user's want to watch movies
    */
    public function getWantToWatchMovies() 
    {
        // get want to watch movies from FB
        $fbWantToWatchList = $this->fbHelper->getWantToWatchMovies();   

        // if there are no movies in fbWantToWatchList, then we are going to return an empty wantToWatchMovies list
        if (count($fbWantToWatchList) > 0)
        {   
            // process (transform) movie objects in fbWantToWatchList, to clean, normalize, 
            // add more properties
            $fbMovies = $this->processFBMovies($fbWantToWatchList);
            
            // get the movie objects (with watch options) from the MovieWatchInfoService,
            // passing in the fbMovies as search critiera
            // ideally, the criteria should only include the title and year properties of the movie
            // instead of sending down all the properties (fbid etc) which are not understood by the MovieWatchInfoService
            // However, this will need creating another array
            $watchInfoMovies = $this->watchInfoService->search($fbMovies); 
            return $this->mergeMovies($fbMovies, $watchInfoMovies);
        }
        
        return array();
    }  

    /*
     * transforms the fbMovies, to clean/normalize such as separating the title and year
     * and adding new properties, such as FBUrl, Image
     */
    private function processFBMovies($fbMovies)
    { 
        return array_map(array($this,'processFBMovie'), $fbMovies);
    }
    
    private function processFBMovie($fbMovie)
    {
        // TODO: extract year from title and add image
        return array(
            self::MOVIE_TITLE => $fbMovie['title'],
            self::MOVIE_FBID => $fbMovie['id'],
            self::MOVIE_FBURL => $fbMovie['url']
        );
    }

    /*
    * Merges the movies from the two collections (fbmovies and watchInfoMovies) and 
    * returns the merged collection of movies
    * A join (hash join) is done between the two collections using the title and year as the join key
    */
    private function mergeMovies($fbMovies, $watchInfoMovies)
    {      
        $mergedMovies = array();
        
        // pivot watchInfoMovies by the join key (title and year)
        $watchInfoMoviesDictionary = $this->prepareWatchInfoMoviesForJoin($watchInfoMovies);
        
        // walk through fbMovies in order and for each fbMovie, get matching movies 
        // from watchInfoMoviesDictionary
        // if number of matches is 0, then add the fbMovie to mergedMovies
        // if number of matches is 1, then merge the two movies and add the mergedMovie to mergedMovies
        // if number of matches is >1, then add the matches to mergedMovies
        foreach($fbMovies as $fbMovie)
        {
            $matches = $this->getMatches($fbMovie, $watchInfoMoviesDictionary);
            $cnt = count($matches);
            if ($cnt == 0)
            {
                $mergedMovies[] = $fbMovie;
            }
            elseif ($cnt == 1) 
            {
                $mergedMovies[] = $this->merge($fbMovie, $matches[0]);
            }
            else
            {
                $mergedMovies = array_merge($mergedMovies, $matches);
            }            
        }
        return $mergedMovies;
    }
    
    /*
     * pivots the watchInfoMovies by the join key (title and year)
     */
    private function prepareWatchInfoMoviesForJoin($watchInfoMovies)
    {
      $watchInfoMoviesDictionary = array();
      
      // TODO: pivot by year within the entry
      foreach($watchInfoMovies as $movie)
      {
          $title = $movie[self::MOVIE_TITLE];
          // create array entry for this title in $watchInfoMoviesDictionary, if not existing
          if(!array_key_exists($title, $watchInfoMoviesDictionary))
          {
              $watchInfoMoviesDictionary[$title] = array();
          }
          $entry = &$watchInfoMoviesDictionary[$title];
          $entry[] = $movie;
      }
        
      return $watchInfoMoviesDictionary;
    }   
    
    /* 
     * returns the matching movies (by the join key) from the watchInfoMovies
     * for the given fbMovie
     */
    private function getMatches($fbMovie, $watchInfoMoviesDictionary)
    {
        $matches = array();
        
        // TODO: If fbMovie has both title and year, do lookup using both title and year
        $title = $fbMovie[self::MOVIE_TITLE];
        if(array_key_exists($title, $watchInfoMoviesDictionary))
        {
            $matches = $watchInfoMoviesDictionary[$title];
        }        
        
        return $matches;        
    }
    
    private function merge($fbMovie, $watchInfoMovie)
    {
        return array_merge($fbMovie,$watchInfoMovie);
    }
}

?>
