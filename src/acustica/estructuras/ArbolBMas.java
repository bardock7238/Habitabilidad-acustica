package acustica.estructuras;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Árbol B+ genérico con orden configurable.
 *
 * Guarda pares (clave, valor). Las claves se comparan con su propio
 * compareTo (K extends Comparable).
 *
 * ESTRUCTURA
 *   - Los nodos internos solo guardan claves separadoras y punteros a hijos;
 *     todos los pares (clave, valor) viven en las hojas.
 *   - Todas las hojas están a la misma profundidad y están enlazadas de
 *     izquierda a derecha, así una consulta por rango recorre hojas contiguas
 *     sin volver a subir por el árbol.
 *
 * ORDEN (m, mínimo 3)
 *   - Un nodo interno tiene como máximo m hijos (m-1 claves) y, si no es la
 *     raíz, al menos ceil(m/2) hijos. La raíz interna tiene al menos 2 hijos.
 *   - Una hoja guarda como máximo m-1 pares y, si no es la raíz, al menos
 *     ceil((m-1)/2).
 *   Con eso buscar, insertar y eliminar cuestan O(log n).
 *
 * MANEJO DE DUPLICADOS
 *   Igual que en ArbolAVL y ArbolRojinegro: dos claves que comparan igual
 *   (compareTo == 0) se consideran la MISMA clave, y la clave debe incluir
 *   un desempate. Por eso el árbol guarda un solo valor por clave:
 *     - insertar con una clave que ya existe devuelve false y NO modifica el
 *       árbol (se conserva el valor que ya estaba; no se sobrescribe).
 *   Para guardar varios resultados con el mismo criterio de búsqueda sin
 *   perder ninguno, la clave debe ser compuesta, de modo que dos resultados
 *   distintos nunca tengan la misma clave. Para el índice acústico se propone
 *   la clave (piso, nivelRecibido, habitacionId): dos habitaciones con el
 *   mismo piso y el mismo nivel recibido quedan como claves distintas por su
 *   habitacionId, y las dos se conservan.
 *   Las consultas por rango devuelven TODOS los valores cuyas claves caen en
 *   el intervalo, ambos extremos incluidos.
 *
 * @param <K> tipo de la clave (comparable)
 * @param <V> tipo del valor
 */
public class ArbolBMas<K extends Comparable<? super K>, V> {

    private static final int ORDEN_MINIMO = 3;

    /** Nodo del árbol: solo guarda las claves; los hijos o valores los define cada tipo. */
    private abstract static class Nodo<K, V> {
        final List<K> claves = new ArrayList<>();
    }

    /** Nodo interno: claves separadoras e hijos (hijos = claves + 1). */
    private static class Interno<K, V> extends Nodo<K, V> {
        final List<Nodo<K, V>> hijos = new ArrayList<>();
    }

    /** Hoja: pares (clave, valor) y enlace a la hoja siguiente. */
    private static class Hoja<K, V> extends Nodo<K, V> {
        final List<V> valores = new ArrayList<>();
        Hoja<K, V> siguiente;
    }

    /** Resultado de partir un nodo: clave que sube al padre y nodo nuevo de la derecha. */
    private static class Division<K, V> {
        final K clave;
        final Nodo<K, V> derecho;

        Division(K clave, Nodo<K, V> derecho) {
            this.clave = clave;
            this.derecho = derecho;
        }
    }

    private final int orden;
    private Nodo<K, V> raiz; // null si el árbol está vacío
    private int tamano;

    /**
     * @param orden máximo de hijos de un nodo interno (mínimo 3)
     */
    public ArbolBMas(int orden) {
        if (orden < ORDEN_MINIMO) {
            throw new IllegalArgumentException("El orden debe ser al menos " + ORDEN_MINIMO);
        }
        this.orden = orden;
    }

    // ------------------------------------------------------------------
    // Operaciones públicas
    // ------------------------------------------------------------------

