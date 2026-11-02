package acustica.dominio;

/**
 * Superficie que delimita una o dos habitaciones (pared, piso, techo, ventana,
 * puerta, fachada).
 *
 * Una superficie con dos habitaciones es una pared interior; una con una sola
 * habitación es una fachada hacia el exterior.
 *
 * La atenuación que aporta se calcula con el modelo v2:
 *
 *     w_{i→j} = max(0, R_k − 10·log10(S_k / A_j))
 *
 * donde {@code R_k} es el índice de reducción sonora del material,
 * {@code S_k} es el área de la superficie y {@code A_j} es la absorción
 * equivalente (Sabine) de la habitación que recibe el sonido.
 */
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

    public Superficie(String id, String tipo, double area, Material material,
            Habitacion habitacion1, Habitacion habitacion2) {
        if (material == null) {
            throw new IllegalArgumentException("La superficie necesita un material");
        }
        if (habitacion1 == null) {
            throw new IllegalArgumentException("La superficie necesita una habitación");
        }
        if (area <= 0) {
            throw new IllegalArgumentException("El área de la superficie debe ser mayor que 0");
        }
        this.id = id;
        this.tipo = tipo;
        this.area = area;
        this.material = material;
        this.habitacion1 = habitacion1;
        this.habitacion2 = habitacion2;
    }

    /** Atenuación hacia el exterior: se usa directamente R_k. */
    public double calcularAtenuacion() {
        return material.getIndiceReduccionSonora();
    }

    /**
     * Atenuación de la superficie cuando el sonido llega a {@code receptora}.
     *
     * @param receptora habitación que recibe el sonido; {@code null} para el exterior
     * @return peso w >= 0 en dB
     */
    public double calcularAtenuacionHacia(Habitacion receptora) {
        if (receptora == null) {
            return calcularAtenuacion(); // hacia el EXTERIOR
        }
        double absorcion = receptora.calcularAbsorcion();
        double w = calcularAtenuacion() - 10 * Math.log10(area / absorcion);
        return Math.max(0.0, w); // Dijkstra exige pesos >= 0
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
