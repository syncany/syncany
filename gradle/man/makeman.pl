#!/usr/bin/perl
# 
# Syncany, www.syncany.org
# Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

#
# This script transforms any input plain text file into a 
# man page compatible file. 
#

use strict;
use warnings;

sub main {
	my $opt_infile = $ARGV[0];
	my $opt_outfile = $ARGV[1];
	
	if (not defined $opt_infile and not defined $opt_outfile) {
		print_usage_and_exit();
	}

	my @infile_lines = read_input($opt_infile);
	my @outfile_lines = format_page(@infile_lines);

	write_output($opt_outfile, @outfile_lines);
}

sub print_usage_and_exit() {
	print STDERR "Usage: makeman.pl [INFILE] [OUTFILE]\n";
	print STDERR "\n";
	print STDERR "Parameters:\n";
	print STDERR "  INFILE   Text file with structure of a man page (optional).\n";
	print STDERR "  OUTFILE  File at which to write the man page (optional).\n";
	print STDERR "\n";
	print STDERR "  Both parameters are optional. If - is given, STDIN/STDOUT is used.\n";	
	print STDERR "\n";
	print STDERR "Examples:\n";
	print STDERR "  makeman.pl sy.skel sy.1             # Creates man page at sy.1\n";
	print STDERR "  cat sy.skel | makeman.pl - > sy.1   # Same result\n";
	print STDERR "  cat sy.skel | makeman.pl - sy.1     # Same result\n";
	print STDERR "\n";
	
	exit 1;
}

sub read_input {
	my ($opt_infile) = @_;

	if (not defined $opt_infile or $opt_infile eq "-") {
		return read_from_stdin();
	}
	else {
		return read_from_file($opt_infile);
	}
}

sub read_from_stdin {
	return <STDIN>;		
}

sub read_from_file {
	my ($opt_infile) = @_;
	
	if (not open(IN, "$opt_infile")) {
		print STDERR "ERROR: Cannot read from file $opt_infile.\n";
		exit 2;
	}
	
	my @infile_lines = <IN>;
	close(IN);
		
	return @infile_lines;
}

sub format_page {
	my (@infile_lines) = @_;
		
	my @outfile_lines = ();
			
	my $indent = 0;
	my $nf = 0;
	my $indent_section = 0;
	my @indents = (0);

	# Print command header
	my $command = parse_command(@infile_lines);
	push(@outfile_lines, ".TH $command 1\n");

	# Parse other lines and print formatted lines
	foreach my $line (@infile_lines) {
		chomp $line;	
		
		# Trim right
		$line =~ s/\s+$//g; 
		
		# Escape line \ -> \\ and ' -> \'
		$line =~ s/([\\\'])/\\$1/g;
	
		# Header
		if ($line =~ /^([A-Z\s]+)$/) {	
			if ($indent > 0) {
				push(@outfile_lines, ".RE\n");
				$indent = 0;
			}
			
			if ($1 =~ /SYNOPSIS|DESCRIPTION|COPYRIGHT/i) {
				$indent_section = 0;
			}
			else {
				$indent_section = 1;
			}
		
			push(@outfile_lines, ".SH $line\n");
		}
	
		# Empty line
		elsif ($line =~ /^\s*$/) {
			push(@outfile_lines, ".PP\n");
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

			if ($indent_section) {
				# Calculate indent (after two left spaces)
				my $line_indent = 0;
	
				if ($line =~ /^(\s+)/) {
					$line_indent = length $1;
				}
		
				# Don't break list items and option lines
				if (!$nf && $line =~ /^\s*[-\*]/) {
					push(@outfile_lines, ".nf\n");
					$nf = 1;
				}
				elsif ($nf && $line =~ /^\s*[^-\*]/) {
					push(@outfile_lines, ".fi\n");
					$nf = 0;
				}
		
				# Remove indent 
				$line =~ s/^\s*//g; 

				# End indent sections				
				while ($line_indent < $indent && $#indents > 0) {
					$indent = pop(@indents);
					push(@outfile_lines, ".RE\n");				
				}
				
				if ($line_indent > $indent) { 
					push(@outfile_lines, ".RS\n");
					push(@outfile_lines, "$line\n");
					
					push(@indents, $indent);
					$indent = $line_indent;
				}
				else {												
					push(@outfile_lines, "$line\n");
					$indent = $line_indent;
				}
			}
			else {
				push(@outfile_lines, "$line\n");
			}
		}
	}
	
	return @outfile_lines;
}
	
sub parse_command {
	my (@infile_lines) = @_;
	
	my $infile_str = join("", @infile_lines),
	my $command = "";
	
	if ($infile_str =~ /\bNAME\b\s+([-\w]+)\s-/s) {
		return $1;
	}
	else {
		print STDERR "ERROR: No valid NAME section found. Cannot determine command name.\n";
		exit 4;
	}	
}
	
sub write_output {
	my ($opt_outfile, @outfile_lines) = @_;

	if (not defined $opt_outfile or $opt_outfile eq "-") {
		write_to_stdout(@outfile_lines);
	}
	else {
		write_to_file($opt_outfile, @outfile_lines);
	}
}

sub write_to_stdout {
	my (@outfile_lines) = @_;
	print @outfile_lines;
}

sub write_to_file {
	my ($opt_outfile, @outfile_lines) = @_;
	
	if (not open(OUT, ">$opt_outfile")) {
		print STDERR "ERROR: Cannot write to file $opt_outfile.\n";
		exit 3;
	}
	
	for my $line (@outfile_lines) {
	    print OUT $line;
	}
	
	close(OUT);
}

main();
