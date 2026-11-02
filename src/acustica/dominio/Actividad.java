package acustica.dominio;

public enum Actividad {
    DORMIR(0, 30, 40),
    ESTUDIAR(0, 35, 45),
    LLAMADAS(55, 45, 55),
    CONVERSAR(65, 55, 65),
    ENTRETENIMIENTO(75, 65, 75),
    INSTRUMENTO(85, 75, 85),
    EJERCICIO(90, 80, 90);

    public final double emiteDba;
    public final double limiteOptimo;
    public final double limiteMax;

    Actividad(double emiteDba, double limiteOptimo, double limiteMax) {
        this.emiteDba = emiteDba;
        this.limiteOptimo = limiteOptimo;
        this.limiteMax = limiteMax;
    }
}