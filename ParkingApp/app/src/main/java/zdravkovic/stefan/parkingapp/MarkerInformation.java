package zdravkovic.stefan.parkingapp;

/**
 * Created by stefa on 9/29/2017.
 */

public class MarkerInformation {
    public String latitude,longitude;
    public String title;
    public float averageRating;
    public int sumChecked,sumUnchecked;
    public String price;

    public MarkerInformation(){

    }

    public MarkerInformation(String latitude, String longitude, String title, float averageRating, int sumChecked, int sumUnchecked, String price) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.title = title;
        this.averageRating = averageRating;
        this.sumChecked = sumChecked;
        this.sumUnchecked = sumUnchecked;
        this.price = price;
    }

}
