package bgu.spl.net;

public class User {
    private String userName;
    private String password;
    private String bDay;
    private boolean logIn;
    private int id;

    public User(String userName, String password, String bDay){
        this.userName = userName;
        this.password = password;
        this.bDay = bDay;
        this.logIn = false;
        this.id = -1;
    }
    public void setId(int id){
        this.id = id;
    }
    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getbDay() {
        return bDay;
    }

    public boolean isLoggedIn() {
        return logIn;
    }

    public void logIn() {
        this.logIn = true;
    }
    public void logOut() {
        this.logIn = false;
    }
    public short getAge(){
        // bDay = dd-mm-yyyy
        // bday = 1-3-2000
        int index = 0;
        String bornYear = "";
        String bornMonth = "";
        String bornDay = "";
        for ( ; index < bDay.length() && bDay.charAt(index) != '-'; index++){
            bornDay = bornDay + bDay.charAt(index);
        }
        index++;
        for ( ; index < bDay.length() && bDay.charAt(index) != '-'; index++){
            bornMonth = bornMonth + bDay.charAt(index);
        }
        index++;
        for ( ; index < bDay.length() && bDay.charAt(index) != '-'; index++){
            bornYear = bornYear + bDay.charAt(index);
        }
        int year = Integer.parseInt(bornYear);
        int month = Integer.parseInt(bornMonth);
        //int day = Integer.parseInt(bornDay);
        if (month == 1){
            return (short) (2022-year);
        }
        else{
            return (short) (2021-year);
        }
    }
}
