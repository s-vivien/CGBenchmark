# CG Benchmark

Like [CGSpunk](https://github.com/danBhentschel/CGSpunk), except it's made in Java  
... and you don't need to have your browser open  
... and you can queue several codes to run big fat batches of games and compare results easily.

### What it does:
Allows you to queue batches of matches on any multiplayer game of CodinGame.  
Simulates PLAY in the IDE and gathers results.  
You can add an unlimited number of source code in the configuration file, they'll be benchmarked one by one.  
Supports multi-account; matches are distributed between the configured accounts to fasten the benchmark.  
A .txt report file with global winrate and replay links will be produced for each of them.  
Reports for a single code looks like [this](https://pastebin.com/PSJphjsi)

### Build:
The tool requires JDK 1.8, and is built with Gradle, using the task `fatJar`.  
The result is a standalone jar.

### Run:
`java -jar CGBenchmark.jar -c <path_to_your_json_configuration_file>`

### Configure:
Before you can run the tool, you must configure your CG account, code list and some other stuff.  
For the `agentId` of the enemy you want to benchmark your code against, you can grab it on [CGStats](http://cgstats.magusgeek.com) by searching the opponent's nickname in the leaderboard; its agentId will be displayed at the top of the results.

The configuration uses the JSON format, and must contains the following items :
```javascript
{
  // Account configuration
  "accountConfigurationList": [
    {
      // Name of your account
      "accountName": "Neumann",
      // Login of your account
      "accountLogin": "email@provider.com",
      // Password of your account
      "accountPassword": "123password",
    }
  ],

  // If enabled, seed list will be ignored and every match will be played against a random seed
  "randomSeed": "false",

  // The name of the multiplayer game as it appears at the end of the url of your IDE
  "multiName": "wondev-woman",
  
  // List of seeds to play
  "seedList": [
    "mapIndex=2\nseed=926288117",
    "mapIndex=1\nseed=994118117",
    "mapIndex=0\nseed=370687330",
    "mapIndex=1\nseed=317391334",
    "mapIndex=2\nseed=310630498",
    "mapIndex=0\nseed=482637334"
  ],

  // Cooldown between every match, 20 is the minimum to avoid CG's limitation
  "requestCooldown": "20",

  // Position of your AI, 0=player1, 1=player2, -1=every match played twice with swapped positions
  "playerPosition": "-1",

  // List of tested codes
  "codeConfigurationList": [
    {
      // Path to your code
      "sourcePath": "C:/CGBenchmark/totest/1.cpp",
      // Number of times each seed will be played
      "nbReplays": "1",
      // Code language
      "language": "C++",
      // AgentID of your enemy
      "enemyAgentId": "1408472",
      // Name of your enemy, for log purpose
      "enemyName": "Agade"
    },
    {
      "sourcePath": "C:/CGBenchmark/totest/2.cpp",
      "nbReplays": "1",
      "language": "C++",
      "enemyAgentId": "1408472",
      "enemyName": "Agade"
    },
    [...]
  ]
}
```

### Things that would be cool to have:

 * Error margin in stats
 * Proper draw support
 * Proper 1vN support

