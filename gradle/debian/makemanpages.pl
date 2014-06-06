#!/usr/bin/perl

use Date::Parse;
use Time::Local;
use POSIX qw(strftime);

# Fix locale (for dates)
use locale;
use POSIX qw(locale_h);
setlocale(LC_CTYPE, "en_US.UTF-8");
setlocale(LC_TIME, "en_US.UTF-8");

if ($#ARGV+1 != 1) {
	print "Usage: makemanpages.pl COMMANDNAME\n";
	exit 1;
}

my $command = $ARGV[0];
my $thcommand = $command eq "syncany" ? "syncany" : "syncany-$command"; 
my $skelfile = $command eq "syncany" ? "help.skel" : "cmd/help.$command.skel";

open(IN, "../../syncany-cli/src/main/resources/org/syncany/cli/help/$skelfile");
open(OUT, '>', "../../build/debian/syncany/debian/manpage.$thcommand.1");

print OUT ".TH $thcommand 1\n";

my $indent = 0;
my $nf = 0;

while (my $line = <IN>) {
	chomp $line;
	
	# Trim right
	$line =~ s/\s+$//g; 
	
	# Header
	if ($line =~ /^[A-Z\s]+$/) {	
		if ($indent > 0) {
			print OUT ".RE\n";
			$indent = 0;
		}
		
		print OUT ".SH $line\n";
		$lastth = $line;
	}
	
	# Empty line
	elsif ($line =~ /^\s*$/) {
		print OUT ".PP\n";
		#print OUT "\n";
	}
	else {	
		# Trim left two spaces
		$line =~ s/^\s\s//g; 
		
		# Calculate indent (after two left spaces)
		my $lineindent = 0;
	
		if ($line =~ /^(\s+)/) {
			$lineindent = length $1;
		}
		
		
		if (!$nf && $line =~ /^\s*[-\*]/) {
			print OUT ".nf\n";
			$nf = 1;
		}
		elsif ($nf && $line =~ /^\s*[^-\*]/) {
			print OUT ".fi\n";
			$nf = 0;
		}

		
		# Remove indent 
		$line =~ s/^\s*//g; 

		# Print without indent
		if ($lineindent > $indent) {
			print OUT ".RS\n";
			print OUT "$line\n";
		}
		elsif ($lineindent < $indent) {
			print OUT ".RE\n";
			print OUT "$line\n";
		}
		else {
			print OUT "$line\n";
		}
	
		# Set new indent	
		$indent = $lineindent
	}
}

close(IN);
close(OUT);

