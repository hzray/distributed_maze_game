

## distributed-maze-game

This is a distributed maze game. The maze is an N-by-N grid, consisting of N*N “cells”. Each cell contains either no “treasure” or exactly one “treasure”. At the start of the game, there are K treasures placed randomly at the grid.

### Set up

1. Compile maze game

```
cd src/main/java
javac *.java
```

2. Run tracker

```
java Tracker 1099 15 10
```

### Launch game

1. Run the game with a 2-character name, e.g. `p1`.

```
java Game 127.0.0.1 1099 p1
```

### Stress testing

1. Compile stress test

```
javac StressTest.java
```

2. Run stress test

```
java StressTest 127.0.0.1 1099 "java Game"
```