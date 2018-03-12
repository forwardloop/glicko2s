package forwardloop.glicko2s;

import static org.junit.Assert.assertEquals;
import static forwardloop.glicko2s.Glicko2J.newPlayerRating;
import org.junit.Test;
import scala.Tuple2;
import java.util.Arrays;
import java.util.List;

public class Glicko2JTest {

    @Test
    public void expectedGlicko2ComputationJava() {

        Glicko2 playerRating = newPlayerRating();
        Glicko2 opponent1 = newPlayerRating();
        Glicko2 opponent2 = newPlayerRating();

        Tuple2<Glicko2, EloResult> g1 = new Tuple2(opponent1, Glicko2J.Win);
        Tuple2<Glicko2, EloResult> g2 = new Tuple2(opponent2, Glicko2J.Loss);
        Tuple2<Glicko2, EloResult> g3 = new Tuple2(opponent1, Glicko2J.Win);

        List<Tuple2<Glicko2, EloResult>> results = Arrays.asList(g1, g2, g3);

        Glicko2 newRating = Glicko2J.calculateNewRating(playerRating, results);

        Glicko1 expectedG1 = new Glicko1(1600, 227.74, 0.059998);
        Glicko1 actualG1 = newRating.toGlicko1();

        assertEquals(actualG1.rating(), expectedG1.rating(), 0.2);
        assertEquals(actualG1.ratingDeviation(), expectedG1.ratingDeviation(), 0.1);
        assertEquals(actualG1.ratingVolatility(), expectedG1.ratingVolatility(), 0.1);
    }
}
