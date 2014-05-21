<?php

require_once 'fb-sdk/facebook.php';

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Helper with utility methods for working with facebook
 *
 * @author arunch
 */
class FacebookHelper
{
    /*
     * movies above recent 500 in wanttowatch are assumed to have already been watched
     * any movies in recent 500 of wanttowatch that have moved to watches, are assumed 
     * to be within recent 1000 of watches. This can be cross checked by looking at the 
     * oldest timestamp in the watches. It must be less than the oldest timestamp in wantstowatch
     */
    const WATCHED_AND_WANTTOWATCH_POSTREQUESTBODY = '[
	{"method":"GET", "relative_url":"me/video.watches?fields=publish_time,data&limit=1000"},
	{"method":"GET", "relative_url":"me/video.wants_to_watch?fields=publish_time,data&limit=500"}
    ]'; 

    /**
    * The object representing facebook
    */
    private $fb;

    /**
    * constructor
    */
    public function __construct($fb) 
    {
        $this->fb = $fb;
    }

    /**
    * Get want to watch movies for the user, based on actions listed in video.wantstowatch
    * and video.watches
    * video.wants_to_watch returns actions for movies,
    * even if some of these movies may also have actions in video.watches
    * So we get both the watches and wantstowatch actions and remove any movies that are in both lists, 
    * to get the actual wantstowatch movies
    * @return array - want to watch movies
    */
    public function getWantToWatchMovies() 
    {      
      $watchedAndWantToWatch = $this->getWatchedAndWantToWatchActions();                         
      return $this->extractWantToWatchMovies($watchedAndWantToWatch);;
    }
    
    /*
     * returns a json object containing the two separate lists for 
     * the watched and wanttowatch actions 
     */
    private function getWatchedAndWantToWatchActions()
    {
      $fb = $this->fb; 

      $result = $fb->api(
              '?include_headers=false', 
              'POST', 
              array('batch' => self::WATCHED_AND_WANTTOWATCH_POSTREQUESTBODY));
                        
      return $result;               
    }    

    /*
    * returns "actual" wanttowatch movies as an array
    * extracts the movie object from each action in wantToWatchActions and checks
    * if this movie object is present in the watched list. If the movie is only present
    * in the wanttowatch actions, includes this movie in the return list
    */
    private function extractWantToWatchMovies($watchedAndWantToWatchActions)
    {
      $wantToWatchMovies = array();
      
      $watchedActionsJsonString = $watchedAndWantToWatchActions[0]['body'];
      $wantToWatchActionsJsonString = $watchedAndWantToWatchActions[1]['body'];      
      
      $watchedActions = json_decode($watchedActionsJsonString, true);
      $wantToWatchActions = json_decode($wantToWatchActionsJsonString, true);
      
      // get MovieIdMap for movies in watched actions
      $watchedMovieIdMap = $this->getMovieIdMap($watchedActions);
      
      foreach($wantToWatchActions['data'] as $action)
      {
          $movie = $this->getMovie($action);
          
          // only add this movie to return list if it does not exist in the watched list
          if(!array_key_exists($movie['id'], $watchedMovieIdMap))
          {          
            // only accept a movie with a title
            if(!empty($movie['title']))
            {
                  $wantToWatchMovies[] = $movie;
            }
          }
      }

      return $wantToWatchMovies;      
    }
    
    /*
     * returns a map of MovieIds of movies that are extracted from the actions
     */
    private function getMovieIdMap($actions)
    {
      $movieIdMap = array();
      
      foreach($actions['data'] as $action)
      {
          $movie = $this->getMovie($action);
          $movieIdMap[$movie['id']] = true;
      }
        
      return $movieIdMap;
    }
    
    /*
     * returns the Movie object extracted from the provided action
     */
    private function getMovie($action)
    {
       return $action['data']['movie'];
    }
}

?>
