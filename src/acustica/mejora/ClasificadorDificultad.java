
package acustica.mejora;

import acustica.estructuras.ArbolRojinegro;
import acustica.estructuras.NodoVisual;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ranking económico de las habitaciones NO_HABITABLE de un horario.
 * No determina el orden recomendado de intervención
 * (eso corresponde a etapa 3).
 */
public final class ClasificadorDificultad {

    private final ArbolRojinegro<Entrada> arbol =
            new ArbolRojinegro<>((a, b) -> {
                int porCosto = Double.compare(
                        a.getCostoTotal(), b.getCostoTotal());

                return porCosto != 0
                        ? porCosto
                        : a.getHabitacionId()
                                .compareTo(b.getHabitacionId());
            });

    private final Set<String> idsRegistrados = new HashSet<>();

    /**
     * Agrega una habitación y su plan, correspondientes
     * al mismo horario.
     */
    public void registrar(
            ResultadoHabitabilidad resultado,
            PlanMejora plan) {

        if (resultado == null) {
            throw new IllegalArgumentException(
                    "El resultado no puede ser null");
        }

        if (resultado.getEstado()
                != EstadoHabitabilidad.NO_HABITABLE) {
            return;
        }

        String id = resultado.getHabitacion().getId();

        if (plan == null
                || !id.equals(plan.getHabitacionId())) {
            throw new IllegalArgumentException(
                    "El plan debe pertenecer a la habitación " + id);
        }

        if (!idsRegistrados.add(id)) {
            throw new IllegalArgumentException(
                    "Habitación repetida en el ranking: " + id);
        }

        Solucion suficiente = plan.masBarataQueCubra();

        Entrada entrada = new Entrada(
                resultado,
                suficiente,
                plan.cantidadVerificadas() > 0);

        arbol.insertar(entrada);
    }

    /** Habitación económicamente más fácil de resolver. */
    public Entrada masFacil() {
        return arbol.minimo();
    }

    /**
     * Habitaciones de menor a mayor costo.
     * Las que requieren combinar quedan al final.
     */
    public List<Entrada> ranking() {
        return arbol.enOrden();
    }

    public int cantidad() {
        return arbol.tamano();
    }

    public boolean arbolValido() {
        return arbol.cumpleInvariantes();
    }

    /** Representación para el HTML. */
    public NodoVisual aNodoVisual() {
        return arbol.aNodoVisual(entrada -> {

            String estado;

            if (!entrada.requiereCombinar()) {
                estado = "Solución suficiente verificada: "
                        + entrada.getMejorSolucion();

            } else if (entrada.tieneVerificadas()) {
                estado = "Requiere combinar: ninguna solución "
                        + "verificada cubre el exceso";

            } else {
                estado = "Requiere combinar o verificar: "
                        + "no hay soluciones verificadas";
            }

            String detalle = entrada.getNombreHabitacion()
                    + " | exceso "
                    + String.format("%.1f",
                            entrada.getExcesoDb())
                    + " dB | " + estado;

            return new NodoVisual(
                    entrada.getHabitacionId(),
                    entrada.requiereCombinar()
                            ? "Combinar"
                            : String.format("total %.0f",
                                    entrada.getCostoTotal()),
                    detalle,
                    "#161616",
                    "#ffffff");
        });
    }

    /** Información de una habitación en el árbol global. */
    public static final class Entrada {

        private final ResultadoHabitabilidad resultado;
        private final Solucion mejorSolucion;
        private final boolean tieneVerificadas;

        private Entrada(
                ResultadoHabitabilidad resultado,
                Solucion mejorSolucion,
                boolean tieneVerificadas) {

            this.resultado = resultado;
            this.mejorSolucion = mejorSolucion;
            this.tieneVerificadas = tieneVerificadas;
        }

        public String getHabitacionId() {
            return resultado.getHabitacion().getId();
        }

        public String getNombreHabitacion() {
            return resultado.getHabitacion().getNombre();
        }

        public double getExcesoDb() {
            return resultado.getExceso();
        }

        public Solucion getMejorSolucion() {
            return mejorSolucion;
        }

        public boolean tieneVerificadas() {
            return tieneVerificadas;
        }

        public boolean requiereCombinar() {
            return mejorSolucion == null;
        }

        /**
         * Infinito pone al final a las habitaciones
         * sin solución suficiente verificada.
         */
        public double getCostoTotal() {
            return mejorSolucion == null
                    ? Double.POSITIVE_INFINITY
                    : mejorSolucion.getCostoTotal();
        }
    }
}
