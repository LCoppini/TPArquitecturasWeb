package Repository;



public class EstudianteCarreraImple {
    //Singleton
    private static EstudianteCarreraImple instance = new EstudianteCarreraImple();

    private EstudianteCarreraImple getInstance(){
        return instance;
    }

    public EstudianteCarreraImple(){

    }
}
