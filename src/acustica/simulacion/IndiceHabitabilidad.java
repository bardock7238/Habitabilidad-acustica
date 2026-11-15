package acustica.simulacion;

import java.util.Comparator;
import java.util.List;

import acustica.dominio.Habitacion;
import acustica.estructuras.ArbolAVL;
import acustica.estructuras.NodoVisual;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;

/**
 * Índice de los resultados de UNA ejecución del Simulador, guardado
 * en un árbol AVL ordenado por exceso de ruido
 * (nivelFinal - limiteAplicado, de menor a mayor).
 *
 * Con eso, el peor caso es el máximo del árbol y las habitaciones que no
 * cumplen (exceso > 0) se obtienen sin recorrer todas. Si dos habitaciones
 * tienen el mismo exceso, el desempate es el id de la habitación.
 *
 * Crear un índice por horario: los resultados de "dia" y "noche" son listas
 * distintas.
 */
public class IndiceHabitabilidad {

    /** Orden: exceso ascendente; empate -> id de habitación. */
    private static final Comparator<ResultadoHabitabilidad> POR_EXCESO =
            Comparator.comparingDouble(ResultadoHabitabilidad::getExceso)
                    .thenComparing(r -> r.getHabitacion().getId());

    private final ArbolAVL<ResultadoHabitabilidad> arbol = new ArbolAVL<>(POR_EXCESO);

    public IndiceHabitabilidad(List<ResultadoHabitabilidad> resultados) {
        for (ResultadoHabitabilidad r : resultados) {
            arbol.insertar(r);
        }
    }

    /** Habitación con mayor exceso (el peor caso), o null si no hay resultados. */
    public ResultadoHabitabilidad peorCaso() {
        return arbol.maximo();
    }

    /** Habitación con más margen (la más holgada), o null si no hay resultados. */
    public ResultadoHabitabilidad mejorCaso() {
        return arbol.minimo();
    }

    /** Resultados con exceso mayor a dB, de menor a mayor exceso. */
    public List<ResultadoHabitabilidad> conExcesoMayorA(double dB) {
        // Resultado "centinela" con exceso = dB y un id que ordena de último
        // (\uffff), para que los empates en dB queden fuera (mayor ESTRICTO).
        Habitacion marca = new Habitacion("\uffff", "", "", 1, 1, 1);
        ResultadoHabitabilidad limite =
                new ResultadoHabitabilidad(marca, dB, 0.0, EstadoHabitabilidad.HABITABLE);
        return arbol.mayoresQue(limite);
    }

    /** Estructura del AVL lista para dibujar (verde = habitable, amarillo = tolerable, rojo = no habitable). */
    public NodoVisual aNodoVisual() {
        return arbol.aNodoVisual(r -> {
            String exceso = Double.isInfinite(r.getExceso()) ? "sin limite"
                    : String.format("%.1f dB (%+.1f)", r.getNivelFinal(), r.getExceso());
            String color;
            String texto = "#ffffff";
            switch (r.getEstado()) {
                case HABITABLE:
                    color = "#2e9e5b";
                    break;
                case TOLERABLE:
                    color = "#f2c230";
                    texto = "#2b2b2b";
                    break;
                case NO_HABITABLE:
                    color = "#d64545";
                    break;
                default:
                    color = "#8a8f98";
                    break;
            }
            return new NodoVisual(r.getHabitacion().getNombre(), exceso, r.toString(), color, texto);
        });
    }

    /** Todos los resultados ordenados por exceso ascendente. */
    public List<ResultadoHabitabilidad> ordenados() {
        return arbol.enOrden();
    }

    public int tamano() {
        return arbol.tamano();
    }

    public int altura() {
        return arbol.altura();
    }

    public boolean estaBalanceado() {
        return arbol.estaBalanceado();
    }
}