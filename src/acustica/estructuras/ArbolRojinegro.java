package acustica.estructuras;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Árbol rojinegro genérico (red-black tree).
 *
 * Cada nodo es ROJO o NEGRO y se cumplen siempre estas reglas:
 *   1. La raíz es negra.
 *   2. Un nodo rojo no tiene hijos rojos.
 *   3. Todos los caminos de un nodo hasta sus hojas (NIL) tienen la misma
 *      cantidad de nodos negros.
 * Con eso buscar, insertar y eliminar cuestan O(log n). Frente al AVL está
 * menos estrictamente balanceado, pero hace menos rotaciones al
 * insertar/eliminar, lo que lo hace buena opción cuando el conjunto cambia
 * seguido (agregar y descartar soluciones candidatas).
 *
 * Igual que en ArbolAVL, el comparador debe incluir un desempate:
 * dos elementos que comparen igual se consideran el mismo.
 */
public class ArbolRojinegro<T> {

    private enum Color { ROJO, NEGRO }

    private static class Nodo<T> {
        T dato;
        Color color;
        Nodo<T> izq;
        Nodo<T> der;
        Nodo<T> padre;

        Nodo(T dato, Color color) {
            this.dato = dato;
            this.color = color;
        }
    }

    private final Comparator<? super T> orden;
    /** Centinela: hoja negra compartida que representa "no hay nodo" (NIL). */
    private final Nodo<T> nil;
    private Nodo<T> raiz;
    private int tamano;

    public ArbolRojinegro(Comparator<? super T> orden) {
        if (orden == null) {
            throw new IllegalArgumentException("El comparador no puede ser null");
        }
        this.orden = orden;
        this.nil = new Nodo<>(null, Color.NEGRO);
        this.nil.izq = nil;
        this.nil.der = nil;
        this.nil.padre = nil;
        this.raiz = nil;
    }

    // ------------------------------------------------------------------
    // Operaciones públicas
    // ------------------------------------------------------------------

    /** Inserta el dato. Devuelve false si ya había uno equivalente. */
    public boolean insertar(T dato) {
        if (dato == null) {
            throw new IllegalArgumentException("No se pueden insertar datos null");
        }
        Nodo<T> padre = nil;
        Nodo<T> actual = raiz;
        while (actual != nil) {
            padre = actual;
            int c = orden.compare(dato, actual.dato);
            if (c == 0) {
                return false;
            }
            actual = c < 0 ? actual.izq : actual.der;
        }

        Nodo<T> nuevo = new Nodo<>(dato, Color.ROJO);
        nuevo.izq = nil;
        nuevo.der = nil;
        nuevo.padre = padre;
        if (padre == nil) {
            raiz = nuevo;
        } else if (orden.compare(dato, padre.dato) < 0) {
            padre.izq = nuevo;
        } else {
            padre.der = nuevo;
        }
        tamano++;
        arreglarInsercion(nuevo);
        return true;
    }

    /** Elimina el dato equivalente al indicado. Devuelve false si no estaba. */
    public boolean eliminar(T dato) {
        Nodo<T> z = buscarNodo(dato);
        if (z == nil) {
            return false;
        }

        Nodo<T> y = z;
        Color colorOriginal = y.color;
        Nodo<T> x;
        if (z.izq == nil) {
            x = z.der;
            trasplantar(z, z.der);
        } else if (z.der == nil) {
            x = z.izq;
            trasplantar(z, z.izq);
        } else {
            y = minimo(z.der);
            colorOriginal = y.color;
            x = y.der;
            if (y.padre == z) {
                x.padre = y;
            } else {
                trasplantar(y, y.der);
                y.der = z.der;
                y.der.padre = y;
            }
            trasplantar(z, y);
            y.izq = z.izq;
            y.izq.padre = y;
            y.color = z.color;
        }
        tamano--;
        if (colorOriginal == Color.NEGRO) {
            arreglarEliminacion(x);
        }
        return true;
    }

    public boolean contiene(T dato) {
        return buscarNodo(dato) != nil;
    }

    /** Elemento más pequeño según el orden, o null si está vacío. */
    public T minimo() {
        return raiz == nil ? null : minimo(raiz).dato;
    }

    /** Elemento más grande según el orden, o null si está vacío. */
    public T maximo() {
        if (raiz == nil) {
            return null;
        }
        Nodo<T> n = raiz;
        while (n.der != nil) {
            n = n.der;
        }
        return n.dato;
    }

