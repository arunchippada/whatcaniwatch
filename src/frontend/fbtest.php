<?php

require 'fb-sdk/facebook.php';
require 'FacebookHelper.php';

// Create our Application instance 
$facebook = new Facebook(array(
  'appId'  => '343468779113013',
  'secret' => 'd85d8652b54d4d6658110dd6a9a752e1',
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
      
      $fbHelper = new FacebookHelper($facebook);      
      $movies = $fbHelper->getWantToWatchMovies();
  } 
  catch (FacebookApiException $e) 
  {
    error_log($e);
    $user = null;
  }
}

// Login or logout url will be needed depending on current user state.
if ($user) 
{
  $logoutUrl = $facebook->getLogoutUrl();
} 
else 
{
  $params = array('scope' => 'user_actions.video');
  $loginUrl = $facebook->getLoginUrl($params);
}

?>

<!doctype html>
<html>
  <head>
    <title>My Choice</title>
    <link rel="stylesheet" type="text/css" href="controls.css">
  </head>
  <body>

    <?php if ($user): ?>        
      
        Logged in as <a href="<?php echo $user_profile['link']; ?>"> <?php echo $user_profile['name']; ?> </a>
        <img src="https://graph.facebook.com/<?php echo $user; ?>/picture">
        <br/>
        <a href="<?php echo $logoutUrl; ?>">Logout</a>
        <br/>
        <br/>
        
        <?php $movieCnt = count($movies); if ($movieCnt > 0): ?> 
        
            <h3>Want to Watch (<?php echo $movieCnt ?>)</h3>
            <br/>
            <table>
                <tr><th>Title</th><th>Movie FB ID</th><th>Facebook Url</th></tr>
                <?php foreach($movies as $m) { ?>
                    <tr>
                        <td> <?php echo $m['title'] ?> </td>
                        <td> <?php echo $m['id'] ?> </td>
                        <td> <?php echo $m['url'] ?> </td>
                    </tr>
                <?php } ?>                
            </table>
        
        <?php else: ?>
            
            No movies in your "Want to watch" list. Add movies to your "Want to watch" list on Facebook.
        
        <?php endif ?>
        
    <?php else: ?>
        
      You are not Connected.
      <br/>
      <a href="<?php echo $loginUrl; ?>">Login with Facebook</a>
      
    <?php endif ?>

    <?php // print_r($user_profile); ?>
  </body>
</html>
