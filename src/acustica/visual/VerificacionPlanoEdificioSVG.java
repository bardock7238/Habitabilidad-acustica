package acustica.visual;

import acustica.dominio.Habitacion;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;
import java.util.ArrayList;
import java.util.List;

public final class VerificacionPlanoEdificioSVG {
    private VerificacionPlanoEdificioSVG() {}

    public static void main(String[] args) {
        String[] ids = {"PQ", "CM", "PA", "D201", "E201", "D202", "SC"};
        String[] nombres = {"Parqueadero", "Cuarto de maquinas", "Pasillo",
                "Dormitorio 201", "Estudio 201", "Dormitorio 202", "Salon comunal"};
        int[] pisos = {1, 1, 2, 2, 2, 2, 3};
        EstadoHabitabilidad[] estados = {
                EstadoHabitabilidad.HABITABLE, EstadoHabitabilidad.NO_EVALUABLE,
                EstadoHabitabilidad.TOLERABLE, EstadoHabitabilidad.NO_HABITABLE,
                EstadoHabitabilidad.HABITABLE, EstadoHabitabilidad.TOLERABLE,
                EstadoHabitabilidad.NO_HABITABLE};
        List<ResultadoHabitabilidad> dia = new ArrayList<>();
        List<ResultadoHabitabilidad> noche = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            Habitacion h = new Habitacion(ids[i], nombres[i], "Recinto", 3, 4, 2.5);
            h.setPiso(pisos[i]);
            dia.add(ResultadoHabitabilidad.propio(h, 30 + i, 45, estados[i]));
            noche.add(ResultadoHabitabilidad.propio(h, 40 + i, 45, estados[i]));
        }
        DatosVistaEdificio datos = new DatosVistaEdificio(dia, noche);
        String svgDia = PlanoEdificioSVG.generar(datos.getDia());
        String svgNoche = PlanoEdificioSVG.generar(datos.getNoche());

        verificar(svgDia.startsWith("<svg") && svgDia.contains("</svg>"), "SVG completo");
        verificar(svgDia.contains("Piso 1") && svgDia.contains("Piso 2")
                && svgDia.contains("Piso 3"), "Tres pisos");
        for (String nombre : nombres) {
            verificar(svgDia.contains(nombre), "Habitacion " + nombre);
        }
        verificar(svgDia.contains("#a6dfb4") && svgDia.contains("#ffe08a")
                && svgDia.contains("#f4a6a6") && svgDia.contains("#cbd0d7"),
                "Cuatro colores");
        verificar(svgDia.contains("30.0 dB(A)") && svgNoche.contains("40.0 dB(A)")
                && !svgDia.equals(svgNoche), "Niveles independientes por horario");
        verificar(svgDia.contains("no representa ubicaciones reales"), "Aviso esquematico");
        System.out.println("TODAS LAS PRUEBAS DEL PLANO SVG PASARON");
    }

    private static void verificar(boolean condicion, String descripcion) {
        if (!condicion) throw new AssertionError("Fallo: " + descripcion);
        System.out.println("OK: " + descripcion);
    }
}
