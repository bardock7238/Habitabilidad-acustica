# Reporte de Proyecto — Habitabilidad Acústica
**Repo:** https://github.com/bardock7238/Habitabilidad-acustica.git  
**Rama analizada:** `main` (commit `fd93f92` — merge PR #33 "Creación del Main")  
**Fecha reporte:** 2026-09-24  
**Clon local:** `./Habitabilidad-acustica/` (descargado como ZIP, 26 archivos) + copia en `%TEMP%\hab-acustica`  
**Nota Issues:** la API `issues?state=all` no devuelve issues tipo tarea (solo PRs 1..33 cerrados/mergeados). No hay "último issue" pendiente. Este reporte cubre el estado completo como entregable.

## 1. Resumen ejecutivo
Proyecto Java de simulación de propagación de ruido en edificaciones y evaluación de habitabilidad según normativa. Estado: funcional y verificable. `main` compila según README, tiene escenario demo completo (6 apartamentos, 12 recintos, 6 fuentes), motor grafo+Dijkstra+RayTracing, carga de CSVs y 2 suites de verificación (grafo + raytracing).

Métricas reales medidas en local:
- 16 archivos `.java`, **1736 líneas** totales
- `Main.java:169`, `CargadorDatos.java:156`, `VerificacionRayTracing.java:234`, `VerificacionGrafo.java:149`, `CalculoAcustico.java:138`, `Dijkstra.java:128`, `Grafo.java:126`, `RayTracing.java:123`, `Simulador.java:94`
- Datos: `normativa_oms.csv` (5 filas), `sectores.csv` (9 filas Res. 0627/2006)
- Docs: `casos-de-prueba-raytracing.md` (3 escenarios, cálculo a mano)
- Ramas remotas: 11 (`main`, `caracteristica/normativa`, `feature/calcul o-acustico|config-datos|fuente|grafo|material|normativa|raytracing|simulador|verificacion-grafo`). Historial: 31 commits en `main`.

## 2. Objetivo
Simular cuánto ruido llega a cada habitación tras atravesar muros/fachadas y decidir HABITABLE / NO HABITABLE comparando contra límite normativo por tipo de recinto y horario (día/noche).

## 3. Arquitectura — `src/acustica/`
```
dominio/      Material, FuenteSonido, Superficie, Habitacion, Edificio
normativa/    Normativa, ResultadoHabitabilidad
simulacion/   Simulador + propagacion/{Grafo, Dijkstra, RayTracing, CalculoAcustico, VerificacionGrafo, VerificacionRayTracing}
config/       CargadorDatos
Main.java
```

### 3.1 Dominio (`dominio/`)
- `Material.java:28` — `nombre, atenuacion(coef dB), espesor`. `obtenerAtenuacion(frec)` ignora frecuencia (retorna constante). Simplificación documentada.
- `FuenteSonido.java:93` — `id, Tipo{FIJA,MOVIL}, nivelSonido dB, frecuencia Hz, Horario{DIA,NOCHE}`. `obtenerNivelEmitido()` es el punto de partida de la simulación.
- `Superficie.java:56` — `id, tipo, area, material, hab1, hab2?`. `calcularAtenuacion() = area * coefMaterial`. `esParedInterior() = hab2 != null`. Fachada = 1 sola habitación.
- `Habitacion.java:89` — `id, nombre, tipo, ancho/largo/alto`, listas superficies+fuentes, `nivelRuido, habitable`. `calcularVolumen()`, `evaluarHabitabilidad(normativa, horario)`.
- `Edificio.java:46` — `nombre, normativaAplicable`, lista habitaciones, `buscarHabitacion(id)`.

### 3.2 Normativa (`normativa/`)
- `Normativa.java:50` — Map `tipo-horario -> limite` en minúsculas. Hardcodea valores de `normativa_oms.csv`: `Dormitorio-noche 30`, `Sala-dia 35`, `Aula-dia 35`, `Hospital-dia 30`, `Hospital-noche 40`. `cumple(nivel,tipo,horario) = nivel <= limite`. Lanza si combinación inexistente (ej. `Sala-noche` no existe).
- `ResultadoHabitabilidad.java:57` — inmutable: `habitacion, nivelFinal, limiteAplicado, habitable`. `getExceso() = nivel-limite`. `toString()` imprime `nivel, limite -> HABITABLE|NO HABITABLE`.
- Datos: `datos/normativa_oms.csv` + `datos/sectores.csv` (sectores A/B/C/D Res. 0627 con límites día/noche 55-80 dB). `CargadorDatos.java:156` lee ambos CSV (respeta comillas, salta encabezado). `Normativa` hoy no usa `CargadorDatos` — mejora pendiente: inyectar mapa cargado en vez de hardcodear.

### 3.3 Simulación (`simulacion/`)
- `Grafo.java:126` — nodos = ids habitación + `EXTERIOR`. Arista no dirigida por superficie interior (peso=atenuación); fachada conecta a `EXTERIOR`. Evita duplicados por `superficieId`.
- `Dijkstra.java:128` — clásico con `PriorityQueue`, pesos >=0. `Resultado{atenuacionAcumulada, predecesor}`, `getAtenuacionHasta()`, `reconstruirCamino()`.
- `RayTracing.java:123` — 2D, `N` rayos a `360/N` grados. Cada superficie ocupa `(area_k/areaTotal)*360°`. Asigna impacto por ángulo acumulado. `Rayo{angulo, nivelInicial, superficieImpactada?}`.
- `CalculoAcustico.java:138` — `calcularNivelRayo = inicial - atenuacion`, `sumarNiveles = 10*log10(sum(10^(Li/10)))`, `calcularNivelHabitacion()`, `propagarEnHabitacion()`, `calcularNivelEnDestino(fuente, origenId, destinoId, grafo) = emitido - atenuacionDijkstra`.
- `Simulador.java:94` — `ejecutar(horario)`: construye grafo, para cada destino suma aporte energético de todas las fuentes activas en ese horario (`fuente.horario == horario`), guarda `nivelRuido`, evalúa normativa. Si falta límite, imprime `Aviso: sin limite...` y continúa.
- Modelo físico: `atenuación = área·coef`, suma incoherente. No es ley física real — es el modelo pactado Tareas 1.x/4.x.

## 4. Escenario `Main.java:169`
Edificio residencial 6 aptos en fila (101-106), cada uno dormitorio (3x3.5x2.5) + sala (4x4.5x2.5) = 12 recintos. Generado en bucle.
- Materiales: Concreto 6.0/0.2 (medianeras+fachadas), Drywall 4.0/0.1 (partición interna 7m²), Vidrio doble 20.0/0.01 (ventana 1.3m²).
- Superficies: muro interno por apto, ventana dormitorio, fachada sala 9.5m², 5 medianeras 8m² entre dormitorios vecinos.
- Fuentes (6): F-TV 65dB Sala101 DIA, F-VE 28dB Dorm101 NOCHE, F-AS 70dB Sala102 DIA MOVIL, F-PL 55dB Dorm103 NOCHE MOVIL, F-LV 58dB Sala105 DIA, F-CV 45dB Dorm106 NOCHE MOVIL.
- Flujo: imprime `obtenerNivelEmitido()`, luego `Simulador.ejecutar("dia")` y `("noche")`, imprime cada `ResultadoHabitabilidad` + peor caso por exceso.

## 5. Verificación y docs
- `docs/casos-de-prueba-raytracing.md`: 3 escenarios con cálculo a mano (N=40, cortes exactos 9°): (1) 2 habs 76.61dB sala / 50.00dB dorm, (2) pasillo 74.17dB / 56.00dB vía 2 puertas, (3) vía exterior 85dB -> 55dB sala / 25dB dorm + tabla normativa.
- `VerificacionGrafo.java:149` — 17 checks aprox. (conectividad ambos sentidos, EXTERIOR, camino y atenuación suma puertas).
- `VerificacionRayTracing.java:234` — 19 checks, tolerancia 0.05dB. Salida esperada `19/19 OK`.
- Comandos:
```bash
javac -d out src/acustica/**/*.java
java -cp out acustica.Main
javac -d out src/acustica/dominio/*.java src/acustica/normativa/*.java src/acustica/simulacion/propagacion/*.java
java -cp out acustica.simulacion.propagacion.VerificacionGrafo
java -cp out acustica.simulacion.propagacion.VerificacionRayTracing
```
Entorno actual: sin `git/gh/java` en PATH — no se pudo compilar aquí. Se verificó por lectura estática.

## 6. Metodología Git
- `main` estable, integración vía PR, revisión cruzada (según README).
- Ramas por integrante: `feature/material|fuente|habitacion|superficie|edificio|normativa` + nuevas `feature/grafo|raytracing|simulador|calculo-acustico|config-datos|verificacion-grafo`, `caracteristica/normativa`.
- Último merge: PR #33 `Creación del Main` por `JuliGR05`, mergeado 2026-09-24. Sin issues abiertos tipo tarea.

## 7. Hallazgos / deuda técnica
1. `Normativa` hardcodea límites; debería cargar vía `CargadorDatos.cargarNormativaOMS()`.
2. `Material.obtenerAtenuacion(frecuencia)` ignora frecuencia; `Superficie` ignora espesor.
3. `Habitacion.volumen` no interviene en acústica; RayTracing 2D solo por área.
4. `Simulador` silencia `Sala-noche/Dormitorio-dia` (sin límite OMS) solo con `println` — conviene resultado `NO EVALUABLE`.
5. `Dijkstra` no pondera distancia/frecuencia, solo atenuación acumulada.
6. Sin tests unitarios (JUnit); solo mains de verificación. Sin CI.

## 8. Sobre clon y merge request
- Clon: en esta máquina no hay `git` ni `gh` en PATH, así que se descargó `main.zip` y se copió a `./Habitabilidad-acustica/`. Equivale a `git clone --branch main`.
- MR: sin token GitHub no se puede abrir PR desde aquí. Para enviar este reporte como MR:
```powershell
# una vez con git+gh instalados y autenticados:
cd Habitabilidad-acustica
git init; git remote add origin https://github.com/bardock7238/Habitabilidad-acustica.git
git checkout -b docs/reporte-proyecto
git add REPORTE_PROYECTO.md
git commit -m "docs: agregar reporte de proyecto"
git push -u origin docs/reporte-proyecto
gh pr create --base main --head docs/reporte-proyecto --title "docs: reporte de proyecto" --body "Agrega REPORTE_PROYECTO.md. Sin issues abiertos; reporte general del estado main."
```
- Este archivo `REPORTE_PROYECTO.md` ya está listo en `./Habitabilidad-acustica/` para ese flujo.

## 9. Recomendación siguiente issue (si se quiere crear)
`docs: integrar CargadorDatos a Normativa + test Sala-noche no evaluable` — elimina hardcode, cubre hueco normativo detectado en §7.4.
