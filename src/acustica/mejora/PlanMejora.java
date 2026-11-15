package acustica.mejora;

import java.util.List;

import acustica.estructuras.ArbolRojinegro;
import acustica.estructuras.NodoVisual;

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

    /** La solución más barata de todas, sin importar si alcanza. */
    public Solucion masBarata() {
        return soluciones.minimo();
    }

    /** La solución más barata que por sí sola reduce al menos el exceso; null si ninguna alcanza. */
    public Solucion masBarataQueCubra() {
        return masBarataQueCubra(excesoDb);
    }

    /** La solución más barata que reduce al menos db; null si ninguna alcanza. */
    public Solucion masBarataQueCubra(double db) {
        return soluciones.primeroQue(s -> s.getReduccionDb() >= db);
    }

   /**
     * La solución que más reduce entre las que no superan el presupuesto
     * (costo TOTAL a largo plazo, incluyendo mantenimiento); null si ninguna
     * entra en el presupuesto.
     */
    public Solucion mejorConPresupuesto(double presupuesto) {
        Solucion mejor = null;
        for (Solucion s : soluciones.tomarMientras(x -> x.getCostoTotal() <= presupuesto)) {
            if (mejor == null || s.getReduccionDb() > mejor.getReduccionDb()) {
                mejor = s;
            }
        }
        return mejor;
    }

    /** Todas las candidatas, de la más barata a la más cara. */
    public List<Solucion> todas() {
        return soluciones.enOrden();
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

    /** Estructura del rojo-negro lista para dibujar (clave: costo total). */
    public NodoVisual aNodoVisual() {
        return soluciones.aNodoVisual(s -> new NodoVisual(
                String.format("costo %.0f", s.getCostoTotal()),
                nombreCorto(s.getTipo()) + " " + s.getId(),
                s.toString(),
                "#161616", "#ffffff"));
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