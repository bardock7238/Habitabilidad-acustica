package acustica.mejora;

import acustica.estructuras.ArbolRojinegro;
import acustica.estructuras.NodoVisual;
import java.util.ArrayList;
import java.util.List;

/**
 * Soluciones candidatas para UNA habitación no habitable, guardadas en un
 * árbol rojinegro ordenado por costo (de la más barata a la más cara).
 *
 * Se usa un rojinegro porque el conjunto cambia seguido: se agregan
 * candidatas al generarlas y se descartan las que ya no sirven; el
 * rojinegro hace pocas rotaciones en cada cambio.
 */
public class PlanMejora {

    private final String habitacionId;
    private final double excesoDb;
    private final ArbolRojinegro<Solucion> soluciones = new ArbolRojinegro<>(Solucion::compareTo);

    /**
     * @param habitacionId id de la habitación a mejorar
     * @param excesoDb     dB que hay que reducir para cumplir el límite (> 0)
     */
    public PlanMejora(String habitacionId, double excesoDb) {
        this.habitacionId = habitacionId;
        this.excesoDb = excesoDb;
    }

    public boolean agregar(Solucion solucion) {
        return soluciones.insertar(solucion);
    }

    public boolean descartar(Solucion solucion) {
        return soluciones.eliminar(solucion);
    }

    
    /**
     * La candidata más barata del árbol.
     * Puede ser una estimación sin verificar.
     */
    public Solucion masBarata() {
        return soluciones.minimo();
    }

    /**
     * La solución verificada más barata,
     * aunque todavía no alcance el límite.
     */
    public Solucion masBarataVerificada() {
        return soluciones.primeroQue(
                Solucion::isVerificada);
    }

    /**
     * La solución verificada más barata
     * que cubre el exceso de esta habitación.
     */
    public Solucion masBarataQueCubra() {
        return masBarataQueCubra(excesoDb);
    }

    /**
     * Busca la solución verificada de menor costo
     * que reduzca al menos la cantidad indicada.
     *
     * Devuelve null si ninguna lo consigue.
     */
    public Solucion masBarataQueCubra(double db) {
        return soluciones.primeroQue(
                s -> s.isVerificada()
                        && s.getReduccionDb() >= db);
    }



    /**
     * Encuentra la solución verificada que más reduce
     * el ruido sin superar el presupuesto.
     *
     * El presupuesto considera el costo total,
     * incluyendo mantenimiento.
     *
     * Devuelve null si no existe una opción
     * verificada dentro del presupuesto.
     */
    public Solucion mejorConPresupuesto(double presupuesto) {

        Solucion mejor = null;

        for (Solucion s : soluciones.tomarMientras(
                x -> x.getCostoTotal() <= presupuesto)) {

            // No recomendar soluciones no verificadas.
            if (!s.isVerificada()) {
                continue;
            }

            if (mejor == null
                    || s.getReduccionDb() > mejor.getReduccionDb()) {
                mejor = s;
            }
        }

        return mejor;
    }


    /** Todas las candidatas, de la más barata a la más cara. */
    public List<Solucion> todas() {
        return soluciones.enOrden();
    }


    /**
     * Devuelve únicamente las soluciones verificadas,
     * ordenadas de menor a mayor costo total.
     */
    public List<Solucion> verificadas() {
        List<Solucion> lista = new ArrayList<>();

        for (Solucion s : soluciones.enOrden()) {
            if (s.isVerificada()) {
                lista.add(s);
            }
        }

        return lista;
    }

    /**
     * Cantidad de soluciones comprobadas
     * mediante una simulación.
     */
    public int cantidadVerificadas() {
        return verificadas().size();
    }

    public int cantidad() {
        return soluciones.tamano();
    }

    public String getHabitacionId() {
        return habitacionId;
    }

    public double getExcesoDb() {
        return excesoDb;
    }

    
    /**
     * Representación visual del árbol rojo-negro.
     *
     * Clave: costo total de la solución.
     *
     * [V] = verificada mediante simulación.
     * [E] = estimación pendiente de verificar.
     */
    public NodoVisual aNodoVisual() {
        return soluciones.aNodoVisual(s -> new NodoVisual(
                String.format("costo %.0f", s.getCostoTotal()),
                (s.isVerificada() ? "[V] " : "[E] ")
                        + nombreCorto(s.getTipo())
                        + " " + s.getId(),
                s.toString(),
                "#161616",
                "#ffffff"));
    }


    private static String nombreCorto(Solucion.Tipo tipo) {
        switch (tipo) {
            case MEJORAR_MATERIAL:
                return "Material";
            case AUMENTAR_ABSORCION:
                return "Absorcion";
            case REDUCIR_EMISION:
                return "Emision";
            default:
                return "Cambio piso";
        }
    }

    /** Solo para pruebas: verifica las reglas del rojinegro. */
    public boolean arbolValido() {
        return soluciones.cumpleInvariantes();
    }
}