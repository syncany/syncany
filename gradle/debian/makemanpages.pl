#!/usr/bin/perl

use Date::Parse;
use Time::Local;
use POSIX qw(strftime);

# Fix locale (for dates)
use locale;
use POSIX qw(locale_h);
setlocale(LC_CTYPE, "en_US.UTF-8");
setlocale(LC_TIME, "en_US.UTF-8");

my $sourceDir = "../../syncany-cli/build/resources/main/org/syncany/cli";
my $targetBuildDir = "../../build/debian/syncany/debian";
my $targetManDir = "$targetBuildDir/man";

my @manpages = glob("$sourceDir/cmd/*.skel");
my $targetManFile = "$targetBuildDir/syncany.manpages";

system("mkdir -p $targetManDir");

open(MAN, '>', $targetManFile);

foreach (@manpages) {	
	my $skelfile = $_;
	my $command = ($skelfile =~ /help\.([-a-z]+)\.skel/) ? $1 : "sy";
	my $thcommand = $command eq "sy" ? "sy" : "sy-$command"; 

	print MAN "debian/man/$thcommand.1\n";

	my $manPage = formatManpage($skelfile, $thcommand);
	
	open OUT, '>', "$targetManDir/$thcommand.1";
	print OUT $manPage;
	close OUT;
}

close(MAN);

#EOF

sub formatManpage {
	my ($skelFile, $thcommand) = @_;
	
	my @manPageLines = ();
		
	if ($thcommand) {
		push(@manPageLines, ".TH $thcommand 1\n");
	}

	my $indent = 0;
	my $nf = 0;
	my $indentSection = 0;
	my @indents = (0);

	open(IN, "$skelFile");

	while (my $line = <IN>) {
		chomp $line;
	
		# Embed %RESOURCE:...%
		if ($line =~ /\%RESOURCE:([^%]+)\%/) {
			$includeSkelFile = "$sourceDir/$1";
			$includeManPage = formatManpage($includeSkelFile);
			
			$line =~ s/\%RESOURCE:[^%]+\%/$includeManPage/;
			push(@manPageLines, "$line\n");

			next;
		}
		
		# Trim right
		$line =~ s/\s+$//g; 
		
		# Escape line \ -> \\ and ' -> \'
		$line =~ s/([\\\'])/\\$1/g;
	
		# Header
		if ($line =~ /^([A-Z\s]+)$/) {	
			if ($indent > 0) {
				push(@manPageLines, ".RE\n");
				$indent = 0;
			}
			
			if ($1 =~ /SYNOPSIS|DESCRIPTION|COPYRIGHT/i) {
				$indentSection = 0;
			}
			else {
				$indentSection = 1;
			}
		
			push(@manPageLines, ".SH $line\n");
		}
	
		# Empty line
		elsif ($line =~ /^\s*$/) {
			push(@manPageLines, ".PP\n");
		}

		# Sections with indents enabled
		else {	
			# Trim left two spaces
			$line =~ s/^\s\s//g; 
						
			# Highlight options --some-option=<option1|option2=abc> or -o
			$line =~ s/(^|\W+?)(--?[-\w]+=?)/$1\\fB$2\\fR/g;
		
			# Highlight commands `ls -al`
			$line =~ s/(`[^`]+`)/\\fB$1\\fR/g;
			
			# Highlight args <args>
			$line =~ s/(<[-\w\|=]+>)/\\fB$1\\fR/g;

			# Replace - with \-
			$line =~ s/-/\\-/g;

			if ($indentSection) {
				# Calculate indent (after two left spaces)
				my $lineindent = 0;
	
				if ($line =~ /^(\s+)/) {
					$lineindent = length $1;
				}
		
				# Don't break list items and option lines
				if (!$nf && $line =~ /^\s*[-\*]/) {
					push(@manPageLines, ".nf\n");
					$nf = 1;
				}
				elsif ($nf && $line =~ /^\s*[^-\*]/) {
					push(@manPageLines, ".fi\n");
					$nf = 0;
				}
		
				# Remove indent 
				$line =~ s/^\s*//g; 

				# End indent sections				
				while ($lineindent < $indent && $#indents > 0) {
					$indent = pop(@indents);
					push(@manPageLines, ".RE\n");				
				}
				
				if ($lineindent > $indent) { 
					push(@manPageLines, ".RS\n");
					push(@manPageLines, "$line\n");
					
					push(@indents, $indent);
					$indent = $lineindent;
				}
				else {												
					push(@manPageLines, "$line\n");
					$indent = $lineindent;
				}
			}
			else {
				push(@manPageLines, "$line\n");
			}
		}
	}

	close(IN);
	
	return join("", @manPageLines);
}

