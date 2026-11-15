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
 * en un árbol AVL ordenado por exceso de ruido.
 *
 * QUÉ ES UN NODO: un {@link ResultadoHabitabilidad}, es decir una habitación
 * con su nivel recibido, el nivel contra el que se midió el exceso y su
 * estado. El AVL no guarda habitaciones sueltas: guarda el resultado completo,
 * así que de cada nodo se pueden leer el recibido, el límite y el estado.
 *
 * POR QUÉ SE ORDENA POR EXCESO Y NO POR NIVEL: el nivel absoluto no dice si
 * una habitación incumple. Una sala con 58 dB puede cumplir su límite de 65 y
 * un dormitorio con 42 dB incumplir el de 30. El exceso
 * ({@link ResultadoHabitabilidad#getExceso()}, nivel comparado - límite) sí:
 * positivo = incumple, negativo = margen. Ordenar por exceso deja el peor caso
 * en el máximo del árbol (O(log n)) y permite pedir solo los que superan un
 * umbral sin recorrer todas las habitaciones.
 *
 * LOS DOS -INFINITO: un resultado NO_EVALUABLE (no hay límite definido) y uno
 * con límite +infinito (nada que exigir) valen ambos -infinito, así que caen
 * al extremo izquierdo del árbol y nunca compiten por el peor caso. Significan
 * cosas distintas: el primero no se pudo evaluar, el segundo no tiene límite
 * que cumplir. Dentro de ese grupo el orden lo decide solo el id, no el ruido;
 * los nodos de la izquierda no son "los más tranquilos".
 *
 * Si dos habitaciones tienen el mismo exceso, el desempate es el id. Crear un
 * índice por horario: los resultados de "dia" y "noche" son listas distintas.
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
                ResultadoHabitabilidad.propio(marca, dB, 0.0, EstadoHabitabilidad.HABITABLE);
        return arbol.mayoresQue(limite);
    }

    /**
     * Estructura del AVL lista para dibujar. Cada nodo es una habitación y
     * muestra: nombre arriba, "nivel recibido · exceso" abajo; el tooltip
     * lleva el resultado completo (incluido el nivel del vecino cuando aplica).
     * Color: verde = habitable, amarillo = tolerable, rojo = no habitable,
     * gris = no evaluable.
     */
    public NodoVisual aNodoVisual() {
        return arbol.aNodoVisual(r -> {
            String exceso = String.format("rec %.1f · %s", r.getNivelRecibido(), r.etiquetaExceso());
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