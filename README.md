# CG Benchmark

Like [CGSpunk](https://github.com/danBhentschel/CGSpunk), except it's made in Java  
... and you don't need to have your browser open  
... and you can queue several codes to run big fat batches of games and compare results easily.

### What it does:
Allows you to queue batches of matches on any multiplayer game of CodinGame.  
Simulates PLAY in the IDE and gathers results.  
You can add an unlimited number of source code in the configuration file, they'll be benchmarked one by one.  
A .txt report file with global winrate and replay links will be produced for each of them.  
Reports for a single code looks like [this](https://pastebin.com/q7pDSAhW)

### Prerequisites
The tool requires JRE 1.8 to run, and JDK 1.8 to build.
You can grab the pre-compiled Jar in the [releases](https://github.com/s-vivien/CGBenchmark/releases) if you just want to use the tool as is.

### Build:
The tool is built with Gradle, using the task `fatJar` :
```
gradle fatJar
```
or, for Windows :
```
gradlew.bat fatJar
```
The result is a standalone jar.

### Run:
`java -jar CGBenchmark.jar -c <path_to_your_configuration_file> [-l]`

### Configure:
Before you can run the tool, you must configure your CG account, code list and some other stuff.  
For the `agentId` of the enemy you want to benchmark your code against, you can grab it on [CGStats](http://cgstats.magusgeek.com) by searching the opponent's nickname in the leaderboard; its agentId will be displayed at the top of the results.

The configuration uses the YAML format (which is a superset of JSON, so your old configs in JSON should still work), and must contains the following items :
```yaml
---
# Account configuration
accountConfigurationList:
- accountName: Neumann # Name of your account
  accountLogin: email@provider.com # Login of your account
  accountPassword: 123password # Password of your account

# If enabled, seed list will be ignored and every match will be played against a random seed
randomSeed: false

# The name of the multiplayer game as it appears at the end of the url of your IDE
multiName: wondev-woman

# Optional. Indicates if the game is a contest or not. FALSE if not provided
isContest: true

# List of seeds to play
seedList:
- seed=747848718
- seed=851888179
- seed=700073541
- seed=683972927
- seed=586660547
- seed=410110611

# Cooldown between every match, 20 is the minimum to avoid CG's limitation
requestCooldown: 20

# [0, N] forced start position at N
# -1 : Each seed is played with every starting positions configuration. (Works only with fixed seed list).
#       In 1v1, it will generate 2 games, 6 games in 1v2 and 24 games in 1v3. Best suited for non symmetrical and/or turn-based games.
# -2 : Each seed is played once, with random starting positions. Best suited for perfectly symmetrical 
#       and non turn-based games in which starting position doesn't really matter, like MM, GoD, CotC, GitC, ...
playerPosition: -1

# Minimum number of enemies to play against
minEnemiesNumber: 1

# Maximum number of enemies to play against
maxEnemiesNumber: 3

# List of tested codes
codeConfigurationList:
- sourcePath: C:/CGBenchmark/totest/1.cpp
  nbReplays: 1 # Number of times each seed will be played
  language: C++ # Code language
  enemies: # Enemies list. At each game, random enemies are picked from this list (their number is also picked randomly between <minEnemiesNumber> and <maxEnemiesNumber>)
  - agentId: '762230' # Enemy agentId
    name: Agade # Enemy name
  - agentId: '817482'
    name: pb4
  - agentId: '812582'
    name: reCurse
- sourcePath: C:/CGBenchmark/totest/2.cpp
  nbReplays: 1
  language: C++
  enemies:
  - agentId: '762230'
    name: Agade
  - agentId: '817482'
    name: pb4
  - agentId: '812582'
    name: reCurse

```

### Latest features :
- YAML support
- Estimation of remaining benchmark time
- Works during contests (new parameter `isContest` in the configuration file)
- You can pause/resume a running benchmark by pressing ENTER
- Logs of every game are saved in a `logs` folder (enable with `-l` parameter)
- `playerPosition` now generates every starting positions configuration, according to player number (2 permutations in 1v1, 6 in 1v2 and 24 in 1v3). See comments in configuration file for detailed explanations.
- 1vN games support
- You can now define a N enemies pool for each code configuration. Enemies will be picked randomly at each game (as well as their number, which will be a random value between `minEnemiesNumber` and `maxEnemiesNumber`). These random choices are deterministic, i.e. if you benchmark two codes with the same `enemies` configuration and a fixed seed list, each seed will be played against the same enemies every time.

### Things that would be cool to have:
 * Unit tests ...
 * Bring back separated P1/P2 winrates for 1v1 games
 * Reduce benchmark time with adaptive cooldown between games
 * Error margin in stats
 * Early benchmark cut if winrate is too low (with a minimum a played matches)
 * Excel-like output
