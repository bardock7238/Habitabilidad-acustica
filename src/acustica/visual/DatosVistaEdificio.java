package acustica.visual;

import acustica.dominio.Habitacion;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Datos independientes de la simulacion para construir una vista 2D del edificio.
 *
 * Solo copia los resultados YA calculados para dia y noche. No ejecuta el
 * Simulador ni consulta Habitacion.getNivelRuido(), que puede cambiar al correr
 * otro escenario.
 *
 * Las dimensiones estan expresadas en metros y los niveles en dB(A).
 * Esta clase no inventa coordenadas (x, y): la distribucion geometrica de los
 * recintos se definira cuando se implemente la interfaz 2D.
 */
public final class DatosVistaEdificio {

    private final Escenario dia;
    private final Escenario noche;

    public DatosVistaEdificio(List<ResultadoHabitabilidad> resultadosDia,
            List<ResultadoHabitabilidad> resultadosNoche) {
        this.dia = new Escenario("dia", resultadosDia);
        this.noche = new Escenario("noche", resultadosNoche);
    }

    public Escenario getDia() {
        return dia;
    }

    public Escenario getNoche() {
        return noche;
    }

    /** Devuelve el escenario por nombre ("dia" o "noche", sin distinguir mayusculas). */
    public Escenario getEscenario(String horario) {
        if (horario == null) {
            throw new IllegalArgumentException("El horario no puede ser null");
        }
        if ("dia".equalsIgnoreCase(horario.trim())) {
            return dia;
        }
        if ("noche".equalsIgnoreCase(horario.trim())) {
            return noche;
        }
        throw new IllegalArgumentException("Horario no reconocido: " + horario);
    }

    /**
     * JSON listo para insertarse como datos en HTML/JavaScript. No necesita
     * dependencias externas ni vuelve a calcular los resultados acusticos.
     * Los limites indefinidos (NaN o infinito) se expresan como JSON null.
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"dia\":");
        dia.appendJson(json);
        json.append(',');
        json.append("\"noche\":");
        noche.appendJson(json);
        json.append('}');
        return json.toString();
    }

    /** Una fotografia inmutable de todas las habitaciones de un horario. */
    public static final class Escenario {
        private final String horario;
        private final List<HabitacionVista> habitaciones;
        private final Map<Integer, List<HabitacionVista>> porPiso;

        private Escenario(String horario, List<ResultadoHabitabilidad> resultados) {
            if (resultados == null) {
                throw new IllegalArgumentException("Los resultados de " + horario + " no pueden ser null");
            }
            this.horario = horario;
            List<HabitacionVista> copias = new ArrayList<>();
            Set<String> ids = new LinkedHashSet<>();
            for (ResultadoHabitabilidad resultado : resultados) {
                if (resultado == null || resultado.getHabitacion() == null) {
                    throw new IllegalArgumentException("Resultado o habitacion nula en " + horario);
                }
                HabitacionVista copia = new HabitacionVista(resultado);
                if (!ids.add(copia.getId())) {
                    throw new IllegalArgumentException("Habitacion duplicada en " + horario + ": " + copia.getId());
                }
                copias.add(copia);
            }
            copias.sort(Comparator.comparingInt(HabitacionVista::getPiso)
                    .thenComparing(HabitacionVista::getId));
            this.habitaciones = Collections.unmodifiableList(copias);

            Map<Integer, List<HabitacionVista>> agrupadas = new LinkedHashMap<>();
            for (HabitacionVista copia : copias) {
                agrupadas.computeIfAbsent(copia.getPiso(), p -> new ArrayList<>()).add(copia);
            }
            for (Map.Entry<Integer, List<HabitacionVista>> entrada : agrupadas.entrySet()) {
                entrada.setValue(Collections.unmodifiableList(entrada.getValue()));
            }
            this.porPiso = Collections.unmodifiableMap(agrupadas);
        }

        public String getHorario() {
            return horario;
        }

        public List<HabitacionVista> getHabitaciones() {
            return habitaciones;
        }

        public Set<Integer> getPisos() {
            return porPiso.keySet();
        }

        /** Lista inmutable, vacia si el piso no existe. */
        public List<HabitacionVista> getHabitacionesDePiso(int piso) {
            List<HabitacionVista> resultado = porPiso.get(piso);
            return resultado == null ? Collections.emptyList() : resultado;
        }

