<?php

require 'MovieWatchInfoService.php';
      
$movies = array(
    array('title' => 'Eega'),
    array('title' => 'Bol Bachchan'),
);
$watchInfoService = new MovieWatchInfoService();      
$result = $watchInfoService->getMovieWatchOptions($movies);

?>

<!doctype html>
<html>
  <head>
    <title>Sesame test</title>
    <link rel="stylesheet" type="text/css" href="controls.css">
  </head>
  <body>

    <?php print_r($result); ?>        
      
  </body>
</html>
