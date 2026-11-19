package acustica.visual;

import acustica.estructuras.NodoVisual;
import acustica.mejora.ClasificadorDificultad;
import acustica.mejora.GeneradorSoluciones;
import acustica.mejora.PlanMejora;
import acustica.mejora.Solucion;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;
import acustica.simulacion.IndiceHabitabilidad;
import acustica.simulacion.IndicePorPisoYNivel;
import acustica.simulacion.Simulador;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dibuja los árboles como imágenes (SVG) dentro de una página HTML que se
 * abre en el navegador:
 *
 *  - Árbol AVL de cada escenario: una habitación por nodo, en verde
 *    (habitable), amarillo (tolerable) o rojo (no habitable).
 *  - Árbol rojo-negro de soluciones de cada habitación NO habitable: nodos
 *    rojos y negros, con las hojas NIL.
 *  - Árbol rojo-negro global de facilidad económica por escenario.
 *
 * Uso:
 *   GraficadorArboles g = new GraficadorArboles("Mi edificio");
 *   g.agregarEscenario("Noche", resultados);
 *   g.guardarYAbrir("out/arboles.html");
 */
public class GraficadorArboles {

    private static final int HORIZONTE_ANIOS = 10;

    /** Umbral y rango de ejemplo para las consultas del índice B+. */
    private static final double NIVEL_ALTO_DB = 55.0;
    private static final double RANGO_DESDE_DB = 30.0;
    private static final double RANGO_HASTA_DB = 55.0;

    // Medidas del dibujo (en píxeles)
    private static final double ANCHO_AVL = 168;
    private static final double ANCHO_RN = 128;
    private static final double ANCHO_BMAS = 170;
    private static final double ANCHO_NIL = 34;
    private static final double ALTO_NODO = 46;
    private static final double ALTO_NIL = 22;
    private static final double SEPARACION = 18;
    private static final double ALTO_NIVEL = 96;
    private static final double MARGEN = 30;

    private final String titulo;
    private final StringBuilder cuerpo = new StringBuilder();

    public GraficadorArboles(String titulo) {
        this.titulo = titulo;
    }

    
    /**
     * Versión anterior: conserva compatibilidad
     * con los escenarios sin simulador.
     */
    public void agregarEscenario(
            String nombre,
            List<ResultadoHabitabilidad> resultados) {

        agregarEscenario(nombre, resultados, null, null);
    }

