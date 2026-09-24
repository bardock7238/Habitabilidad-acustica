package acustica.simulacion.propagacion;
 
import acustica.dominio.FuenteSonido;
import acustica.dominio.Habitacion;
import acustica.dominio.Superficie;
 

import java.util.ArrayList;
import java.util.List;

/**
 * Modela la propagación de sonido por rayos: desde una FuenteSonido dentro de
 * una Habitacion, emite rayos en distintas direcciones y determina con cuál
 * Superficie de esa habitación choca cada uno.
 *
 * El reparto se modela en 2D: los rayos salen distribuidos uniformemente en los 360° 
 * alrededor de la fuente, y cada Superficie "ocupa" una porción de ese círculo
 * proporcional a su área respecto al área total de las superficies de la
 * habitación — una superficie más grande intercepta más rayos, igual que en
 * la realidad recibiría más energía del sonido emitido.
 */

public class RayTracing {
 
    /** Un rayo individual emitido por la fuente. */
    public static class Rayo {
        private final double anguloGrados;   // dirección de salida (0-360)
        private final double nivelInicial;   // dB al salir de la fuente
        private Superficie superficieImpactada; // null si la habitación no tiene superficies registradas
 
        private Rayo(double anguloGrados, double nivelInicial) {
            this.anguloGrados = anguloGrados;
            this.nivelInicial = nivelInicial;
        }
 
        public double getAnguloGrados() {
            return anguloGrados;
        }
 
        public double getNivelInicial() {
            return nivelInicial;
        }
 
        public Superficie getSuperficieImpactada() {
            return superficieImpactada;
        }
 
        /** true si el rayo efectivamente chocó con alguna superficie. */
        public boolean impacto() {
            return superficieImpactada != null;
        }
 
        @Override
        public String toString() {
            return "Rayo{angulo=" + anguloGrados + "°, nivelInicial=" + nivelInicial + " dB, choca con="
                    + (superficieImpactada != null ? superficieImpactada.getId() : "ninguna") + "}";
        }
    }
 
    /**
     * Emite `cantidadRayos` rayos distribuidos uniformemente en 360° desde la
     * fuente, y calcula con qué superficie de la habitación choca cada uno.
     *
     * @param fuente      fuente de sonido que emite los rayos
     * @param habitacion  habitación donde está la fuente (de ahí salen las superficies candidatas)
     * @param cantidadRayos cuántos rayos emitir (a más rayos, más preciso el reparto, más costoso computar)
     * @return lista de rayos ya emitidos, cada uno con su superficie de impacto asignada (o ninguna)
     */
    public static List<Rayo> emitirRayos(FuenteSonido fuente, Habitacion habitacion, int cantidadRayos) {
        if (cantidadRayos <= 0) {
            throw new IllegalArgumentException("cantidadRayos debe ser mayor que 0");
        }
 
        double nivelInicial = fuente.obtenerNivelEmitido();
        double pasoAngulo = 360.0 / cantidadRayos;
 
        List<Rayo> rayos = new ArrayList<>();
        for (int i = 0; i < cantidadRayos; i++) {
            double angulo = i * pasoAngulo;
            Rayo rayo = new Rayo(angulo, nivelInicial);
            rayo.superficieImpactada = determinarSuperficieImpactada(angulo, habitacion);
            rayos.add(rayo);
        }
 
        return rayos;
    }
 
    /**
     * Decide con qué superficie choca un rayo según su ángulo de salida.
     *
     * Reparte los 360° entre las superficies de la habitación proporcionalmente
     * a su área: recorre las superficies en orden, va acumulando la porción de
     * círculo que le corresponde a cada una, y devuelve la primera cuyo tramo
     * acumulado ya cubre el ángulo del rayo.
     */
    private static Superficie determinarSuperficieImpactada(double anguloGrados, Habitacion habitacion) {
        List<Superficie> superficies = habitacion.getSuperficies();
        if (superficies.isEmpty()) {
            return null; // habitación sin superficies registradas: el rayo no choca con nada
        }
 
        double areaTotal = 0.0;
        for (Superficie s : superficies) {
            areaTotal += s.getArea();
        }
        if (areaTotal <= 0.0) {
            return null; // datos inválidos (áreas en 0): no se puede repartir el círculo
        }
 
        double anguloAcumulado = 0.0;
        for (Superficie s : superficies) {
            anguloAcumulado += (s.getArea() / areaTotal) * 360.0;
            if (anguloGrados < anguloAcumulado) {
                return s;
            }
        }
 
        // Por redondeo de punto flotante, el último tramo puede quedar
        // justo por debajo de 360°; el rayo en el borde cae aquí.
        return superficies.get(superficies.size() - 1);
    }
}

