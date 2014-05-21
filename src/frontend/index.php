<?php

header('Content-Type: text/html;charset=utf-8');

require_once 'config.php';
require_once 'fb-sdk/facebook.php';
require_once 'UserMoviesService.php';

// Our Facebook Application settings 
$facebook = new Facebook(array(
  'appId'  => FACEBOOK_APPID,
  'secret' => FACEBOOK_APPSECRET,
));

// Get User ID
$user = $facebook->getUser();

// We may or may not have this data based on whether the user is logged in.
//
// If we have a $user id here, it means we know the user is logged into
// Facebook, but we don't know if the access token is valid. An access
// token is invalid if the user logged out of Facebook.

if ($user) 
{
  try 
  {
      //Proceed assuming we have a logged in user who's authenticated.
      $user_profile = $facebook->api('/me');       
  } 
  catch (FacebookApiException $e) 
  {
    error_log($e);
    $user = null;
  }
}

// The actual user state has now been determined
if ($user) 
{
    // logout url
    $logoutUrl = $facebook->getLogoutUrl();
    
    $userHomePageLink = $user_profile['link'];
    $userMoviesPageLink = $userHomePageLink . "/video_movies_want";

    // get user's want to watch movies
    $userMoviesService = new UserMoviesService($facebook); 
    $movies = $userMoviesService->getWantToWatchMovies();       
    $movieCnt = count($movies);
} 
else 
{
    // login url
    $params = array('scope' => 'user_actions.video');
    $loginUrl = $facebook->getLoginUrl($params);
}

?>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta content="text/html; charset=UTF-8" http-equiv="Content-Type">
    <title>What Can I Watch</title>
    <link href="css/site.css" type="text/css" rel="stylesheet">
</head>

<body>
<div id="wrapper">
	<div id="header-wrapper">
		<div id="header">
			<div id="header-inner"></div>
		</div>
	</div>				
	
	<div id="menu">
		<ul>
		<li><a dir="ltr" href="index.php">Home</a></li>
		<li><a dir="ltr" href="privacy.html">Privacy</a></li>
		<li><a dir="ltr" href="contact.html">Contact</a></li>
		</ul>
		<div class="clear"></div>
	</div>

   	<div id="main_container">
   	    <div class="right_column">
	    <b><u>Idea</u></b>

            <p>How many times have you been in a situation where you want to watch a movie, but first, you are faced with the task of picking a movie that is available and would also match your interests. You spend some time browsing through your netflix 
            instant queue, but none of those movies seem like what you want to watch now, since they were never your first choice to begin with. You spend some more time browsing through the netflix catalog. Not satisfied with what you found so far, 
            you hop onto youtube and start searching there. Often, the experience of searching for the right movie can be frustrating and takes up a lot of time. </p>
            <p>Wouldn't it be nice if you are allowed a watchlist of movies that is not restricted by availability and when it comes time to watch a movie, you have a dashboard that tells you which of these movies are available and where. WhatCanIWatch does 
            just that, by showing you the availability and the watch options for the list of movies on your facebook WantToWatch list.</p>

            <b><u>How it works</u></b>
            <p>Facebook has a feature that allows you to maintain your favorite movies in two lists - &quot;Watched&quot; and &quot;Want To Watch&quot;. In the &quot;Want To Watch&quot; list, you can maintain a list of your favorite movies that you would like 
            to watch.</p>
	    <p>WhatCanIWatch service gets access to your Facebook &quot;Want To Watch&quot; list of movies and checks the availability options for these movies across various media networks such as Netflix, Amazon video, YouTube, Apple Itunes, Redbox, Crackle 
            etc and shows you a dashboard of the availability options for your favorite movies.</p>
            <p>Currently, WhatCanIWatch service uses another free service called CanIStreamIt to check the availabilty options for the movies across the various media networks.</p>
                
   	    </div><!-- RIGHT COLUMN -->
       	<div class="left_column">