    /**
     * Agrega un escenario y genera sus árboles.
     *
     * Si recibe un simulador y un horario,
     * verifica las mejoras de materiales.
     */
    public void agregarEscenario(
            String nombre,
            List<ResultadoHabitabilidad> resultados,
            Simulador simulador,
            String horario) {

        if ((simulador == null) != (horario == null)) {
            throw new IllegalArgumentException(
                    "Debes proporcionar simulador y horario juntos");
        }

        IndiceHabitabilidad indice = new IndiceHabitabilidad(resultados);

        List<ResultadoHabitabilidad> ordenados = new ArrayList<>(indice.ordenados());
        Collections.reverse(ordenados); // de mayor a menor exceso (peor caso primero)

        long noHabitables = ordenados.stream()
                .filter(r -> r.getEstado() == EstadoHabitabilidad.NO_HABITABLE).count();

        cuerpo.append("<section>\n<h2>").append(esc(nombre)).append("</h2>\n");
        cuerpo.append("<p class=\"resumen\">").append(indice.tamano()).append(" habitaciones, ")
                .append(noHabitables).append(" NO habitable(s). Altura del AVL: ")
                .append(indice.altura()).append(".</p>\n");

        cuerpo.append("<h3>Árbol AVL: el edificio ordenado por exceso de ruido</h3>\n");
        cuerpo.append("<p class=\"leyenda\">")
                .append(chip("#2e9e5b", "HABITABLE")).append(chip("#f2c230", "TOLERABLE"))
                .append(chip("#d64545", "NO HABITABLE")).append(chip("#8a8f98", "NO EVALUABLE"))
                .append("<span class=\"nota\">Cada nodo es una habitación: arriba el nombre, abajo "
                        + "el nivel que recibe y su exceso (nivel comparado &minus; límite), no el nivel "
                        + "absoluto: un nivel alto puede cumplir su límite y uno bajo incumplirlo, el "
                        + "exceso sí dice si se pasa. Izquierda = menos exceso, derecha = más exceso, "
                        + "así el peor caso queda a la derecha. En alto impacto el exceso se mide en el "
                        + "vecino más afectado; el tooltip lo detalla. «Sin límite» y «no evaluable» valen "
                        + "&minus;infinito: caen a la izquierda y nunca son peor caso. La insignia azul "
                        + "es el factor de equilibrio (fe).</span></p>\n");
        cuerpo.append("<div class=\"arbol\">")
                .append(dibujar(indice.aNodoVisual(), ANCHO_AVL)).append("</div>\n");

        // ==========================================
        // ÁRBOL B+ POR PISO Y NIVEL
        // ==========================================

        IndicePorPisoYNivel porPisoYNivel =
                new IndicePorPisoYNivel(resultados);

        cuerpo.append("<h3>Árbol B+ por piso y nivel de ruido</h3>\n");
        cuerpo.append("<p class=\"leyenda\">")
                .append(chip("#3b5b92", "Nodo interno"))
                .append(chip("#2e7d6b", "Hoja"))
                .append("<span class=\"nota\">Clave (piso, nivel recibido, habitación): "
                        + "el nivel es el que recibe el espacio, no el del vecino de una "
                        + "actividad de alto impacto. El id desempata habitaciones del mismo "
                        + "piso con igual nivel. Las hojas están enlazadas de izquierda a "
                        + "derecha, así las consultas por rango recorren pocas hojas.</span></p>\n");

        if (porPisoYNivel.estaVacio()) {

            cuerpo.append("<p class=\"nota\">No hay habitaciones para indexar.</p>\n");

        } else {

            cuerpo.append("<p class=\"resumen\">")
                    .append(porPisoYNivel.tamano())
                    .append(" habitaciones indexadas. Altura del B+: ")
                    .append(porPisoYNivel.altura())
                    .append(". Invariantes: ")
                    .append(porPisoYNivel.cumpleInvariantes() ? "válidas" : "INVÁLIDAS")
                    .append(".</p>\n");

            cuerpo.append("<div class=\"arbol\">")
                    .append(dibujarBMas(porPisoYNivel.aNodoVisual(), ANCHO_BMAS))
                    .append("</div>\n");

            cuerpo.append("<h4>Consultas del índice B+</h4>\n");
            cuerpo.append("<p class=\"nota\">")
                    .append("Umbral de ejemplo: ")
                    .append(String.format("%.1f", NIVEL_ALTO_DB))
                    .append(" dB; rango de ejemplo: [")
                    .append(String.format("%.1f", RANGO_DESDE_DB))
                    .append(", ")
                    .append(String.format("%.1f", RANGO_HASTA_DB))
                    .append("] dB.</p>\n");
            cuerpo.append("<ul>\n");

            for (int piso : porPisoYNivel.pisos()) {

                List<ResultadoHabitabilidad> delPiso =
                        porPisoYNivel.habitacionesDePiso(piso);
                List<ResultadoHabitabilidad> altos =
                        porPisoYNivel.queSuperanNivel(piso, NIVEL_ALTO_DB);
                List<ResultadoHabitabilidad> enRango =
                        porPisoYNivel.enRangoDeNiveles(
                                piso, RANGO_DESDE_DB, RANGO_HASTA_DB);

                cuerpo.append("<li><strong>Piso ")
                        .append(piso)
                        .append("</strong> (")
                        .append(delPiso.size())
                        .append(" habitaciones)<br>\n")
                        .append("Todas, por nivel: ")
                        .append(resumenNiveles(delPiso))
                        .append("<br>\n")
                        .append("Superan ")
                        .append(String.format("%.1f", NIVEL_ALTO_DB))
                        .append(" dB: ")
                        .append(resumenNiveles(altos))
                        .append("<br>\n")
                        .append("Entre ")
                        .append(String.format("%.1f", RANGO_DESDE_DB))
                        .append(" y ")
                        .append(String.format("%.1f", RANGO_HASTA_DB))
                        .append(" dB: ")
                        .append(resumenNiveles(enRango))
                        .append("</li>\n");
            }

            cuerpo.append("</ul>\n");
        }

        // Un árbol rojo-negro global por escenario/horario.
        ClasificadorDificultad clasificador =
                new ClasificadorDificultad();

        // Construimos los dibujos individuales por separado para mostrarlos después.
        StringBuilder individuales = new StringBuilder();
        boolean hay = false;
        for (ResultadoHabitabilidad r : ordenados) {

            PlanMejora plan;
            if (simulador != null) {
                plan = GeneradorSoluciones.generar(
                        r, HORIZONTE_ANIOS, simulador, horario);
            } else {
                plan = GeneradorSoluciones.generar(
                        r, HORIZONTE_ANIOS);
            }

            if (plan == null) {
                continue;
            }

            // Agregar la habitación NO_HABITABLE al árbol global.
            // Reutilizamos el plan que ya fue generado.
            clasificador.registrar(r, plan);

            if (!hay) {
                individuales.append("<h3>Árboles rojo-negro individuales: soluciones por habitación (desplegables)</h3>\n");
                individuales.append("<p class=\"leyenda\">").append(chip("#c62828", "Nodo rojo"))
                        .append(chip("#161616", "Nodo negro / NIL"))
                        .append("<span class=\"nota\">Ordenado por costo total a ")
                        .append(HORIZONTE_ANIOS)
                        .append(" años (incluye mantenimiento): "
                                + "las más baratas a la izquierda. "
                                + "[V] significa verificada mediante simulación "
                                + "y [E] significa estimada. "
                                + "Una solución verificada no necesariamente "
                                + "cubre todo el exceso acústico. "
                                + "Pase el mouse sobre un nodo para "
                                + "ver sus detalles.</span></p>\n");
                hay = true;
            }
            
            individuales.append("<details class=\"detalle-habitacion\">\n")
                    .append("<summary>")
                    .append(esc(r.getHabitacion().getNombre()))
                    .append(" &mdash; exceso ")
                    .append(String.format("%.1f", plan.getExcesoDb()))
                    .append(" dB, ")
                    .append(plan.cantidad())
                    .append(" candidatas, ")
                    .append(plan.cantidadVerificadas())
                    .append(" verificadas</summary>\n");

            individuales.append("<div class=\"arbol\">")
                    .append(dibujar(plan.aNodoVisual(), ANCHO_RN))
                    .append("</div>\n");

            // Buscar la solución más económica que fue
            // verificada y que cubre el exceso.
            Solucion suficiente = plan.masBarataQueCubra();

            if (suficiente != null) {

                individuales.append("<p class=\"nota\">")
                        .append("<strong>Solución verificada "
                                + "que cubre el exceso:</strong> ")
                        .append(esc(suficiente.getDescripcion()))
                        .append(" (reducción: ")
                        .append(String.format(
                                "%.1f", suficiente.getReduccionDb()))
                        .append(" dB).</p>\n");

            } else {

                individuales.append("<p class=\"nota\">")
                        .append("Ninguna solución verificada "
                                + "cubre el exceso por sí sola.")
                        .append("</p>\n");
            }

            individuales.append("</details>\n");
        }

        // ==========================================
        // ÁRBOL ROJO-NEGRO GLOBAL
        // ==========================================

        cuerpo.append(
                "<h3>Árbol rojo-negro global: ranking de facilidad</h3>\n");
        cuerpo.append("<p class=\"leyenda\">")
                .append(chip("#c62828", "Nodo rojo"))
                .append(chip("#161616", "Nodo negro / NIL"))
                .append("<span class=\"nota\">"
                        + "Cada nodo representa una habitación NO HABITABLE. "
                        + "El árbol se ordena por el costo total de su "
                        + "mejor solución verificada que cubra el exceso. "
                        + "Los empates se resuelven por ID. "
                        + "Las habitaciones sin solución suficiente "
                        + "verificada quedan al final. "
                        + "El mínimo representa la habitación más fácil "
                        + "económicamente, no necesariamente la primera "
                        + "intervención recomendada."
                        + "</span></p>\n");

        if (clasificador.cantidad() == 0) {

            cuerpo.append(
                    "<p class=\"nota\">No hay habitaciones "
                            + "NO HABITABLES en este escenario.</p>\n");

        } else {

            // Dibujar el árbol rojo-negro global.
            cuerpo.append("<div class=\"arbol\">")
                    .append(dibujar(
                            clasificador.aNodoVisual(), ANCHO_RN))
                    .append("</div>\n");

            // Mostrar el recorrido inorden como ranking.
            cuerpo.append("<h4>Ranking de facilidad económica</h4>\n");
            cuerpo.append("<ol>\n");

            for (ClasificadorDificultad.Entrada entrada
                    : clasificador.ranking()) {

                cuerpo.append("<li><strong>")
                        .append(esc(entrada.getNombreHabitacion()))
                        .append("</strong> (ID ")
                        .append(esc(entrada.getHabitacionId()))
                        .append(") &mdash; exceso ")
                        .append(String.format(
                                "%.1f", entrada.getExcesoDb()))
                        .append(" dB: ");

                if (entrada.requiereCombinar()) {

                    if (entrada.tieneVerificadas()) {
                        cuerpo.append(
                                "Requiere combinar: ninguna solución "
                                        + "verificada cubre el exceso.");
                    } else {
                        cuerpo.append(
                                "Requiere combinar o verificar: "
                                        + "no hay soluciones verificadas.");
                    }

                } else {

                    cuerpo.append("Costo total ")
                            .append(String.format(
                                    "%.2f", entrada.getCostoTotal()))
                            .append(" &mdash; solución suficiente "
                                    + "verificada: ")
                            .append(esc(entrada
                                    .getMejorSolucion().getId()));
                }

                cuerpo.append("</li>\n");
            }

            cuerpo.append("</ol>\n");
        }

        // Los árboles de soluciones se muestran DESPUÉS del ranking global.
        if (hay) {
            cuerpo.append(individuales);
        }

        cuerpo.append("</section>\n");
    }

