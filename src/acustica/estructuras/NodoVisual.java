package acustica.estructuras;

/**
 * Copia "para dibujar" de un nodo de un árbol: solo guarda lo que se ve
 * (textos, color, hijos), sin depender del tipo de dato del árbol.
 * Los árboles la generan con aNodoVisual(...) y GraficadorArboles la dibuja.
 */
public class NodoVisual {

    private final String linea1;
    private final String linea2;
    private final String detalle;      // texto que aparece al pasar el mouse
    private String color;              // relleno, formato CSS (#rrggbb)
    private final String colorTexto;
    private String insignia;           // etiqueta pequeña sobre el nodo (por ejemplo "fe=0"); puede ser null
    private final boolean nil;
    private NodoVisual izq;
    private NodoVisual der;

    public NodoVisual(String linea1, String linea2, String detalle, String color, String colorTexto) {
        this(linea1, linea2, detalle, color, colorTexto, false);
    }

    private NodoVisual(String linea1, String linea2, String detalle, String color,
            String colorTexto, boolean nil) {
        this.linea1 = linea1;
        this.linea2 = linea2;
        this.detalle = detalle;
        this.color = color;
        this.colorTexto = colorTexto;
        this.nil = nil;
    }

    /** Hoja NIL de un árbol rojo-negro (siempre negra). */
    public static NodoVisual nil() {
        return new NodoVisual("NIL", null, "Hoja NIL (siempre negra)", "#161616", "#ffffff", true);
    }

    public String getLinea1() {
        return linea1;
    }

    public String getLinea2() {
        return linea2;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getColorTexto() {
        return colorTexto;
    }

    public String getInsignia() {
        return insignia;
    }

    public void setInsignia(String insignia) {
        this.insignia = insignia;
    }

    public boolean isNil() {
        return nil;
    }

    public NodoVisual getIzq() {
        return izq;
    }

    public void setIzq(NodoVisual izq) {
        this.izq = izq;
    }

    public NodoVisual getDer() {
        return der;
    }

    public void setDer(NodoVisual der) {
        this.der = der;
    }
}
