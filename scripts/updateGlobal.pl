#!/usr/bing/perl
use strict;
use warnings;

use DateTime;
use Cwd;
use File::Copy qw(move);

my $projDir = 'C:\Projects\wciw';
my $logDir = "$projDir\\logs";
my $srcDir = "$projDir\\src";
my $dataDir = "$projDir\\data";
my $repositoryDir = 'C:\Users\Administrator\AppData\Roaming\Aduna\OpenRDF Sesame';
my $repositoryName = 'movies-global'; 

my $today = DateTime->now->strftime('%m_%d_%Y');
my $globalAddedDataFileName = "$dataDir\\globalupdates\\global_added_$today";
my $globalRemovedDataFileName = "$dataDir\\globalupdates\\global_removed_$today";

my @addedUnitFiles = ("$dataDir\\globalupdates\\cisi_added");
my @removedUnitFiles = ("$dataDir\\globalupdates\\cisi_removed");

# TODO: replace move with merge
# merge all unit update (added and removed) files to global update files
foreach my $addedFile (@addedUnitFiles)
{
	move $addedFile, $globalAddedDataFileName;
}
foreach my $removedFile (@removedUnitFiles)
{
	move $removedFile, $globalRemovedDataFileName;
}

# update movies-global repository from global_added and global_removed
chdir("$logDir\\globalupdates");
print "Current working dir = " . cwd() . "\n";

my $logFileName = "global_added_$today.log";
my $cmd = "java -jar $srcDir\\Utilities\\dist\\Utilities.jar addTriples \"$repositoryDir\" $repositoryName $globalAddedDataFileName > $logFileName";
print "$cmd\n";
system("$cmd");

$logFileName = "global_removed_$today.log";
$cmd = "java -jar $srcDir\\Utilities\\dist\\Utilities.jar removeTriples \"$repositoryDir\" $repositoryName $globalRemovedDataFileName > $logFileName";
print "$cmd\n";
system("$cmd");

# delete the unit update files
# TODO: uncomment after move -> merge
#unlink @addedUnitFiles;
#unlink @removedUnitFiles;