# Reporte - Habitabilidad Acústica

Repo: https://github.com/bardock7238/Habitabilidad-acustica.git
Rama que revisamos: main, hasta el merge del PR #33 (lo del Main).
Fecha: 2026-09-24

Aclaración: buscamos issues abiertos y no había ninguno como tarea, solo los PR ya cerrados del 1 al 33. Así que hicimos este reporte general de cómo va el proyecto.

## De qué se trata
Es un programa en Java que simula cómo se mueve el ruido dentro de un edificio y dice si cada cuarto cumple o no la norma según si es de día o de noche. Está funcionando y se puede probar.

En total contamos 16 archivos .java con unas 1736 líneas. Lo más grande es la verificación del raytracing (234 líneas) y el Main (169). Los datos están en `datos/` con la norma OMS (5 filas) y los sectores de la Res. 0627 (9 filas). También hay un doc con los casos de prueba hechos a mano.

## Cómo está organizado
Todo está en `src/acustica/`:

- `dominio/`: lo básico, Material, FuenteSonido, Superficie, Habitacion y Edificio.
- `normativa/`: guarda los límites y el resultado por cuarto.
- `simulacion/`: el que corre todo, con la carpeta `propagacion/` (Grafo, Dijkstra, RayTracing, CalculoAcustico y las dos verificaciones).
- `config/`: el CargadorDatos que lee los CSV.
- `Main.java`: arma el ejemplo y lo corre.

En dominio la idea es simple: el Material tiene un coeficiente, la Superficie calcula `area * coeficiente` y eso es lo que frena el sonido. La Fuente solo guarda su nivel inicial con `obtenerNivelEmitido()`, y de ahí parte todo lo demás. La Habitacion guarda sus muros y sus fuentes, y el Edificio es solo la lista de cuartos.

En normativa tenemos los límites a mano en el código: Dormitorio-noche 30, Sala-día 35, etc. Si se pide algo que no existe, por ejemplo Sala-noche, tira error. El Resultado solo guarda nivel, límite y si pasó o no, y calcula el exceso para ver el peor caso. Algo que notamos es que Normativa todavía no usa el CargadorDatos, sería bueno conectarlos para no tener los valores quemados.

En simulación el Grafo convierte cada cuarto en un nodo y cada muro en una conexión, las fachadas van al nodo EXTERIOR. Dijkstra busca el camino que menos atenúa y RayTracing reparte N rayos en 360 grados según el área de cada muro. Al final todo se suma con la fórmula `10*log10(suma(10^(Li/10)))`. El Simulador filtra las fuentes por horario, suma los aportes y compara con la norma. Si no hay límite solo saca un aviso y sigue.

## El ejemplo del Main
Armamos un edificio de 6 apartamentos en fila (101 al 106), cada uno con su dormitorio y su sala, o sea 12 cuartos. Se hace con un for porque son iguales.

Materiales que usamos: concreto para medianeras y fachadas, drywall para la división interna y vidrio doble para ventanas.

Fuentes que pusimos (6):
- TV 65 dB en Sala 101 de día
- Ventilador 28 dB en Dorm 101 de noche
- Aspiradora 70 dB en Sala 102 de día
- Parlante 55 dB en Dorm 103 de noche
- Lavadora 58 dB en Sala 105 de día
- Conversación 45 dB en Dorm 106 de noche

El Main primero muestra cada fuente con su nivel y después corre día y noche e imprime cuarto por cuarto si es habitable y cuál fue el peor.

## Pruebas
Tenemos el doc `casos-de-prueba-raytracing.md` con 3 casos calculados a mano con N=40 para que los números den exactos:
1. Dos cuartos: 76.61 en sala, 50.00 en dormitorio.
2. Con pasillo: 74.17 y 56.00 pasando por dos puertas.
3. Con vía exterior de 85 dB: 55 en sala y 25 en dormitorio.

Y dos clases para verificar: VerificacionGrafo y VerificacionRayTracing (esta última debería dar 19/19 OK). Se corren así:
```
javac -d out src/acustica/**/*.java
java -cp out acustica.Main
```

Acá no lo pudimos compilar porque no había java instalado, lo revisamos leyendo el código.

## Git
Trabajamos con main estable y ramas feature por persona, con PR y revisión de otro compañero. Hay 11 ramas y unos 31 commits en main. Lo último fue el PR #33 del Main por JuliGR05.

## Cosas por mejorar
- Conectar Normativa con CargadorDatos para no quemar los valores.
- Material ignora la frecuencia y Superficie ignora el espesor, por ahora es simplificado.
- El volumen del cuarto no se usa, el raytracing es 2D solo por área.
- Cuando no hay límite (Sala-noche) solo sale un aviso, mejor dejarlo como NO EVALUABLE.
- Faltan tests con JUnit, solo tenemos los mains de verificación.

## Siguiente paso
Sería bueno abrir un issue para lo de Normativa + CargadorDatos y lo de Sala-noche no evaluable, que es lo que más se nota.
