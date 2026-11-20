package acustica.visual;

import acustica.normativa.EstadoHabitabilidad;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Representacion ESQUEMATICA del edificio comunal. No es un plano arquitectonico.
 * Usa exclusivamente los resultados previamente preparados por DatosVistaEdificio.
 */
public final class PlanoEdificioSVG {
    private static final int ANCHO = 940;
    private static final int MARGEN = 34;
    private static final int ALTO_PISO = 160;
    private static final int ALTO_CABECERA = 110;
    private static final int ALTO_PIE = 72;
    private static final Map<String, Integer> ORDEN = new HashMap<>();

    static {
        List<String> ids = Arrays.asList("SC", "PA", "D201", "E201", "D202", "PQ", "CM");
        for (int i = 0; i < ids.size(); i++) ORDEN.put(ids.get(i), i);
    }

    private PlanoEdificioSVG() { }

    /** Genera SVG sin modificar los resultados ni ejecutar el simulador. */
    public static String generar(DatosVistaEdificio.Escenario escenario) {
        if (escenario == null) throw new IllegalArgumentException("El escenario no puede ser null");
        List<Integer> pisos = new ArrayList<>(escenario.getPisos());
        pisos.sort(java.util.Collections.reverseOrder());
        int alto = ALTO_CABECERA + pisos.size() * ALTO_PISO + ALTO_PIE;
        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\" role=\"img\" aria-label=\"Edificio comunal, escenario %s\">\n",
                ANCHO, alto, ANCHO, alto, escapar(escenario.getHorario())));
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#f5f7fb\"/>");
        svg.append("<text x=\"34\" y=\"42\" font-family=\"sans-serif\" font-size=\"25\" font-weight=\"bold\" fill=\"#192b43\">Edificio comunal</text>");
        svg.append("<text x=\"34\" y=\"68\" font-family=\"sans-serif\" font-size=\"15\" fill=\"#45556b\">Escenario: ")
                .append(escapar(escenario.getHorario())).append(" · Vista esquemática (no representa ubicaciones reales)</text>");
        for (int i = 0; i < pisos.size(); i++) {
            int piso = pisos.get(i);
            int y = ALTO_CABECERA + i * ALTO_PISO;
            svg.append(String.format(Locale.ROOT,
                    "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"142\" rx=\"12\" fill=\"#ffffff\" stroke=\"#ccd5e0\"/>",
                    MARGEN, y, ANCHO - 2 * MARGEN));
            svg.append(String.format(Locale.ROOT,
                    "<text x=\"%d\" y=\"%d\" font-family=\"sans-serif\" font-size=\"18\" font-weight=\"bold\" fill=\"#233b57\">Piso %d</text>",
                    MARGEN + 16, y + 29, piso));
            List<DatosVistaEdificio.HabitacionVista> habitaciones = new ArrayList<>(escenario.getHabitacionesDePiso(piso));
            habitaciones.sort((a, b) -> {
                int c = Integer.compare(ORDEN.getOrDefault(a.getId(), 999), ORDEN.getOrDefault(b.getId(), 999));
                return c != 0 ? c : a.getId().compareTo(b.getId());
            });
            if (habitaciones.isEmpty()) continue;
            int inicioX = MARGEN + 120;
            int disponible = ANCHO - MARGEN - 18 - inicioX;
            int separacion = 10;
            int anchoHabitacion = (disponible - separacion * (habitaciones.size() - 1)) / habitaciones.size();
            for (int j = 0; j < habitaciones.size(); j++) {
                DatosVistaEdificio.HabitacionVista h = habitaciones.get(j);
                int x = inicioX + j * (anchoHabitacion + separacion);
                String color = color(h.getEstado());
                svg.append(String.format(Locale.ROOT,
                        "<g><rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"94\" rx=\"9\" fill=\"%s\" stroke=\"#53606e\" stroke-width=\"1\"/>",
                        x, y + 37, anchoHabitacion, color));
                svg.append("<title>").append(escapar(h.getNombre())).append(" (ID: ").append(escapar(h.getId()))
                        .append(") · ").append(escapar(h.getEstado().name())).append("</title>");
                texto(svg, x + anchoHabitacion / 2, y + 65, h.getNombre(), 13, true);
                texto(svg, x + anchoHabitacion / 2, y + 88,
                        Double.isFinite(h.getNivelRecibido())
                                ? String.format(Locale.ROOT, "%.1f dB(A)", h.getNivelRecibido()) : "Sin dato", 15, true);
                texto(svg, x + anchoHabitacion / 2, y + 112, h.getEstado().name(), 10, false);
                svg.append("</g>");
            }
        }
        int leyendaY = ALTO_CABECERA + pisos.size() * ALTO_PISO + 22;
        String[] estados = {"HABITABLE", "TOLERABLE", "NO_HABITABLE", "NO_EVALUABLE"};
        for (int i = 0; i < estados.length; i++) {
            int x = 42 + i * 220;
            svg.append(String.format(Locale.ROOT,
                    "<rect x=\"%d\" y=\"%d\" width=\"16\" height=\"16\" rx=\"3\" fill=\"%s\" stroke=\"#7a8793\"/>",
                    x, leyendaY, color(EstadoHabitabilidad.valueOf(estados[i]))));
            svg.append(String.format(Locale.ROOT,
                    "<text x=\"%d\" y=\"%d\" font-family=\"sans-serif\" font-size=\"13\" fill=\"#263747\">%s</text>",
                    x + 23, leyendaY + 13, estados[i]));
        }
        return svg.append("</svg>\n").toString();
    }

    /** Guarda el SVG; la carpeta de destino se crea si hace falta. */
    public static Path guardar(DatosVistaEdificio.Escenario escenario, Path archivo) throws IOException {
        if (archivo == null) throw new IllegalArgumentException("La ruta no puede ser null");
        if (archivo.getParent() != null) Files.createDirectories(archivo.getParent());
        return Files.write(archivo, generar(escenario).getBytes(StandardCharsets.UTF_8));
    }

    private static String color(EstadoHabitabilidad estado) {
        if (estado == null) return "#cbd0d7";
        switch (estado) {
            case HABITABLE: return "#a6dfb4";
            case TOLERABLE: return "#ffe08a";
            case NO_HABITABLE: return "#f4a6a6";
            case NO_EVALUABLE: default: return "#cbd0d7";
        }
    }

    private static void texto(StringBuilder svg, int x, int y, String contenido, int tamano, boolean negrita) {
        svg.append(String.format(Locale.ROOT,
                "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"%d\" %s fill=\"#172535\">",
                x, y, tamano, negrita ? "font-weight=\"bold\"" : ""));
        svg.append(escapar(contenido)).append("</text>");
    }

    private static String escapar(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