        private void appendJson(StringBuilder json) {
            json.append('{');
            json.append("\"horario\":");
            appendString(json, horario);
            json.append(",\"habitaciones\":[");
            for (int i = 0; i < habitaciones.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                habitaciones.get(i).appendJson(json);
            }
            json.append("]}");
        }
    }

    /** Datos propios de un recinto, copiados en el momento de crear la vista. */
    public static final class HabitacionVista {
        private final String id;
        private final String nombre;
        private final int piso;
        private final double ancho;
        private final double largo;
        private final double alto;
        private final double nivelRecibido;
        private final double nivelFinal;
        private final Double limiteNormativo;
        private final EstadoHabitabilidad estado;
        private final boolean usaNivelDeVecino;

        private HabitacionVista(ResultadoHabitabilidad resultado) {
            Habitacion h = resultado.getHabitacion();
            this.id = h.getId();
            this.nombre = h.getNombre();
            this.piso = h.getPiso();
            this.ancho = h.getAncho();
            this.largo = h.getLargo();
            this.alto = h.getAlto();
            this.nivelRecibido = resultado.getNivelRecibido();
            this.nivelFinal = resultado.getNivelFinal();
            double limite = resultado.getLimiteAplicado();
            this.limiteNormativo = Double.isFinite(limite) ? limite : null;
            this.estado = resultado.getEstado();
            this.usaNivelDeVecino = resultado.usaNivelDeVecino();
        }

        public String getId() { return id; }
        public String getNombre() { return nombre; }
        public int getPiso() { return piso; }
        public double getAncho() { return ancho; }
        public double getLargo() { return largo; }
        public double getAlto() { return alto; }
        public double getNivelRecibido() { return nivelRecibido; }
        public double getNivelFinal() { return nivelFinal; }

        /**
         * Limite utilizado para evaluar este resultado. Puede ser el del propio
         * recinto, el limite de una actividad o el del vecino mas afectado.
         * Es null si la evaluacion no tiene un limite numerico aplicable.
         */
        public Double getLimiteNormativo() { return limiteNormativo; }
        public EstadoHabitabilidad getEstado() { return estado; }
        public boolean usaNivelDeVecino() { return usaNivelDeVecino; }

        private void appendJson(StringBuilder json) {
            json.append('{');
            json.append("\"id\":"); appendString(json, id);
            json.append(",\"nombre\":"); appendString(json, nombre);
            json.append(",\"piso\":").append(piso);
            json.append(",\"ancho\":"); appendNumero(json, ancho);
            json.append(",\"largo\":"); appendNumero(json, largo);
            json.append(",\"alto\":"); appendNumero(json, alto);
            json.append(",\"nivelRecibido\":"); appendNumero(json, nivelRecibido);
            json.append(",\"nivelFinal\":"); appendNumero(json, nivelFinal);
            json.append(",\"limiteNormativo\":");
            if (limiteNormativo == null) {
                json.append("null");
            } else {
                appendNumero(json, limiteNormativo);
            }
            json.append(",\"estado\":"); appendString(json, estado.name());
            json.append(",\"usaNivelDeVecino\":").append(usaNivelDeVecino);
            json.append('}');
        }
    }

    private static void appendNumero(StringBuilder json, double numero) {
        // JSON no admite NaN ni Infinity.
        if (Double.isFinite(numero)) {
            json.append(numero);
        } else {
            json.append("null");
        }
    }

    private static void appendString(StringBuilder json, String valor) {
        if (valor == null) {
            json.append("null");
            return;
        }
        json.append('"');
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"': json.append("\\\""); break;
                case '\\': json.append("\\\\"); break;
                case '\n': json.append("\\n"); break;
                case '\r': json.append("\\r"); break;
                case '\t': json.append("\\t"); break;
                default:
                    if (c < 0x20 || c == '<' || c == '>' || c == '&'
                            || c == '\u2028' || c == '\u2029') {
                        // Los escapes \u003c, \u003e y \u0026 evitan problemas
                        // al incrustar el JSON dentro de un <script> HTML.
                        String hex = Integer.toHexString(c);
                        json.append("\\u");
                        for (int ceros = hex.length(); ceros < 4; ceros++) {
                            json.append('0');
                        }
                        json.append(hex);
                    } else {
                        json.append(c);
                    }
            }
        }
        json.append('"');
    }
}
