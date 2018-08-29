[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.forwardloop/glicko2s_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.forwardloop/glicko2s_2.11)
[![Build Status](https://travis-ci.org/forwardloop/glicko2s.svg)](https://travis-ci.org/forwardloop/glicko2s)
[![Coverage Status](https://coveralls.io/repos/github/forwardloop/glicko2s/badge.svg?branch=master)](https://coveralls.io/github/forwardloop/glicko2s?branch=master)

# glicko2s

Glicko2 sport players' rating algorithm for the JVM. Details on ELO and Glicko systems can be found at [ELO Wikipedia](https://en.wikipedia.org/wiki/Elo_rating_system), [Glicko Wikipedia](https://en.wikipedia.org/wiki/Glicko_rating_system),
or [Glicko-2 Example](http://www.glicko.net/glicko/glicko2.pdf). This project is used for computing ELO ratings in the [squash players](http://www.squashpoints.com) ranking system, for example in
[Waterfront](http://www.squashpoints.com/leagues/7232/public/latest) and [Fareham Leisure Centre](http://www.squashpoints.com/leagues/7182/public/latest) leagues. 

## Build

### Maven 

```xml
    <dependency>
        <groupId>com.github.forwardloop</groupId>
        <artifactId>glicko2s_2.12</artifactId>
        <version>0.9.4</version>
    </dependency>
```

### Gradle

```
    compile 'com.github.forwardloop:glicko2s_2.12:0.9.4'
```

### sbt

```scala
    libraryDependencies += "com.github.forwardloop" %% "glicko2s" % "0.9.4"
```


## Usage

Compute new rating for a player based on a sequence of match results with other players:

### Java
 
```java
     import static forwardloop.glicko2s.Glicko2J.newPlayerRating;
     import forwardloop.glicko2s.Glicko2;
     import scala.Tuple2;
     import java.util.Arrays;
     import java.util.List;
```

```java
     Glicko2 player = newPlayerRating();
     Glicko2 opponent1 = newPlayerRating();
     Glicko2 opponent2 = newPlayerRating();
    
     Tuple2<Glicko2, Result> match1 = new Tuple2(opponent1, Glicko2J.Win);
     Tuple2<Glicko2, Result> match2 = new Tuple2(opponent2, Glicko2J.Loss);
     Tuple2<Glicko2, Result> match3 = new Tuple2(opponent1, Glicko2J.Win);
    
     List<Tuple2<Glicko2, Result>> results = Arrays.asList(match1, match2, match3);
     Glicko2 newRating = Glicko2J.calculateNewRating(player, results);
``` 

### Scala

The project is cross-compiled for Scala 2.11 and 2.12.

```scala
    import forwardloop.glicko2s.{Loss, Win, Glicko2}
    
    val player, opponent1, opponent2 = new Glicko2
    val results = Seq(
         (opponent1, Win), 
         (opponent2, Loss), 
         (opponent1, Win))
    val newRating = player.calculateNewRating(results)
```

The rating, rating deviation and volatility parameters will change as follows:

```scala
    //player.toGlicko1:    rating: 1500, deviation: 350.00, volatility: 0.060000
    //newRating.toGlicko1: rating: 1600, deviation: 227.74, volatility: 0.059998
```    

## Customise 

### Weights of results 

The simple implementation of the `EloResult` trait provided allows three outcomes: win, draw or loss with 
weights 1.0, 0.5, 0.0, respectively. This can be fine tuned to differentiate between outcomes like 3:0 and 3:2,
to better reflect true players' level in ELO computations. An example implementation for racquet sports can be found
[here](https://github.com/forwardloop/highrung-model/blob/master/src/main/scala/highrung/model/RacquetEloResult.scala)