    /**
     * Inserta el par. Devuelve false (sin cambiar nada) si ya había una clave
     * equivalente; en ese caso se conserva el valor anterior.
     */
    public boolean insertar(K clave, V valor) {
        if (clave == null) {
            throw new IllegalArgumentException("No se pueden insertar claves null");
        }
        if (valor == null) {
            throw new IllegalArgumentException("No se pueden insertar valores null");
        }
        if (raiz == null) {
            Hoja<K, V> hoja = new Hoja<>();
            hoja.claves.add(clave);
            hoja.valores.add(valor);
            raiz = hoja;
            tamano = 1;
            return true;
        }
        Hoja<K, V> destino = hojaDe(clave);
        if (posicionExacta(destino.claves, clave) >= 0) {
            return false; // clave duplicada: no se inserta
        }
        Division<K, V> division = insertar(raiz, clave, valor);
        if (division != null) {
            Interno<K, V> nuevaRaiz = new Interno<>();
            nuevaRaiz.claves.add(division.clave);
            nuevaRaiz.hijos.add(raiz);
            nuevaRaiz.hijos.add(division.derecho);
            raiz = nuevaRaiz;
        }
        tamano++;
        return true;
    }

    /** Valor asociado a la clave, o null si no está. */
    public V buscar(K clave) {
        if (clave == null || raiz == null) {
            return null;
        }
        Hoja<K, V> hoja = hojaDe(clave);
        int i = posicionExacta(hoja.claves, clave);
        return i >= 0 ? hoja.valores.get(i) : null;
    }

    /** Elimina la clave indicada. Devuelve false si no estaba. */
    public boolean eliminar(K clave) {
        if (clave == null || raiz == null) {
            return false;
        }
        if (!eliminar(raiz, clave)) {
            return false;
        }
        tamano--;
        if (raiz instanceof Hoja) {
            if (raiz.claves.isEmpty()) {
                raiz = null;
            }
        } else {
            Interno<K, V> interna = (Interno<K, V>) raiz;
            if (interna.hijos.size() == 1) {
                raiz = interna.hijos.get(0); // el árbol baja un nivel
            }
        }
        return true;
    }

