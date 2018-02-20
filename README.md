[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.forwardloop/glicko2s_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.forwardloop/glicko2s_2.11)
[![Build Status](https://travis-ci.org/forwardloop/glicko2s.svg)](https://travis-ci.org/forwardloop/glicko2s)
[![Coverage Status](https://coveralls.io/repos/github/forwardloop/glicko2s/badge.svg?branch=master)](https://coveralls.io/github/forwardloop/glicko2s?branch=master)

# glicko2s

Glicko2 sport players' rating algorithm for the JVM.
 
Project is used for computing ELO ratings in the [squash players](http://www.squashpoints.com) ranking system, for example in
[Waterfront](http://www.squashpoints.com/leagues/7232/public/latest) and [Fareham Leisure Centre](http://www.squashpoints.com/leagues/7182/public/latest) leagues. 

## Usage

### Maven 

```xml
    <dependency>
        <groupId>com.github.forwardloop</groupId>
        <artifactId>glicko2s_2.11</artifactId>
        <version>0.9.2</version>
    </dependency>
```

### sbt

```scala
    libraryDependencies += "com.github.forwardloop" % "glicko2s_2.11" % "0.9.2"
```

### Java
 
```java
     import static forwardloop.glicko2s.Glicko2J.newPlayerRating;
     import forwardloop.glicko2s.Glicko2;
     import scala.Tuple2;
     import java.util.Arrays;
     import java.util.List;
```

```java
     Glicko2 playerRating = newPlayerRating();
     Glicko2 opponent1 = newPlayerRating();
     Glicko2 opponent2 = newPlayerRating();
    
     Tuple2<Glicko2, Result> match1 = new Tuple2(opponent1, Glicko2J.Win);
     Tuple2<Glicko2, Result> match2 = new Tuple2(opponent2, Glicko2J.Loss);
     Tuple2<Glicko2, Result> match3 = new Tuple2(opponent1, Glicko2J.Win);
    
     List<Tuple2<Glicko2, Result>> results = Arrays.asList(match1, match2, match3);
    
     Glicko2 newRating = Glicko2J.calculateNewRating(playerRating, results);
     
     //playerRating.toGlicko1: rating: 1500, deviation: 350.00, volatility: 0.060000
     //newRating.toGlicko1:    rating: 1600, deviation: 227.74, volatility: 0.059998
``` 

### Scala

```scala
    import forwardloop.glicko2s.{Loss, Win, Glicko2}
    
    val playerRating, opponent1, opponent2 = new Glicko2
    val results = List((opponent1, Win), (opponent2, Loss), (opponent1, Win))
    val newRating = playerRating.calculateNewRating(results)
    
    //playerRating.toGlicko1: rating: 1500, deviation: 350.00, volatility: 0.060000
    //newRating.toGlicko1:    rating: 1600, deviation: 227.74, volatility: 0.059998
```
 



