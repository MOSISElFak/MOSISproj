package zdravkovic.stefan.parkingapp;

/**
 * Created by stefa on 10/1/2017.
 */

public class RatingObject {
    public float avgRating;
    public int sumChecked,sumUnchecked;

    public RatingObject(float avgRating, int sumChecked, int sumUnchecked) {
        this.avgRating = avgRating;
        this.sumChecked = sumChecked;
        this.sumUnchecked = sumUnchecked;
    }
    public RatingObject(){
        this.avgRating = 0;
        this.sumChecked = 0;
        this.sumUnchecked = 0;
    }
}