<!-- MAIN CONTENT BEGINS HERE -->

	    <?php if ($user){ ?>        
        Welcome <a href="<?php echo $userHomePageLink; ?>"> <?php echo $user_profile['name']; ?> </a>
        <img src="https://graph.facebook.com/<?php echo $user; ?>/picture">
        <br/>
        <a href="<?php echo $logoutUrl; ?>">Logout</a>
        <br/>
        <br/>
        <?php if ($movieCnt > 0) { ?> 
            Showing <?php echo $movieCnt; ?> results for movies in your <a href="<?php echo $userMoviesPageLink; ?>">Facebook watch list</a>
            <table>
                <tr><th>Title</th><th>Year</th><th>Streaming Free/Subscription</th><th>Streaming Rental</th><th>Digital Purchase</th><th>DVD/BluRay</th><th>Xfinity</th></tr>
                <?php foreach($movies as $m) { ?>
                    <tr>
                        <td> <?php echo $m[UserMoviesService::MOVIE_TITLE] ?> </td>
                        <td> <?php echo '' ?> </td>
                        <td>
                            <?php 
                                if(array_key_exists(UserMoviesService::MEDIATYPE_STREAMING, $m))
                                {
                                    foreach($m[UserMoviesService::MEDIATYPE_STREAMING] as $provider) 
                                    { 
                                        printf("<span title='%s' class='picon %s'></span>", $provider, $provider);
                                    }
                                }
                            ?>
                        </td>
                        <td>
                            <?php 
                                if(array_key_exists(UserMoviesService::MEDIATYPE_RENTAL, $m))
                                {
                                    foreach($m[UserMoviesService::MEDIATYPE_RENTAL] as $provider) 
                                    { 
                                        printf("<span title='%s' class='picon %s'></span>", $provider, $provider);
                                    }
                                }
                            ?>
                        </td>
                        <td>
                            <?php 
                                if(array_key_exists(UserMoviesService::MEDIATYPE_PURCHASE, $m))
                                {
                                    foreach($m[UserMoviesService::MEDIATYPE_PURCHASE] as $provider) 
                                    { 
                                        printf("<span title='%s' class='picon %s'></span>", $provider, $provider);
                                    }
                                }
                            ?>
                        </td>
                        <td>
                            <?php 
                                if(array_key_exists(UserMoviesService::MEDIATYPE_DVD, $m))
                                {
                                    foreach($m[UserMoviesService::MEDIATYPE_DVD] as $provider) 
                                    { 
                                        printf("<span title='%s' class='picon %s'></span>", $provider, $provider);
                                    }
                                }
                            ?>
                        </td>
                        <td>
                            <?php 
                                if(array_key_exists(UserMoviesService::MEDIATYPE_XFINITY, $m))
                                {
                                    foreach($m[UserMoviesService::MEDIATYPE_XFINITY] as $provider) 
                                    { 
                                        printf("<span title='%s' class='picon %s'></span>", $provider, $provider);
                                    }
                                }
                            ?>
                        </td>                        
                    </tr>
                <?php } ?>                
            </table>

        <?php } else { ?>            
            No movies in your <a href="<?php echo $userMoviesPageLink; ?>">Facebook watch list</a>. 
            <br/>
            <br/>                        
            Check back here after adding movies in your <a href="<?php echo $userMoviesPageLink; ?>">Facebook watch list</a>. 
            <br/>
            <b>NOTE : </b>When adding movies to your watch list, you can use the appropriate privacy setting to control how Facebook shares this information with your friends
        <?php } ?>

    <?php } else { ?>                    
      You are not Connected.
      <br/>
      <br/>      
      <a href="<?php echo $loginUrl; ?>">Login with your Facebook account</a>      
    <?php } ?>

    <?php // print_r($user_profile); ?>


<!-- MAIN CONTENT ENDS -->        </div><!-- LEFT COLUMN -->
    </div><!-- MAIN CONTAINER -->
</div><!-- WRAPPER -->
</body>
</html>
