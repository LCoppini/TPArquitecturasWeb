package Repository;

public class CarreraImple implements CarreraInter {
    // Singleton
    private static CarreraImple instance = new CarreraImple();

    private CarreraImple getInstance(){
        return instance;
    }

    public CarreraImple(){

    }


}
