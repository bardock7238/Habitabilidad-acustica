package acustica.mejora;

/**
 * Una solución candidata para volver habitable una habitación.
 *
 * Guarda qué se haría, cuántos dB se espera reducir y cuánto cuesta. Además
 * modela el LARGO PLAZO: una solución se desgasta con el tiempo, y mientras
 * dura el margen (reducción - exceso) la habitación sigue habitable. Cuando el
 * margen se acaba hay que hacer mantenimiento, y ese costo se suma al costo
 * total a {@code horizonteAnios}.
 *
 * El orden natural (el del árbol rojinegro) es por COSTO TOTAL a largo plazo,
 * ascendente; si empata, gana la que más reduce y, por último, el id (así dos
 * soluciones distintas nunca comparan igual).
 */
public class Solucion implements Comparable<Solucion> {

    public enum Tipo {
        MEJORAR_MATERIAL,
        AUMENTAR_ABSORCION,
        REDUCIR_EMISION,
        CAMBIAR_DE_PISO
    }

    /** Horizonte por defecto cuando no se indica uno. */
    public static final int HORIZONTE_POR_DEFECTO = 10;

    private final String id;
    private final Tipo tipo;
    private final String habitacionId;
    private final String descripcion;
    private final double reduccionDb;
    private final double costo;
    private final double aniosSinMantenimiento;
    private final double costoMantenimiento;
    private final int horizonteAnios;
    private final boolean verificada;
    private final int mantenimientos;
    private final double costoTotal;

    /** Solución sin desgaste (no necesita mantenimiento) y sin verificar por simulación. */
    public Solucion(String id, Tipo tipo, String habitacionId, String descripcion,
            double reduccionDb, double costo) {
        this(id, tipo, habitacionId, descripcion, reduccionDb, costo,
                Double.POSITIVE_INFINITY, 0.0, HORIZONTE_POR_DEFECTO, false);
    }

    /**
     * @param aniosSinMantenimiento años que la solución sigue cumpliendo antes de
     *                              necesitar mantenimiento (Infinity si no se desgasta)
     * @param costoMantenimiento    costo de UN mantenimiento (restaura la solución)
     * @param horizonteAnios        años a los que se calcula el costo total
     * @param verificada            true si se comprobó volviendo a correr el simulador;
     *                              false si es solo una estimación
     */
    public Solucion(String id, Tipo tipo, String habitacionId, String descripcion,
            double reduccionDb, double costo, double aniosSinMantenimiento,
            double costoMantenimiento, int horizonteAnios, boolean verificada) {
        if (id == null || tipo == null || habitacionId == null) {
            throw new IllegalArgumentException("id, tipo y habitación no pueden ser null");
        }
        if (reduccionDb <= 0) {
            throw new IllegalArgumentException("La reducción debe ser mayor que 0 dB");
        }
        if (costo < 0 || costoMantenimiento < 0) {
            throw new IllegalArgumentException("Los costos no pueden ser negativos");
        }
        if (aniosSinMantenimiento <= 0) {
            throw new IllegalArgumentException("Los años sin mantenimiento deben ser mayores que 0");
        }
        if (horizonteAnios <= 0) {
            throw new IllegalArgumentException("El horizonte debe ser mayor que 0 años");
        }
        this.id = id;
        this.tipo = tipo;
        this.habitacionId = habitacionId;
        this.descripcion = descripcion;
        this.reduccionDb = reduccionDb;
        this.costo = costo;
        this.aniosSinMantenimiento = aniosSinMantenimiento;
        this.costoMantenimiento = costoMantenimiento;
        this.horizonteAnios = horizonteAnios;
        this.verificada = verificada;
        this.mantenimientos = contarMantenimientos(aniosSinMantenimiento, horizonteAnios);
        this.costoTotal = costo + mantenimientos * costoMantenimiento;
    }

    /**
     * Años que la solución mantiene la habitación dentro del límite.
     *
     * El margen es lo que sobra de reducción sobre el exceso; cada año la
     * solución pierde {@code degradacionDbPorAnio}. Devuelve:
     *  - Infinity si no se desgasta, o si la solución sola no alcanza a cubrir
     *    el exceso (en ese caso el mantenimiento no cambia nada);
     *  - al menos 1 año si cubre justo (sin margen).
     */
    public static double aniosDeVida(double reduccionDb, double excesoDb, double degradacionDbPorAnio) {
        if (degradacionDbPorAnio <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        double margen = reduccionDb - excesoDb;
        if (margen < 0) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.max(1.0, margen / degradacionDbPorAnio);
    }

    private static int contarMantenimientos(double anios, int horizonte) {
        if (Double.isInfinite(anios) || anios >= horizonte) {
            return 0;
        }
        return (int) Math.ceil(horizonte / anios) - 1;
    }

    public String getId() {
        return id;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getHabitacionId() {
        return habitacionId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** dB que se espera bajar el nivel que llega a la habitación. */
    public double getReduccionDb() {
        return reduccionDb;
    }

    /** Costo inicial de aplicar la solución. */
    public double getCosto() {
        return costo;
    }

    /** Años antes de necesitar mantenimiento (Infinity si no se desgasta). */
    public double getAniosSinMantenimiento() {
        return aniosSinMantenimiento;
    }

    public double getCostoMantenimiento() {
        return costoMantenimiento;
    }

    public int getHorizonteAnios() {
        return horizonteAnios;
    }

    /** Cuántos mantenimientos hacen falta dentro del horizonte. */
    public int getMantenimientos() {
        return mantenimientos;
    }

    /** true si dentro del horizonte hace falta al menos un mantenimiento. */
    public boolean requiereMantenimiento() {
        return mantenimientos > 0;
    }

    /** Costo inicial + mantenimientos dentro del horizonte. Es la clave del árbol. */
    public double getCostoTotal() {
        return costoTotal;
    }

    /** true si se comprobó con el simulador; false si es una estimación por fórmula. */
    public boolean isVerificada() {
        return verificada;
    }

    /** Costo total por cada dB que se reduce: sirve para comparar eficiencia. */
    public double getCostoPorDb() {
        return costoTotal / reduccionDb;
    }

    @Override
    public int compareTo(Solucion otra) {
        int c = Double.compare(costoTotal, otra.costoTotal);
        if (c != 0) {
            return c;
        }
        c = Double.compare(otra.reduccionDb, reduccionDb); // más reducción primero
        return c != 0 ? c : id.compareTo(otra.id);
    }

    @Override
    public String toString() {
        String mant = requiereMantenimiento()
                ? String.format(", %d mantenimiento(s) en %d años", mantenimientos, horizonteAnios)
                : ", sin mantenimiento";
        return String.format("[%s%s] %s -> -%.1f dB, costo %.1f, total %.1f%s",
                tipo, verificada ? "*" : "", descripcion, reduccionDb, costo, costoTotal, mant);
    }
}