    /** Escribe la página HTML en el archivo indicado (crea las carpetas si hacen falta). */
    public Path guardar(String ruta) throws IOException {
        Path archivo = Paths.get(ruta);
        if (archivo.getParent() != null) {
            Files.createDirectories(archivo.getParent());
        }
        String html = "<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n<meta charset=\"UTF-8\">\n"
                + "<title>" + esc(titulo) + " - Árboles</title>\n<style>\n"
                + "body{font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;margin:28px;"
                + "background:#f4f5f7;color:#1b1f24}\n"
                + "h1{margin:0 0 4px}h2{margin-top:36px;padding-top:12px;border-top:2px solid #cfd4dc}"
                + "h3{margin:22px 0 4px}h4{margin:18px 0 4px}\n"
                + ".resumen,.nota{color:#5a6270;font-size:14px}\n"
                + ".leyenda{display:flex;flex-wrap:wrap;gap:8px;align-items:center;margin:6px 0 10px}\n"
                + ".chip{display:inline-flex;align-items:center;gap:6px;background:#fff;border:1px solid #d9dde3;"
                + "border-radius:999px;padding:3px 10px;font-size:13px}\n"
                + ".chip i{width:12px;height:12px;border-radius:50%;display:inline-block}\n"
                + ".arbol{overflow-x:auto;background:#fff;border:1px solid #d9dde3;border-radius:12px;padding:8px}\n"
                + ".detalle-habitacion{margin:10px 0;border:1px solid #d9dde3;border-radius:10px;background:#fff;padding:12px}\n"
                + ".detalle-habitacion summary{cursor:pointer;font-weight:600;padding:5px 2px}\n"
                + ".detalle-habitacion[open] summary{margin-bottom:12px}\n"
                + "svg text{font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif}\n"
                + "</style>\n</head>\n<body>\n<h1>" + esc(titulo) + "</h1>\n"
                + "<p class=\"nota\">Árboles generados por la simulación de habitabilidad acústica.</p>\n"
                + cuerpo + "</body>\n</html>\n";
        Files.write(archivo, html.getBytes(StandardCharsets.UTF_8));
        return archivo;
    }

