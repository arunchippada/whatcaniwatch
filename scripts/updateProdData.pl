#!/usr/bing/perl
use strict;
use warnings;

use DateTime;
use Cwd;
use File::Copy qw(move);
use Win32::Process::Info;
use Win32::Process;

my $projDir = 'C:\Projects\wciw';
my $logDir = "$projDir\\logs";
my $buildDir = "$projDir\\build";
my $dataDir = "$projDir\\data";
my $repositoryDir = 'C:\Users\Administrator\AppData\Roaming\Aduna\OpenRDF Sesame';
my $repositoryName = 'movies-global'; 
my $jettyDir = 'C:\projects\jetty-distribution-9.0.4.v20130625\jetty-distribution-9.0.4.v20130625';
my $jettyCmdLine = 'java -jar start.jar';

sub stopJetty
{
	my $pi = Win32::Process::Info->new();
	my @info = $pi->GetProcInfo();
	foreach my $procInfo (@info)
	{
		my $cmd = $procInfo->{CommandLine};
		if(defined($cmd))
		{
			# remove any extra spaces in the cmd line before comparison
			$cmd =~ s/\s+/ /g;
			if($cmd eq $jettyCmdLine)
			{
				my $process;
				my $pid = $procInfo->{ProcessId};
				Win32::Process::Open($process,$pid,1);
				$process->Kill(0);
				print "Stopped jetty processid = $pid\n";
			}
		}
	}	
}

sub startJetty
{
	chdir("$jettyDir");
	print "Starting Jetty\n";	
	system("$jettyCmdLine");
}

# get the update files from the data dir
sub getUpdateFiles
{
	my @files = glob("$dataDir\\global_*");
	return @files;
}

# get the unique date patterns from the update file names
sub getUniqueDatePatterns
{
	my @files = @_;
	my %dates = ();
	foreach my $file (@files)
	{
		# the date pattern is expected at the end of the file name
		my ($date) = ($file =~ /(\d+_\d+_\d+)$/);
		$dates{$date} = 1;
	}
	return sort keys(%dates);
}

# update the global movies repository from the update files
sub updateGlobalRepository
{
	my @dates = @_;

	chdir("$logDir");	
	print "Current working dir = " . cwd() . "\n";	
	foreach my $date (@dates)
	{
		my $addedFileName = "global_added_$date";
		my $cmd = "java -jar $buildDir\\Utilities\\Utilities.jar addTriples \"$repositoryDir\" $repositoryName $dataDir\\$addedFileName > $addedFileName.log";
		print "$cmd\n";
		system("$cmd");		
		sleep 60;

		my $removedFileName = "global_removed_$date";
		$cmd = "java -jar $buildDir\\Utilities\\Utilities.jar removeTriples \"$repositoryDir\" $repositoryName $dataDir\\$removedFileName > $removedFileName.log";
		print "$cmd\n";
		system("$cmd");
		sleep 60;
	}
}


# MAIN

# first check if there are any update files in the dataDir. If not, then there is nothing to do and we simply exit
my @updateFiles = getUpdateFiles();
my $cnt = scalar(@updateFiles);
print "Found $cnt update files\n";
if(!$cnt)
{
	print "Exiting\n";
	exit;
}

# clean up any previous logfiles (to save space on the Prod machine)
unlink glob("$logDir\\*");

# get the date patterns from the update files
my @dates = getUniqueDatePatterns(@updateFiles);

# stop jetty
stopJetty();

sleep 60;

# update movies-global repository from the update files
updateGlobalRepository(@dates);

# delete the update files
unlink @updateFiles;

# start jetty (this will not return, so must be the last thing
startJetty();


