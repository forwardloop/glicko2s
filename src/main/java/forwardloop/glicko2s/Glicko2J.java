package forwardloop.glicko2s;

import scala.Tuple2;
import scala.collection.JavaConversions;
import java.util.List;

/**
 * Scala interop
 */
public class Glicko2J {

    public static final Result Win = Win$.MODULE$;
    public static final Result Loss = Loss$.MODULE$;

    public static Glicko2 newPlayerRating() {
        return new Glicko1(
                Glicko2.NewPlayerRatingG1(),
                Glicko2.NewPlayerRatingDeviationG1(),
                Glicko2.NewPlayerVolatilityG1()).toGlicko2();
    }

    public static Glicko2 calculateNewRating(Glicko2 baseRating, List<Tuple2<Glicko2, Result>> results){
        return baseRating.calculateNewRating( JavaConversions.asScalaBuffer(results).toSeq() );
    }
}
