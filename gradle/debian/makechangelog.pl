#!/usr/bin/perl

use Date::Parse;
use Time::Local;
use POSIX qw(strftime);

# Fix locale (for dates)
use locale;
use POSIX qw(locale_h);
setlocale(LC_CTYPE, "en_US.UTF-8");
setlocale(LC_TIME, "en_US.UTF-8");

if ($#ARGV+1 != 5) {
	print "Usage: makechangelog.pl <package-name> <distribution> <version> <in-file> <out-file>\n";
	exit 1;
}


my $app_package_name = $ARGV[0];
my $app_distribution = $ARGV[1];
my $app_version = $ARGV[2];
my $file_in = $ARGV[3];
my $file_out = $ARGV[4];
my $last_date;
my $last_version;

open(IN, $file_in);
open(OUT, '>', $file_out);

while (<IN>) {
	chomp;
	
	$_ =~ s/\s+$//g; # trim right
	
	if (/### .+ ([^ ]+)\s+\(Date: ([^)]+)\)/) {		
		if ($last_version) {
			footer($last_date, $last_version);

			$last_version = $1;
			$last_date = $2;
		}
		else {
			$last_version = $app_version;
			$last_date = "n/a";
		}

		print OUT "$app_package_name ($last_version) $app_distribution; urgency=low\n";
		print OUT "\n";
	}
	elsif (/^\s|-/) {	
		$_ =~ s/^  /    /;
		$_ =~ s/^- /  * /;
	
		print OUT "$_\n";
	}
}

footer($last_date, $last_version);

close(IN);
close(OUT);

sub footer {
	my ($last_date, $last_version) = @_;
	
	my ($ss, $mm, $hh, $day, $month, $year, $zone) = strptime($last_date);
	my @releasetime = ($day) ? localtime(timelocal(0, 0, 0, $day, $month, $year)) : localtime(time());
	my $rfc822date = strftime("%a, %d %b %Y %H:%M:%S %z", @releasetime);		

	print OUT "\n";
	print OUT " -- Syncany Team <hello\@syncany.org>  $rfc822date\n";
	print OUT "\n";
}