    /**
     * Primer elemento, recorriendo de menor a mayor, que cumple la condición;
     * null si ninguno la cumple. Sirve para "la solución más barata que
     * cubra X dB".
     */
    public T primeroQue(Predicate<? super T> condicion) {
        Nodo<T> n = minimo(raiz);
        while (n != nil) {
            if (condicion.test(n.dato)) {
                return n.dato;
            }
            n = sucesor(n);
        }
        return null;
    }

    /**
     * Elementos, de menor a mayor, desde el primero hasta que la condición
     * deje de cumplirse (se detiene ahí). Sirve para "todas las soluciones
     * con costo <= presupuesto".
     */
    public List<T> tomarMientras(Predicate<? super T> condicion) {
        List<T> lista = new ArrayList<>();
        Nodo<T> n = minimo(raiz);
        while (n != nil && condicion.test(n.dato)) {
            lista.add(n.dato);
            n = sucesor(n);
        }
        return lista;
    }

    /** Todos los elementos en orden ascendente. */
    public List<T> enOrden() {
        List<T> lista = new ArrayList<>(tamano);
        Nodo<T> n = minimo(raiz);
        while (n != nil) {
            lista.add(n.dato);
            n = sucesor(n);
        }
        return lista;
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacio() {
        return tamano == 0;
    }

    /** Altura del árbol (0 si está vacío). */
    public int altura() {
        return altura(raiz);
    }

    /** Comprueba las reglas del rojinegro y el orden del árbol binario de búsqueda. */
    public boolean cumpleInvariantes() {
        if (raiz.color != Color.NEGRO) {
            return false;
        }
        return alturaNegra(raiz, null, null) >= 0;
    }

    /**
     * Copia la estructura del árbol en nodos visuales para dibujarlo. El color
     * de cada nodo es el del rojo-negro (rojo o negro) y las hojas NIL se
     * incluyen, como en los diagramas.
     *
     * @param convertir crea el nodo visual (textos) de cada elemento; el color
     *                  lo pone el árbol
     */
    public NodoVisual aNodoVisual(Function<? super T, NodoVisual> convertir) {
        return raiz == nil ? null : aNodoVisual(raiz, convertir);
    }

    private NodoVisual aNodoVisual(Nodo<T> n, Function<? super T, NodoVisual> convertir) {
        if (n == nil) {
            return NodoVisual.nil();
        }
        NodoVisual v = convertir.apply(n.dato);
        v.setColor(n.color == Color.ROJO ? "#c62828" : "#161616");
        v.setIzq(aNodoVisual(n.izq, convertir));
        v.setDer(aNodoVisual(n.der, convertir));
        return v;
    }

    // ------------------------------------------------------------------
    // Reparación tras insertar
    // ------------------------------------------------------------------

    private void arreglarInsercion(Nodo<T> z) {
        while (z.padre.color == Color.ROJO) {
            if (z.padre == z.padre.padre.izq) {
                Nodo<T> tio = z.padre.padre.der;
                if (tio.color == Color.ROJO) {
                    // Caso 1: tío rojo -> se recolorea y se sube el problema
                    z.padre.color = Color.NEGRO;
                    tio.color = Color.NEGRO;
                    z.padre.padre.color = Color.ROJO;
                    z = z.padre.padre;
                } else {
                    if (z == z.padre.der) {
                        // Caso 2: triángulo -> se convierte en línea
                        z = z.padre;
                        rotarIzquierda(z);
                    }
                    // Caso 3: línea -> una rotación y recolorear
                    z.padre.color = Color.NEGRO;
                    z.padre.padre.color = Color.ROJO;
                    rotarDerecha(z.padre.padre);
                }
            } else {
                Nodo<T> tio = z.padre.padre.izq;
                if (tio.color == Color.ROJO) {
                    z.padre.color = Color.NEGRO;
                    tio.color = Color.NEGRO;
                    z.padre.padre.color = Color.ROJO;
                    z = z.padre.padre;
                } else {
                    if (z == z.padre.izq) {
                        z = z.padre;
                        rotarDerecha(z);
                    }
                    z.padre.color = Color.NEGRO;
                    z.padre.padre.color = Color.ROJO;
                    rotarIzquierda(z.padre.padre);
                }
            }
        }
        raiz.color = Color.NEGRO;
    }

    // ------------------------------------------------------------------
    // Reparación tras eliminar
    // ------------------------------------------------------------------

    private void arreglarEliminacion(Nodo<T> x) {
        while (x != raiz && x.color == Color.NEGRO) {
            if (x == x.padre.izq) {
                Nodo<T> w = x.padre.der; // hermano
                if (w.color == Color.ROJO) {
                    w.color = Color.NEGRO;
                    x.padre.color = Color.ROJO;
                    rotarIzquierda(x.padre);
                    w = x.padre.der;
                }
                if (w.izq.color == Color.NEGRO && w.der.color == Color.NEGRO) {
                    w.color = Color.ROJO;
                    x = x.padre;
                } else {
                    if (w.der.color == Color.NEGRO) {
                        w.izq.color = Color.NEGRO;
                        w.color = Color.ROJO;
                        rotarDerecha(w);
                        w = x.padre.der;
                    }
                    w.color = x.padre.color;
                    x.padre.color = Color.NEGRO;
                    w.der.color = Color.NEGRO;
                    rotarIzquierda(x.padre);
                    x = raiz;
                }
            } else {
                Nodo<T> w = x.padre.izq;
                if (w.color == Color.ROJO) {
                    w.color = Color.NEGRO;
                    x.padre.color = Color.ROJO;
                    rotarDerecha(x.padre);
                    w = x.padre.izq;
                }
                if (w.der.color == Color.NEGRO && w.izq.color == Color.NEGRO) {
                    w.color = Color.ROJO;
                    x = x.padre;
                } else {
                    if (w.izq.color == Color.NEGRO) {
                        w.der.color = Color.NEGRO;
                        w.color = Color.ROJO;
                        rotarIzquierda(w);
                        w = x.padre.izq;
                    }
                    w.color = x.padre.color;
                    x.padre.color = Color.NEGRO;
                    w.izq.color = Color.NEGRO;
                    rotarDerecha(x.padre);
                    x = raiz;
                }
            }
        }
        x.color = Color.NEGRO;
    }

    // ------------------------------------------------------------------
    // Utilidades internas
    // ------------------------------------------------------------------

    private void rotarIzquierda(Nodo<T> x) {
        Nodo<T> y = x.der;
        x.der = y.izq;
        if (y.izq != nil) {
            y.izq.padre = x;
        }
        y.padre = x.padre;
        if (x.padre == nil) {
            raiz = y;
        } else if (x == x.padre.izq) {
            x.padre.izq = y;
        } else {
            x.padre.der = y;
        }
        y.izq = x;
        x.padre = y;
    }

    private void rotarDerecha(Nodo<T> y) {
        Nodo<T> x = y.izq;
        y.izq = x.der;
        if (x.der != nil) {
            x.der.padre = y;
        }
        x.padre = y.padre;
        if (y.padre == nil) {
            raiz = x;
        } else if (y == y.padre.izq) {
            y.padre.izq = x;
        } else {
            y.padre.der = x;
        }
        x.der = y;
        y.padre = x;
    }

    /** Pone v en el lugar de u dentro del árbol. */
    private void trasplantar(Nodo<T> u, Nodo<T> v) {
        if (u.padre == nil) {
            raiz = v;
        } else if (u == u.padre.izq) {
            u.padre.izq = v;
        } else {
            u.padre.der = v;
        }
        v.padre = u.padre;
    }

    private Nodo<T> buscarNodo(T dato) {
        Nodo<T> actual = raiz;
        while (actual != nil) {
            int c = orden.compare(dato, actual.dato);
            if (c == 0) {
                return actual;
            }
            actual = c < 0 ? actual.izq : actual.der;
        }
        return nil;
    }

    private Nodo<T> minimo(Nodo<T> n) {
        while (n != nil && n.izq != nil) {
            n = n.izq;
        }
        return n;
    }

    private Nodo<T> sucesor(Nodo<T> n) {
        if (n.der != nil) {
            return minimo(n.der);
        }
        Nodo<T> p = n.padre;
        while (p != nil && n == p.der) {
            n = p;
            p = p.padre;
        }
        return p;
    }

    private int altura(Nodo<T> n) {
        return n == nil ? 0 : 1 + Math.max(altura(n.izq), altura(n.der));
    }

    /**
     * Devuelve la altura negra del subárbol, o -1 si viola alguna regla
     * (color, altura negra o orden entre min y max).
     */
    private int alturaNegra(Nodo<T> n, T min, T max) {
        if (n == nil) {
            return 1;
        }
        if ((min != null && orden.compare(n.dato, min) <= 0)
                || (max != null && orden.compare(n.dato, max) >= 0)) {
            return -1;
        }
        if (n.color == Color.ROJO && (n.izq.color == Color.ROJO || n.der.color == Color.ROJO)) {
            return -1;
        }
        int hi = alturaNegra(n.izq, min, n.dato);
        int hd = alturaNegra(n.der, n.dato, max);
        if (hi < 0 || hd < 0 || hi != hd) {
            return -1;
        }
        return hi + (n.color == Color.NEGRO ? 1 : 0);
    }
}