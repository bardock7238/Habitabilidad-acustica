package acustica.estructuras;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Árbol AVL genérico (árbol binario de búsqueda autobalanceado).
 *
 * En cada nodo la diferencia de alturas entre el subárbol izquierdo y el
 * derecho (factor de equilibrio) es -1, 0 o 1. Si una inserción o una
 * eliminación rompe eso, se corrige con rotaciones. Por eso buscar, insertar
 * y eliminar cuestan O(log n) siempre.
 *
 * El orden lo define el Comparator: dos elementos que comparen igual
 * se consideran el MISMO elemento (no se admiten duplicados), así que el
 * comparador debe incluir un desempate (por ejemplo, el id de la habitación).
 */
public class ArbolAVL<T> {

    private static class Nodo<T> {
        T dato;
        Nodo<T> izq;
        Nodo<T> der;
        int altura = 1;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private final Comparator<? super T> orden;
    private Nodo<T> raiz;
    private int tamano;

    public ArbolAVL(Comparator<? super T> orden) {
        if (orden == null) {
            throw new IllegalArgumentException("El comparador no puede ser null");
        }
        this.orden = orden;
    }

    // ------------------------------------------------------------------
    // Operaciones públicas
    // ------------------------------------------------------------------

    /** Inserta el dato. Devuelve false si ya había uno equivalente. */
    public boolean insertar(T dato) {
        if (dato == null) {
            throw new IllegalArgumentException("No se pueden insertar datos null");
        }
        int antes = tamano;
        raiz = insertar(raiz, dato);
        return tamano > antes;
    }

    /** Elimina el dato equivalente al indicado. Devuelve false si no estaba. */
    public boolean eliminar(T dato) {
        int antes = tamano;
        raiz = eliminar(raiz, dato);
        return tamano < antes;
    }

    public boolean contiene(T dato) {
        Nodo<T> actual = raiz;
        while (actual != null) {
            int c = orden.compare(dato, actual.dato);
            if (c == 0) {
                return true;
            }
            actual = c < 0 ? actual.izq : actual.der;
        }
        return false;
    }

    /** Elemento más pequeño según el orden, o null si el árbol está vacío. */
    public T minimo() {
        if (raiz == null) {
            return null;
        }
        Nodo<T> n = raiz;
        while (n.izq != null) {
            n = n.izq;
        }
        return n.dato;
    }

    /** Elemento más grande según el orden, o null si el árbol está vacío. */
    public T maximo() {
        if (raiz == null) {
            return null;
        }
        Nodo<T> n = raiz;
        while (n.der != null) {
            n = n.der;
        }
        return n.dato;
    }

    /** Todos los elementos en orden ascendente. */
    public List<T> enOrden() {
        List<T> lista = new ArrayList<>(tamano);
        enOrden(raiz, lista);
        return lista;
    }

