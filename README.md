#JSimpleSynth
Joshua Lund

This is a basic synthesizer to play sounds and sound recordings based on keys
inputted by the user.  It is written in Java using the Swing API for its GUI
and the Java sound API for audio output.  The user may run the program and hit
"Go" to either record keys, add to the existing composition, or listen to the
existing composition.

There are also Save and Save WAV buttons to output the current composition to
a custom binary format or a raw WAV file, respectively.  Files in this custom
format can also be loaded using the program's Load button.  (A recording
entitled abc.jss is included to test this functionality.)  The user may select
between 4 different "instruments" while playing sounds.  There is also an
option to adjust the time slice size used by the program.  (This affects the
latency, precision and buffer size used in the program.)  The default value
seems to be quite suitable, however.

The program can be used to make crude musical recordings using the keyboard
and to save those recordings either to its custom format or to WAV files.

The program can be compiled by changing to the directory containing the source
files and running the following command:

    javac *.java

Then the program can be run by executing it as follows:

    java Program

The program utilizes Java's Swing API, so it must be run in a graphical
environment in order to use its features.  The program is not very pretty, and
its user interface could definitely use some polish, however it is fully
functional and showcases its mixing and sound generating features reasonably
well.
