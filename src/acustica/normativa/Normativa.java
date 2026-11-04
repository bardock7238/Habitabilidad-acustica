package acustica.normativa;

import java.util.HashMap;
import java.util.Map;

import acustica.config.CargadorDatos;
import acustica.config.CargadorDatos.SectorRuido;

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

    /**
     * Espacios comunes de un edificio residencial. La OMS no define límites
     * para ellos, así que se usan los del sector B (zonas residenciales) de
     * {@code datos/sectores.csv} (Res. 0627/2006). Esos límites son de ruido
     * ambiental por zona; aplicarlos dentro del edificio es una aproximación.
     */
    private static final String[] TIPOS_COMUNES = {
        "Parqueadero", "Salon comunal", "Cuarto de maquinas", "Pasillo"
    };

    private final Map<String, Double> limites;

    public Normativa() {
        limites = new HashMap<>();
        cargarDesdeCSV();
        if (limites.isEmpty()) {
            cargarValoresPorDefecto();
        }
        cargarLimitesComunes();
    }

    /**
     * Asigna a los espacios comunes el límite del sector B residencial
     * (65 dB de día, 55 dB de noche). Si el CSV no se puede leer, usa esos
     * mismos valores como respaldo. No pisa límites ya definidos.
     */
    private void cargarLimitesComunes() {
        double dia = 65.0;
        double noche = 55.0;
        try {
            for (SectorRuido sector : CargadorDatos.cargarSectores()) {
                if (sector.sector.equals("B")
                        && sector.subsector.toLowerCase().startsWith("zonas residenciales")) {
                    dia = sector.limiteDiaDb;
                    noche = sector.limiteNocheDb;
                    break;
                }
            }
        } catch (RuntimeException sinArchivo) {
            // se mantienen los valores de respaldo
        }
        for (String tipo : TIPOS_COMUNES) {
            limites.putIfAbsent(construirClave(tipo, "dia"), dia);
            limites.putIfAbsent(construirClave(tipo, "noche"), noche);
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
