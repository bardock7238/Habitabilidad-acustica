package acustica.dominio;

public class Superficie {

    private final String id;
    private final String tipo;
    private final double area;
    private final Material material;
    private final Habitacion habitacion1;
    private final Habitacion habitacion2;

    public Superficie(String id, String tipo, double area, Material material, Habitacion habitacion1) {
        this(id, tipo, area, material, habitacion1, null);
    }

    public Superficie(String id, String tipo, double area, Material material, Habitacion habitacion1, Habitacion habitacion2) {
        this.id = id;
        this.tipo = tipo;
        this.area = area;
        this.material = material;
        this.habitacion1 = habitacion1;
        this.habitacion2 = habitacion2;
    }

    public double calcularAtenuacion() {
        return material.getCoeficienteAtenuacion();
    }

    public double calcularAtenuacionHacia(Habitacion receptora) {
        if (receptora == null) 
            return calcularAtenuacion();     // hacia el EXTERIOR
    double w = calcularAtenuacion() - 10 * Math.log10(area / receptora.calcularAbsorcion());
    return Math.max(0.0, w);      // Dijkstra exige pesos >= 0
}

    public boolean esParedInterior() {
        return habitacion2 != null;
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public double getArea() {
        return area;
    }

    public Material getMaterial() {
        return material;
    }

    public Habitacion getHabitacion1() {
        return habitacion1;
    }

    public Habitacion getHabitacion2() {
        return habitacion2;
    }
}