    /** Guarda la página y trata de abrirla en el navegador; si no se puede, avisa la ruta. */
    public void guardarYAbrir(String ruta) {
        try {
            Path archivo = guardar(ruta);
            System.out.println("\nArboles dibujados en: " + archivo.toAbsolutePath());
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(archivo.toUri());
                    return;
                }
            } catch (Exception | Error e) {
                // sin navegador disponible: se deja la ruta impresa arriba
            }
            System.out.println("Abra ese archivo con su navegador para verlos.");
        } catch (IOException e) {
            System.out.println("No se pudo guardar el dibujo de los arboles: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Dibujo SVG
    // ------------------------------------------------------------------

    /** Posición calculada de un nodo. */
    private static class Pos {
        double x;           // centro horizontal
        double y;           // borde superior
        double anchoIzq;    // espacio reservado a la izquierda del nodo
        double anchoDer;    // espacio reservado a la derecha del nodo
        double anchoTotal;  // ancho del subárbol (solo árboles con N hijos)
    }

    private String dibujar(NodoVisual raiz, double anchoNodo) {
        if (raiz == null) {
            return "<p class=\"nota\">(árbol vacío)</p>";
        }
        Map<NodoVisual, Pos> pos = new HashMap<>();
        medir(raiz, anchoNodo, pos);
        Pos pr = pos.get(raiz);
        colocar(raiz, MARGEN, 0, pos);

        double ancho = pr.anchoIzq + pr.anchoDer + 2 * MARGEN;
        double alto = (profundidad(raiz) - 1) * ALTO_NIVEL + ALTO_NODO + 2 * MARGEN;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(java.util.Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.0f\" height=\"%.0f\" viewBox=\"0 0 %.0f %.0f\">\n",
                ancho, alto, ancho, alto));
        aristas(raiz, pos, sb, anchoNodo);
        nodos(raiz, pos, sb, anchoNodo);
        sb.append("</svg>");
        return sb.toString();
    }

    /**
     * Dibuja un árbol con N hijos por nodo (el B+). A diferencia del AVL y del
     * rojo-negro, un nodo interno del B+ puede tener más de dos hijos; el
     * padre se centra sobre el conjunto de sus hijos y cada subárbol reserva
     * su propio ancho para que no se solapen.
     */
    private String dibujarBMas(NodoVisual raiz, double anchoNodo) {
        if (raiz == null) {
            return "<p class=\"nota\">(árbol vacío)</p>";
        }
        Map<NodoVisual, Pos> pos = new HashMap<>();
        double anchoArbol = medirNario(raiz, anchoNodo, pos);
        colocarNario(raiz, MARGEN, 0, pos);

        double ancho = anchoArbol + 2 * MARGEN;
        double alto = (profundidadNario(raiz) - 1) * ALTO_NIVEL + ALTO_NODO + 2 * MARGEN;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(java.util.Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.0f\" height=\"%.0f\" viewBox=\"0 0 %.0f %.0f\">\n",
                ancho, alto, ancho, alto));
        aristasNarias(raiz, pos, sb);
        nodosNarios(raiz, pos, sb, anchoNodo);
        sb.append("</svg>");
        return sb.toString();
    }

    /** Ancho de un nodo B+: crece con el texto porque puede tener varias claves. */
    private double anchoDeBMas(NodoVisual n, double anchoNodo) {
        if (n.isNil()) {
            return ANCHO_NIL;
        }
        int largo = n.getLinea1() == null ? 0 : n.getLinea1().length();
        double porTexto = 22 + 6.6 * Math.min(largo, 42);
        return Math.max(anchoNodo, porTexto);
    }

    /** Mide el subárbol de un árbol N-ario y deja el ancho total en cada Pos. */
    private double medirNario(NodoVisual n, double anchoNodo, Map<NodoVisual, Pos> pos) {
        Pos p = new Pos();
        pos.put(n, p);
        double propio = anchoDeBMas(n, anchoNodo) + SEPARACION;
        double hijos = 0;
        for (NodoVisual hijo : n.getHijos()) {
            hijos += medirNario(hijo, anchoNodo, pos);
        }
        p.anchoTotal = Math.max(propio, hijos);
        p.anchoIzq = p.anchoTotal / 2;
        p.anchoDer = p.anchoTotal / 2;
        return p.anchoTotal;
    }

    /** Coloca el padre centrado sobre sus hijos, de izquierda a derecha. */
    private void colocarNario(NodoVisual n, double inicio, int nivel, Map<NodoVisual, Pos> pos) {
        Pos p = pos.get(n);
        p.x = inicio + p.anchoTotal / 2;
        p.y = MARGEN + nivel * ALTO_NIVEL;

        double hijos = 0;
        for (NodoVisual hijo : n.getHijos()) {
            hijos += pos.get(hijo).anchoTotal;
        }

        double cursor = p.x - hijos / 2;
        for (NodoVisual hijo : n.getHijos()) {
            colocarNario(hijo, cursor, nivel + 1, pos);
            cursor += pos.get(hijo).anchoTotal;
        }
    }

    private int profundidadNario(NodoVisual n) {
        if (n == null) {
            return 0;
        }
        int maxima = 0;
        for (NodoVisual hijo : n.getHijos()) {
            maxima = Math.max(maxima, profundidadNario(hijo));
        }
        return 1 + maxima;
    }

    private void aristasNarias(NodoVisual n, Map<NodoVisual, Pos> pos, StringBuilder sb) {
        Pos p = pos.get(n);
        for (NodoVisual hijo : n.getHijos()) {
            Pos h = pos.get(hijo);
            sb.append(String.format(java.util.Locale.ROOT,
                    "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"1.6\"/>\n",
                    p.x, p.y + altoDe(n), h.x, h.y, "#5d6470"));
            aristasNarias(hijo, pos, sb);
        }
    }

    private void nodosNarios(NodoVisual n, Map<NodoVisual, Pos> pos, StringBuilder sb, double anchoNodo) {
        Pos p = pos.get(n);
        dibujarNodo(n, p, sb, anchoDeBMas(n, anchoNodo), altoDe(n), 42, 30);
        for (NodoVisual hijo : n.getHijos()) {
            nodosNarios(hijo, pos, sb, anchoNodo);
        }
    }

    private double anchoDe(NodoVisual n, double anchoNodo) {
        return n.isNil() ? ANCHO_NIL : anchoNodo;
    }

    private double altoDe(NodoVisual n) {
        return n.isNil() ? ALTO_NIL : ALTO_NODO;
    }

    /** Calcula cuánto espacio necesita cada subárbol a cada lado de su raíz. */
    private double medir(NodoVisual n, double anchoNodo, Map<NodoVisual, Pos> pos) {
        Pos p = new Pos();
        pos.put(n, p);
        double propio = anchoDe(n, anchoNodo) + SEPARACION;

        if (n.getIzq() == null && n.getDer() == null) {
            p.anchoIzq = propio / 2;
            p.anchoDer = propio / 2;
            return propio;
        }
        double izq = n.getIzq() != null ? medir(n.getIzq(), anchoNodo, pos) : propio / 2;
        double der = n.getDer() != null ? medir(n.getDer(), anchoNodo, pos) : propio / 2;
        // El propio nodo tiene que caber dentro de su espacio, a ambos lados del centro
        izq = Math.max(izq, propio / 2);
        der = Math.max(der, propio / 2);
        p.anchoIzq = izq;
        p.anchoDer = der;
        return izq + der;
    }

    /** Pone cada nodo en su lugar: el padre queda justo en el borde entre sus dos subárboles. */
    private void colocar(NodoVisual n, double inicio, int nivel, Map<NodoVisual, Pos> pos) {
        Pos p = pos.get(n);
        p.x = inicio + p.anchoIzq;
        p.y = MARGEN + nivel * ALTO_NIVEL;
        if (n.getIzq() != null) {
            colocar(n.getIzq(), inicio, nivel + 1, pos);
        }
        if (n.getDer() != null) {
            colocar(n.getDer(), p.x, nivel + 1, pos);
        }
    }

    private int profundidad(NodoVisual n) {
        if (n == null) {
            return 0;
        }
        return 1 + Math.max(profundidad(n.getIzq()), profundidad(n.getDer()));
    }

    private void aristas(NodoVisual n, Map<NodoVisual, Pos> pos, StringBuilder sb, double anchoNodo) {
        Pos p = pos.get(n);
        for (NodoVisual hijo : new NodoVisual[] {n.getIzq(), n.getDer()}) {
            if (hijo == null) {
                continue;
            }
            Pos h = pos.get(hijo);
            sb.append(String.format(java.util.Locale.ROOT,
                    "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"1.6\"/>\n",
                    p.x, p.y + altoDe(n), h.x, h.y, hijo.isNil() ? "#9aa0a8" : "#5d6470"));
            aristas(hijo, pos, sb, anchoNodo);
        }
    }

    private void nodos(NodoVisual n, Map<NodoVisual, Pos> pos, StringBuilder sb, double anchoNodo) {
        Pos p = pos.get(n);
        dibujarNodo(n, p, sb, anchoDe(n, anchoNodo), altoDe(n), 22, 26);

        if (n.getIzq() != null) {
            nodos(n.getIzq(), pos, sb, anchoNodo);
        }
        if (n.getDer() != null) {
            nodos(n.getDer(), pos, sb, anchoNodo);
        }
    }

    /** Dibuja la caja, sus textos y la insignia de un nodo ya posicionado. */
    private void dibujarNodo(NodoVisual n, Pos p, StringBuilder sb, double w, double h,
            int maxLinea1, int maxLinea2) {
        double x = p.x - w / 2;
        java.util.Locale l = java.util.Locale.ROOT;

        sb.append("<g><title>").append(esc(n.getDetalle())).append("</title>\n");
        sb.append(String.format(l,
                "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"%d\" fill=\"%s\" "
                        + "stroke=\"rgba(0,0,0,0.35)\" stroke-width=\"1.5\"/>\n",
                x, p.y, w, h, n.isNil() ? 4 : 10, n.getColor()));
        if (n.isNil()) {
            sb.append(String.format(l,
                    "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"10\" font-weight=\"700\" "
                            + "fill=\"%s\">NIL</text>\n", p.x, p.y + 15, n.getColorTexto()));
        } else {
            double y1 = n.getLinea2() == null ? p.y + h / 2 + 5 : p.y + 20;
            sb.append(String.format(l,
                    "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"13\" font-weight=\"700\" "
                            + "fill=\"%s\">%s</text>\n", p.x, y1, n.getColorTexto(),
                    esc(cortar(n.getLinea1(), maxLinea1))));
            if (n.getLinea2() != null) {
                sb.append(String.format(l,
                        "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"11\" "
                                + "fill=\"%s\">%s</text>\n", p.x, p.y + 36, n.getColorTexto(),
                        esc(cortar(n.getLinea2(), maxLinea2))));
            }
        }
        if (n.getInsignia() != null) {
            double ancho = 8 + 6.2 * n.getInsignia().length();
            sb.append(String.format(l,
                    "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"16\" rx=\"8\" fill=\"#2f6fe4\" "
                            + "stroke=\"#ffffff\" stroke-width=\"1.5\"/>\n"
                            + "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"10\" "
                            + "font-weight=\"700\" fill=\"#ffffff\">%s</text>\n",
                    x + w - ancho + 6, p.y - 9, ancho, x + w - ancho / 2 + 6, p.y + 2,
                    esc(n.getInsignia())));
        }
        sb.append("</g>\n");
    }

    // ------------------------------------------------------------------
    // Utilidades de texto
    // ------------------------------------------------------------------

    /** Lista compacta de habitaciones con su nivel recibido para el HTML. */
    private static String resumenNiveles(List<ResultadoHabitabilidad> resultados) {
        if (resultados.isEmpty()) {
            return "ninguna";
        }
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < resultados.size(); i++) {
            if (i > 0) {
                texto.append(", ");
            }
            ResultadoHabitabilidad r = resultados.get(i);
            texto.append(esc(r.getHabitacion().getNombre()))
                    .append(" (")
                    .append(String.format("%.1f", r.getNivelRecibido()))
                    .append(" dB)");
        }
        return texto.toString();
    }

    private static String chip(String color, String texto) {
        return "<span class=\"chip\"><i style=\"background:" + color + "\"></i>" + esc(texto) + "</span>";
    }

    private static String cortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }

    private static String esc(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
