#!/usr/bin/perl -w
print "Content-type: text/html\r\n";
print "\r\n";

print "Hello, World.";
print "uh oh";

foreach my $line ( <STDIN> ) {
    chomp( $line );
    print "$line\n";
}