    /**
     * Elementos estrictamente MAYORES que limite, en orden
     * ascendente. Solo recorre las ramas necesarias (O(log n + k)).
     */
    public List<T> mayoresQue(T limite) {
        List<T> lista = new ArrayList<>();
        mayoresQue(raiz, limite, lista);
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

    /** Comprueba que TODOS los nodos cumplen |factor de equilibrio| <= 1 y las alturas guardadas son correctas. */
    public boolean estaBalanceado() {
        return verificar(raiz) >= 0;
    }

    /**
     * Copia la estructura del árbol en nodos visuales para dibujarlo.
     * Cada nodo lleva como insignia su factor de equilibrio (fe).
     *
     * @param convertir crea el nodo visual (textos y color) de cada elemento
     */
    public NodoVisual aNodoVisual(Function<? super T, NodoVisual> convertir) {
        return aNodoVisual(raiz, convertir);
    }

    private NodoVisual aNodoVisual(Nodo<T> n, Function<? super T, NodoVisual> convertir) {
        if (n == null) {
            return null;
        }
        NodoVisual v = convertir.apply(n.dato);
        v.setInsignia("fe=" + factor(n));
        v.setIzq(aNodoVisual(n.izq, convertir));
        v.setDer(aNodoVisual(n.der, convertir));
        return v;
    }

    // ------------------------------------------------------------------
    // Inserción / eliminación
    // ------------------------------------------------------------------

    private Nodo<T> insertar(Nodo<T> nodo, T dato) {
        if (nodo == null) {
            tamano++;
            return new Nodo<>(dato);
        }
        int c = orden.compare(dato, nodo.dato);
        if (c < 0) {
            nodo.izq = insertar(nodo.izq, dato);
        } else if (c > 0) {
            nodo.der = insertar(nodo.der, dato);
        } else {
            return nodo; // duplicado: no se inserta
        }
        return rebalancear(nodo);
    }

    private Nodo<T> eliminar(Nodo<T> nodo, T dato) {
        if (nodo == null) {
            return null;
        }
        int c = orden.compare(dato, nodo.dato);
        if (c < 0) {
            nodo.izq = eliminar(nodo.izq, dato);
        } else if (c > 0) {
            nodo.der = eliminar(nodo.der, dato);
        } else {
            if (nodo.izq == null || nodo.der == null) {
                tamano--;
                return nodo.izq != null ? nodo.izq : nodo.der;
            }
            // Dos hijos: se reemplaza por el sucesor (mínimo del subárbol derecho)
            Nodo<T> sucesor = nodo.der;
            while (sucesor.izq != null) {
                sucesor = sucesor.izq;
            }
            nodo.dato = sucesor.dato;
            nodo.der = eliminar(nodo.der, sucesor.dato);
        }
        return rebalancear(nodo);
    }

    // ------------------------------------------------------------------
    // Equilibrio y rotaciones
    // ------------------------------------------------------------------

    private int altura(Nodo<T> n) {
        return n == null ? 0 : n.altura;
    }

    private void actualizarAltura(Nodo<T> n) {
        n.altura = 1 + Math.max(altura(n.izq), altura(n.der));
    }

    /** Factor de equilibrio = altura(izq) - altura(der). */
    private int factor(Nodo<T> n) {
        return n == null ? 0 : altura(n.izq) - altura(n.der);
    }

    private Nodo<T> rebalancear(Nodo<T> n) {
        actualizarAltura(n);
        int f = factor(n);

        if (f > 1) { // pesado a la izquierda
            if (factor(n.izq) < 0) {
                n.izq = rotarIzquierda(n.izq); // caso izquierda-derecha
            }
            return rotarDerecha(n);
        }
        if (f < -1) { // pesado a la derecha
            if (factor(n.der) > 0) {
                n.der = rotarDerecha(n.der); // caso derecha-izquierda
            }
            return rotarIzquierda(n);
        }
        return n;
    }

    private Nodo<T> rotarDerecha(Nodo<T> y) {
        Nodo<T> x = y.izq;
        y.izq = x.der;
        x.der = y;
        actualizarAltura(y);
        actualizarAltura(x);
        return x;
    }

    private Nodo<T> rotarIzquierda(Nodo<T> x) {
        Nodo<T> y = x.der;
        x.der = y.izq;
        y.izq = x;
        actualizarAltura(x);
        actualizarAltura(y);
        return y;
    }

    // ------------------------------------------------------------------
    // Recorridos y verificación
    // ------------------------------------------------------------------

    private void enOrden(Nodo<T> n, List<T> salida) {
        if (n == null) {
            return;
        }
        enOrden(n.izq, salida);
        salida.add(n.dato);
        enOrden(n.der, salida);
    }

    private void mayoresQue(Nodo<T> n, T limite, List<T> salida) {
        if (n == null) {
            return;
        }
        if (orden.compare(n.dato, limite) > 0) {
            mayoresQue(n.izq, limite, salida);
            salida.add(n.dato);
        }
        mayoresQue(n.der, limite, salida);
    }

    /** Devuelve la altura real del subárbol, o -1 si encuentra un nodo mal balanceado. */
    private int verificar(Nodo<T> n) {
        if (n == null) {
            return 0;
        }
        int hi = verificar(n.izq);
        int hd = verificar(n.der);
        if (hi < 0 || hd < 0 || Math.abs(hi - hd) > 1 || n.altura != 1 + Math.max(hi, hd)) {
            return -1;
        }
        return 1 + Math.max(hi, hd);
    }
}