package acustica.normativa;

import java.util.HashMap;
import java.util.Map;

import acustica.config.CargadorDatos;

/**
 * Guarda los límites de ruido (dB) según el tipo de recinto y el horario.
 *
 * Los valores se leen de {@code datos/normativa_oms.csv} a través de
 * {@link CargadorDatos}, para que el CSV sea la única fuente de verdad. Si el
 * archivo no está disponible (por ejemplo, si se ejecuta el programa desde
 * otro directorio), se usan los mismos valores como respaldo.
 *
 * La clave interna tiene la forma "tipo-horario" en minúsculas
 * (ej: "dormitorio-noche").
 */
public class Normativa {

    private final Map<String, Double> limites;

    public Normativa() {
        limites = new HashMap<>();
        cargarDesdeCSV();
        if (limites.isEmpty()) {
            cargarValoresPorDefecto();
        }
    }

    /**
     * Carga los límites del CSV. Si el archivo no existe o está mal formado,
     * deja el mapa vacío para que el constructor use los valores por defecto.
     */
    private void cargarDesdeCSV() {
        try {
            Map<String, Map<String, Double>> datos = CargadorDatos.cargarNormativaOMS();
            for (Map.Entry<String, Map<String, Double>> recinto : datos.entrySet()) {
                for (Map.Entry<String, Double> horario : recinto.getValue().entrySet()) {
                    limites.put(construirClave(recinto.getKey(), horario.getKey()), horario.getValue());
                }
            }
        } catch (RuntimeException sinArchivo) {
            System.err.println("Aviso: no se pudo leer datos/normativa_oms.csv; "
                    + "se usan los valores por defecto.");
            limites.clear();
        }
    }

    /**
     * Valores de respaldo, equivalentes a {@code datos/normativa_oms.csv}.
     * Se usan solo si el CSV no se pudo leer.
     */
    private void cargarValoresPorDefecto() {
        limites.put(construirClave("Dormitorio", "dia"), 35.0);
        limites.put(construirClave("Dormitorio", "noche"), 30.0);
        limites.put(construirClave("Sala", "dia"), 35.0);
        limites.put(construirClave("Sala", "noche"), 30.0);
        limites.put(construirClave("Aula", "dia"), 35.0);
        limites.put(construirClave("Hospital", "dia"), 30.0);
        limites.put(construirClave("Hospital", "noche"), 30.0);
    }

    /** Arma la clave "tipo-horario" en minúsculas, para que no importe cómo se escriba. */
    private String construirClave(String tipo, String horario) {
        if (tipo == null || horario == null) {
            throw new IllegalArgumentException("El tipo de recinto y el horario no pueden ser null");
        }
        return tipo.trim().toLowerCase() + "-" + horario.trim().toLowerCase();
    }

    /**
     * Devuelve el límite en dB para ese tipo de recinto y horario.
     * Lanza IllegalArgumentException si la combinación no existe.
     */
    public double obtenerLimite(String tipo, String horario) {
        String clave = construirClave(tipo, horario);
        Double limite = limites.get(clave);
        if (limite == null) {
            throw new IllegalArgumentException("No hay límite definido para: " + clave);
        }
        return limite;
    }

    /** Devuelve true si el nivel medido no supera el límite (es decir, es habitable). */
    public boolean cumple(double nivel, String tipo, String horario) {
        return nivel <= obtenerLimite(tipo, horario);
    }

    /** Copia inmutable de la tabla de límites cargada. */
    public Map<String, Double> getLimites() {
        return Map.copyOf(limites);
    }
}
