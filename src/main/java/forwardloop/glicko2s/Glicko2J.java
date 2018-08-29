package forwardloop.glicko2s;

import scala.Tuple2;
import scala.collection.JavaConversions;
import java.util.List;

/**
 * Java-Scala interop
 */
public class Glicko2J {

    public static final EloResult Win = Win$.MODULE$;
    public static final EloResult Loss = Loss$.MODULE$;

    private Glicko2J(){}

    public static Glicko2 newPlayerRating() {
        return new Glicko1(
                Glicko2.NewPlayerRatingG1(),
                Glicko2.NewPlayerRatingDeviationG1(),
                Glicko2.NewPlayerVolatilityG1()).toGlicko2();
    }

    public static Glicko2 calculateNewRating(Glicko2 baseRating, List<Tuple2<Glicko2, EloResult>> results){
        return baseRating.calculateNewRating( JavaConversions.asScalaBuffer(results).toSeq() );
    }
}