    /**
     * Valores cuyas claves están entre desde y hasta (ambos incluidos), en
     * orden ascendente de clave. Recorre las hojas enlazadas, así que cuesta
     * O(log n + k). Si desde es mayor que hasta, devuelve una lista vacía.
     */
    public List<V> rango(K desde, K hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Los extremos del rango no pueden ser null");
        }
        List<V> resultado = new ArrayList<>();
        if (raiz == null || desde.compareTo(hasta) > 0) {
            return resultado;
        }
        Hoja<K, V> hoja = hojaDe(desde);
        int i = limiteInferior(hoja.claves, desde);
        while (hoja != null) {
            while (i < hoja.claves.size()) {
                if (hoja.claves.get(i).compareTo(hasta) > 0) {
                    return resultado;
                }
                resultado.add(hoja.valores.get(i));
                i++;
            }
            hoja = hoja.siguiente;
            i = 0;
        }
        return resultado;
    }

    /** Clave más pequeña, o null si el árbol está vacío. */
    public K minimo() {
        if (raiz == null) {
            return null;
        }
        return primeraHoja().claves.get(0);
    }

    /** Clave más grande, o null si el árbol está vacío. */
    public K maximo() {
        if (raiz == null) {
            return null;
        }
        Nodo<K, V> n = raiz;
        while (n instanceof Interno) {
            Interno<K, V> interno = (Interno<K, V>) n;
            n = interno.hijos.get(interno.hijos.size() - 1);
        }
        return n.claves.get(n.claves.size() - 1);
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacio() {
        return tamano == 0;
    }

    /** Altura del árbol en niveles (0 si está vacío, 1 si solo hay una hoja). */
    public int altura() {
        int altura = 0;
        Nodo<K, V> n = raiz;
        while (n != null) {
            altura++;
            n = n instanceof Interno ? ((Interno<K, V>) n).hijos.get(0) : null;
        }
        return altura;
    }

    /**
     * Comprueba las reglas del B+: todas las hojas a la misma profundidad,
     * cantidad de claves e hijos dentro de los límites del orden, claves
     * ordenadas y dentro del rango que marcan sus separadores, y hojas
     * enlazadas en el mismo orden del árbol, que en total contienen
     * exactamente tamano() pares.
     */
    public boolean cumpleInvariantes() {
        if (raiz == null) {
            return tamano == 0;
        }
        List<Hoja<K, V>> hojas = new ArrayList<>();
        if (verificar(raiz, true, null, null, hojas) < 0) {
            return false;
        }
        // Cadena de hojas: debe pasar por las mismas hojas, en el mismo orden
        Hoja<K, V> actual = primeraHoja();
        int total = 0;
        K anterior = null;
        for (Hoja<K, V> esperada : hojas) {
            if (actual != esperada) {
                return false;
            }
            for (K clave : actual.claves) {
                if (anterior != null && anterior.compareTo(clave) >= 0) {
                    return false;
                }
                anterior = clave;
                total++;
            }
            actual = actual.siguiente;
        }
        return actual == null && total == tamano;
    }

    /**
     * Copia la estructura del árbol en nodos visuales para dibujarlo. Cada
     * nodo del B+ es un nodo visual con sus claves separadas por " | " y sus
     * hijos en getHijos(). Las hojas llevan la insignia "hoja →" si tienen
     * una hoja siguiente enlazada, o "hoja" si son la última; los nodos
     * internos llevan la insignia "interno".
     *
     * @param claveATexto convierte cada clave en el texto que se muestra
     */
    public NodoVisual aNodoVisual(Function<? super K, String> claveATexto) {
        return raiz == null ? null : aNodoVisual(raiz, claveATexto);
    }

    private NodoVisual aNodoVisual(Nodo<K, V> n, Function<? super K, String> claveATexto) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < n.claves.size(); i++) {
            if (i > 0) {
                texto.append(" | ");
            }
            texto.append(claveATexto.apply(n.claves.get(i)));
        }
        if (n instanceof Hoja) {
            Hoja<K, V> hoja = (Hoja<K, V>) n;
            NodoVisual v = new NodoVisual(texto.toString(), hoja.claves.size() + " pares",
                    "Hoja con " + hoja.claves.size() + " pares: " + texto
                            + (hoja.siguiente != null ? ". Enlazada a la hoja siguiente." : ". Última hoja."),
                    "#2e7d6b", "#ffffff");
            v.setInsignia(hoja.siguiente != null ? "hoja \u2192" : "hoja");
            return v;
        }
        Interno<K, V> interno = (Interno<K, V>) n;
        NodoVisual v = new NodoVisual(texto.toString(), interno.hijos.size() + " hijos",
                "Nodo interno con " + interno.hijos.size() + " hijos. Claves separadoras: " + texto,
                "#3b5b92", "#ffffff");
        v.setInsignia("interno");
        for (Nodo<K, V> hijo : interno.hijos) {
            v.agregarHijo(aNodoVisual(hijo, claveATexto));
        }
        return v;
    }

    // ------------------------------------------------------------------
    // Límites de tamaño según el orden
    // ------------------------------------------------------------------

    /** Máximo de pares en una hoja. */
    private int maxHoja() {
        return orden - 1;
    }

    /** Mínimo de pares en una hoja que no es la raíz: ceil((m-1)/2). */
    private int minHoja() {
        return orden / 2;
    }

    /** Mínimo de hijos en un nodo interno que no es la raíz: ceil(m/2). */
    private int minHijos() {
        return (orden + 1) / 2;
    }

    // ------------------------------------------------------------------
    // Búsqueda dentro de un nodo
    // ------------------------------------------------------------------

    /** Primera posición cuya clave es >= clave (o claves.size() si todas son menores). */
    private int limiteInferior(List<K> claves, K clave) {
        int lo = 0;
        int hi = claves.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (claves.get(mid).compareTo(clave) < 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /** Posición de la clave en la lista, o -1 si no está. */
    private int posicionExacta(List<K> claves, K clave) {
        int i = limiteInferior(claves, clave);
        return i < claves.size() && claves.get(i).compareTo(clave) == 0 ? i : -1;
    }

    /**
     * Hijo por el que hay que bajar para buscar la clave: la cantidad de
     * separadores <= clave. Convención: el hijo i guarda claves menores que
     * claves[i] y el hijo i+1 guarda claves mayores o iguales.
     */
    private int indiceHijo(Interno<K, V> n, K clave) {
        int lo = 0;
        int hi = n.claves.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (n.claves.get(mid).compareTo(clave) <= 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /** Hoja donde está (o debería estar) la clave. Requiere árbol no vacío. */
    private Hoja<K, V> hojaDe(K clave) {
        Nodo<K, V> n = raiz;
        while (n instanceof Interno) {
            Interno<K, V> interno = (Interno<K, V>) n;
            n = interno.hijos.get(indiceHijo(interno, clave));
        }
        return (Hoja<K, V>) n;
    }

    private Hoja<K, V> primeraHoja() {
        Nodo<K, V> n = raiz;
        while (n instanceof Interno) {
            n = ((Interno<K, V>) n).hijos.get(0);
        }
        return (Hoja<K, V>) n;
    }

    // ------------------------------------------------------------------
    // Inserción
    // ------------------------------------------------------------------

    /** Inserta en el subárbol (la clave es nueva). Devuelve la división si el nodo se partió. */
    private Division<K, V> insertar(Nodo<K, V> nodo, K clave, V valor) {
        if (nodo instanceof Hoja) {
            Hoja<K, V> hoja = (Hoja<K, V>) nodo;
            int i = limiteInferior(hoja.claves, clave);
            hoja.claves.add(i, clave);
            hoja.valores.add(i, valor);
            return hoja.claves.size() > maxHoja() ? dividirHoja(hoja) : null;
        }
        Interno<K, V> interno = (Interno<K, V>) nodo;
        int i = indiceHijo(interno, clave);
        Division<K, V> division = insertar(interno.hijos.get(i), clave, valor);
        if (division == null) {
            return null;
        }
        interno.claves.add(i, division.clave);
        interno.hijos.add(i + 1, division.derecho);
        return interno.hijos.size() > orden ? dividirInterno(interno) : null;
    }

    /**
     * Parte una hoja desbordada (m pares): la izquierda se queda con m/2 y el
     * resto pasa a una hoja nueva. La primera clave de la hoja nueva SE COPIA
     * al padre como separador (sigue estando en la hoja).
     */
    private Division<K, V> dividirHoja(Hoja<K, V> hoja) {
        int corte = hoja.claves.size() / 2;
        Hoja<K, V> derecha = new Hoja<>();
        derecha.claves.addAll(hoja.claves.subList(corte, hoja.claves.size()));
        derecha.valores.addAll(hoja.valores.subList(corte, hoja.valores.size()));
        hoja.claves.subList(corte, hoja.claves.size()).clear();
        hoja.valores.subList(corte, hoja.valores.size()).clear();
        derecha.siguiente = hoja.siguiente;
        hoja.siguiente = derecha;
        return new Division<>(derecha.claves.get(0), derecha);
    }

    /**
     * Parte un nodo interno desbordado (m+1 hijos, m claves): la clave del
     * medio SUBE al padre (ya no queda en ninguna mitad).
     */
    private Division<K, V> dividirInterno(Interno<K, V> interno) {
        int medio = interno.claves.size() / 2;
        K subida = interno.claves.get(medio);
        Interno<K, V> derecha = new Interno<>();
        derecha.claves.addAll(interno.claves.subList(medio + 1, interno.claves.size()));
        derecha.hijos.addAll(interno.hijos.subList(medio + 1, interno.hijos.size()));
        interno.claves.subList(medio, interno.claves.size()).clear();
        interno.hijos.subList(medio + 1, interno.hijos.size()).clear();
        return new Division<>(subida, derecha);
    }

    // ------------------------------------------------------------------
    // Eliminación
    // ------------------------------------------------------------------

    /** Elimina del subárbol. Devuelve false si la clave no estaba. */
    private boolean eliminar(Nodo<K, V> nodo, K clave) {
        if (nodo instanceof Hoja) {
            Hoja<K, V> hoja = (Hoja<K, V>) nodo;
            int i = posicionExacta(hoja.claves, clave);
            if (i < 0) {
                return false;
            }
            hoja.claves.remove(i);
            hoja.valores.remove(i);
            return true;
        }
        Interno<K, V> interno = (Interno<K, V>) nodo;
        int i = indiceHijo(interno, clave);
        Nodo<K, V> hijo = interno.hijos.get(i);
        if (!eliminar(hijo, clave)) {
            return false;
        }
        if (quedoCorto(hijo)) {
            rebalancear(interno, i);
        }
        return true;
    }

    /** true si el nodo (que no es la raíz) quedó por debajo del mínimo. */
    private boolean quedoCorto(Nodo<K, V> n) {
        if (n instanceof Hoja) {
            return n.claves.size() < minHoja();
        }
        return ((Interno<K, V>) n).hijos.size() < minHijos();
    }

    /** true si el nodo tiene más del mínimo y por eso puede prestar un elemento. */
    private boolean puedePrestar(Nodo<K, V> n) {
        if (n instanceof Hoja) {
            return n.claves.size() > minHoja();
        }
        return ((Interno<K, V>) n).hijos.size() > minHijos();
    }

    /**
     * Arregla el hijo i del padre, que quedó corto: primero pide prestado al
     * hermano izquierdo, luego al derecho; si ninguno puede prestar, se
     * fusiona con un hermano.
     */
    private void rebalancear(Interno<K, V> padre, int i) {
        Nodo<K, V> hijo = padre.hijos.get(i);
        Nodo<K, V> izquierdo = i > 0 ? padre.hijos.get(i - 1) : null;
        Nodo<K, V> derecho = i < padre.hijos.size() - 1 ? padre.hijos.get(i + 1) : null;

        if (izquierdo != null && puedePrestar(izquierdo)) {
            prestarDeIzquierda(padre, i, izquierdo, hijo);
        } else if (derecho != null && puedePrestar(derecho)) {
            prestarDeDerecha(padre, i, hijo, derecho);
        } else if (izquierdo != null) {
            fusionar(padre, i - 1, izquierdo, hijo);
        } else {
            fusionar(padre, i, hijo, derecho);
        }
    }

    private void prestarDeIzquierda(Interno<K, V> padre, int i, Nodo<K, V> izquierdo, Nodo<K, V> hijo) {
        int ultimo = izquierdo.claves.size() - 1;
        if (hijo instanceof Hoja) {
            Hoja<K, V> hoja = (Hoja<K, V>) hijo;
            Hoja<K, V> hermana = (Hoja<K, V>) izquierdo;
            hoja.claves.add(0, hermana.claves.remove(ultimo));
            hoja.valores.add(0, hermana.valores.remove(ultimo));
            padre.claves.set(i - 1, hoja.claves.get(0));
        } else {
            Interno<K, V> interno = (Interno<K, V>) hijo;
            Interno<K, V> hermano = (Interno<K, V>) izquierdo;
            interno.claves.add(0, padre.claves.get(i - 1));
            interno.hijos.add(0, hermano.hijos.remove(hermano.hijos.size() - 1));
            padre.claves.set(i - 1, hermano.claves.remove(ultimo));
        }
    }

    private void prestarDeDerecha(Interno<K, V> padre, int i, Nodo<K, V> hijo, Nodo<K, V> derecho) {
        if (hijo instanceof Hoja) {
            Hoja<K, V> hoja = (Hoja<K, V>) hijo;
            Hoja<K, V> hermana = (Hoja<K, V>) derecho;
            hoja.claves.add(hermana.claves.remove(0));
            hoja.valores.add(hermana.valores.remove(0));
            padre.claves.set(i, hermana.claves.get(0));
        } else {
            Interno<K, V> interno = (Interno<K, V>) hijo;
            Interno<K, V> hermano = (Interno<K, V>) derecho;
            interno.claves.add(padre.claves.get(i));
            interno.hijos.add(hermano.hijos.remove(0));
            padre.claves.set(i, hermano.claves.remove(0));
        }
    }

    /**
     * Fusiona el hijo derecho dentro del izquierdo (hijos i e i+1 del padre) y
     * quita del padre el separador y el puntero al derecho.
     */
    private void fusionar(Interno<K, V> padre, int i, Nodo<K, V> izquierdo, Nodo<K, V> derecho) {
        if (izquierdo instanceof Hoja) {
            Hoja<K, V> izq = (Hoja<K, V>) izquierdo;
            Hoja<K, V> der = (Hoja<K, V>) derecho;
            izq.claves.addAll(der.claves);
            izq.valores.addAll(der.valores);
            izq.siguiente = der.siguiente;
        } else {
            Interno<K, V> izq = (Interno<K, V>) izquierdo;
            Interno<K, V> der = (Interno<K, V>) derecho;
            izq.claves.add(padre.claves.get(i)); // el separador baja
            izq.claves.addAll(der.claves);
            izq.hijos.addAll(der.hijos);
        }
        padre.claves.remove(i);
        padre.hijos.remove(i + 1);
    }

    // ------------------------------------------------------------------
    // Verificación de invariantes
    // ------------------------------------------------------------------

    /**
     * Devuelve la profundidad del subárbol (hojas = 1), o -1 si viola alguna
     * regla. Las claves del subárbol deben ser >= min y < max (null = sin límite).
     */
    private int verificar(Nodo<K, V> n, boolean esRaiz, K min, K max, List<Hoja<K, V>> hojas) {
        // Claves estrictamente crecientes y dentro de [min, max)
        for (int i = 0; i < n.claves.size(); i++) {
            K c = n.claves.get(i);
            if (c == null
                    || (i > 0 && n.claves.get(i - 1).compareTo(c) >= 0)
                    || (min != null && c.compareTo(min) < 0)
                    || (max != null && c.compareTo(max) >= 0)) {
                return -1;
            }
        }
        if (n instanceof Hoja) {
            Hoja<K, V> hoja = (Hoja<K, V>) n;
            int cantidad = hoja.claves.size();
            if (hoja.valores.size() != cantidad || cantidad == 0 || cantidad > maxHoja()
                    || (!esRaiz && cantidad < minHoja())) {
                return -1;
            }
            for (V valor : hoja.valores) {
                if (valor == null) {
                    return -1;
                }
            }
            hojas.add(hoja);
            return 1;
        }
        Interno<K, V> interno = (Interno<K, V>) n;
        int hijos = interno.hijos.size();
        if (hijos != interno.claves.size() + 1 || hijos > orden
                || hijos < (esRaiz ? 2 : minHijos())) {
            return -1;
        }
        int profundidad = -1;
        for (int i = 0; i < hijos; i++) {
            K desde = i == 0 ? min : interno.claves.get(i - 1);
            K hasta = i == hijos - 1 ? max : interno.claves.get(i);
            int p = verificar(interno.hijos.get(i), false, desde, hasta, hojas);
            if (p < 0 || (profundidad >= 0 && p != profundidad)) {
                return -1;
            }
            profundidad = p;
        }
        return profundidad + 1;
    }
}
