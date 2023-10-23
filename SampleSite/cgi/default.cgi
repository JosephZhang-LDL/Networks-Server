#!/usr/bin/perl -w
print "Content-Type: text/html\r\n";
print "\r\n";

print "Hello, World.";

foreach my $line ( <STDIN> ) {
    chomp( $line );
    print "$line\n";
}