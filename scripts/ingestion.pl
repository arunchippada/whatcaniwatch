#!/usr/bing/perl
use strict;
use warnings;

use DateTime;
use Cwd;

my $projDir = 'C:\Projects\wciw';
my $logDir = "$projDir\\logs";
my $srcDir = "$projDir\\src";
my $scriptDir = "$projDir\\scripts";

my $today = DateTime->now->strftime('%m_%d_%Y');

# canistreamit cycle to publish the next set of changes to globalupdates
chdir("$logDir\\cisi");
print "Current working dir = " . cwd() . "\n";

my $logFileName = "cisi_$today.log";
my $cmd = "java -jar $srcDir\\CISIIngestionService\\dist\\CISIIngestionService.jar > $logFileName";

# using system so that we will wait for the ingestion to complete
print "$cmd\n";
system("$cmd");

# run updateGlobal.pl to consume the unit changes in the global repository
$cmd = "perl $scriptDir\\updateGlobal.pl";
print "$cmd\n";
system("$cmd");
