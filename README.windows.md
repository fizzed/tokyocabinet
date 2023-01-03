
Unfortunately, porting TokyoCabinet to windows is very complex w/ the posix APIs used.

We tried compiling it w/ MinGW, and got it to partially work, but ultimately many of the unit tests all failed.

https://github.com/fizzed/tokyocabinet-mingw-experimental

./configure --host=x86_64-w64-mingw32