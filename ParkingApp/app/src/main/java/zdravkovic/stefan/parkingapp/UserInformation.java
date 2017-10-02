package zdravkovic.stefan.parkingapp;

/**
 * Created by stefa on 9/13/2017.
 */

public class UserInformation {

    public String first_name;
    public String last_name;
    public String address;
    public String city;
    public String country;
    public String zip;
    public String phone_number;

    public UserInformation(){

    }

    public UserInformation(String first_name, String last_name, String address, String city, String country, String zip, String phone_number) {
        this.first_name = first_name;
        this.last_name = last_name;
        this.address = address;
        this.city = city;
        this.country = country;
        this.zip = zip;
        this.phone_number = phone_number;
    }
}
