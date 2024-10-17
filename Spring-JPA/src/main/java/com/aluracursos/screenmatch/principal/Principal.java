package com.aluracursos.screenmatch.principal;

import com.aluracursos.screenmatch.model.*;
import com.aluracursos.screenmatch.repository.SerieRepository;
import com.aluracursos.screenmatch.service.ConsumoAPI;
import com.aluracursos.screenmatch.service.ConvierteDatos;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private Dotenv dotenv = Dotenv.load(); // Load environment variables
    private final String API_KEY = "&apikey=" + dotenv.get("API_KEY"); // Get the API_KEY from the .env
    private final String URL_BASE = dotenv.get("URL_BASE");
    private ConvierteDatos conversor = new ConvierteDatos();
    private List<DatosSerie> datosSeries = new ArrayList<>();
    private SerieRepository repository;
    private List<Serie> series;
    private Optional<Serie> serieBuscada;

    public Principal(SerieRepository repository) {
        this.repository = repository;
    }

    public void muestraElMenu() {

        int opcion;
        do {
            mostrarMenu();
            opcion = obtenerOpcion();
            procesarOpcion(opcion);
        } while (opcion != 0);
    }

    private void mostrarMenu() {
        System.out.println("###################################################");
            var menu = """
                1 - Buscar series. 
                2 - Buscar episodios.
                3 - Mostrar series buscadas.
                4 - Buscar series por titulo.    
                5 - Top 5 mejores Series.    
                6 - Buscar Series por Categoria.  
                7 - Filtrar Series por temporada y evaluación.    
                8 - Buscar episodis por titulo.
                9 - Top 5 episodios por Serie.
                
                0 - Salir.
                """;
        System.out.println(menu);
        System.out.println("###################################################");
    }

    private int obtenerOpcion() {
        int opcion = -1;
        while (opcion < 0 || opcion > 9) {
            System.out.print("Seleccione una opción: ");
            if (teclado.hasNextInt()) {
                opcion = teclado.nextInt();
                teclado.nextLine();
            } else {
                System.out.println("Entrada no válida. Por favor, ingrese un número entre 0 y 9.");
                teclado.next();
            }
        }
        return opcion;
    }

    private void procesarOpcion(int opcion) {
        switch (opcion) {
            case 1:
                buscarSerieWeb();
                break;
            case 2:
                buscarEpisodioPorSerie();
                break;
            case 3:
                mostrarSeriesBuscadas();
                break;
            case 4:
                buscarSeriesPorTitulo();
                break;
            case 5:
                buscarTop5Series();
                break;
            case 6:
                buscarSeriePorCategoria();
                break;
            case 7:
                filtrarSeriesPorTemporadaYEvaluacion();
                break;
            case 8:
                buscarEpisodiosPorTitulo();
                break;
            case 9:
                buscarTop5Episodios();
                break;
            case 0:
                System.out.println("Cerrando la aplicación...");
                break;
            default:
                System.out.println("Opción inválida");
        }
    }



    private DatosSerie getDatosSerie() {
        System.out.println("Escribe el nombre de la serie que deseas buscar");
        var nombreSerie = teclado.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + nombreSerie.replace(" ", "+") + API_KEY);
        System.out.println(json);
        DatosSerie datos = conversor.obtenerDatos(json, DatosSerie.class);
        return datos;
    }
    private void buscarEpisodioPorSerie() {
        mostrarSeriesBuscadas();
        System.out.println("Escribe el nombre de la Serie, de la cual deseas ver los episodios.");
        var nombreSerie = teclado.nextLine();

        Optional<Serie> serie = series.stream().filter(s -> s.getTitulo().toLowerCase()
                        .contains(nombreSerie.toLowerCase()))
                .findFirst();

        if(serie.isPresent()) {
            var serieEncontrada = serie.get();
            List<DatosTemporadas> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumoApi.obtenerDatos(URL_BASE + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DatosTemporadas datosTemporada = conversor.obtenerDatos(json, DatosTemporadas.class);
                temporadas.add(datosTemporada);
            }
            temporadas.forEach(System.out::println);
            List<Episodio> episodios = temporadas.stream().flatMap(d -> d.episodios().stream()
                    .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repository.save(serieEncontrada);
        }
    }


    private void buscarSerieWeb() {
        DatosSerie datos = getDatosSerie();
        //datosSeries.add(datos);
        Serie serie = new Serie(datos);
        repository.save(serie);
        System.out.println(datos);
    }

    private void mostrarSeriesBuscadas() {
        series = repository.findAll();

        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriesPorTitulo() {
        System.out.println("Escribe el nombre de la Serie, que deseas buscar.");
        var nombreSerie = teclado.nextLine();

        serieBuscada = repository.findByTituloContainsIgnoreCase(nombreSerie);

        if(serieBuscada.isPresent()) {
            System.out.println("La serie buscada es: " + serieBuscada.get());
        } else {
            System.out.println("Serie no encontrada");
        }
    }

    private void buscarTop5Series() {
        List<Serie> topSeries = repository.findTop5ByOrderByEvaluacionDesc();
        topSeries.forEach(s ->
                System.out.println("Serie: " + s.getTitulo() + " Evaluación: " + s.getEvaluacion()));
    }

    private void buscarSeriePorCategoria() {
        System.out.println("Escriba el genero/categoria de la Serie que desea buscar.");
        var genero = teclado.nextLine();

        var categoria = Categoria.fromEspaniol(genero);
        List<Serie> seriesPorCategoria = repository.findByGenero(categoria);
        System.out.println("La series de la categoria  " + genero);
        seriesPorCategoria.forEach(System.out::println);
    }

    public void filtrarSeriesPorTemporadaYEvaluacion() {
        System.out.println("¿Filtrar series con cuantas temporadas?");
        var totalTemporadas = teclado.nextInt();
        teclado.nextLine();
        System.out.println("¿Con evaluación a partir de cual valor?");
        var evaluacion = teclado.nextDouble();
        teclado.nextLine();
        List<Serie> filtroSeries = repository.seriesPorTemporadaYEvaluacion();
        System.out.println(" = > Series filtradas < = ");
        filtroSeries.forEach(s ->
                System.out.println(s.getTitulo() + " * evaluación: " + s.getEvaluacion()));
    }

    private void buscarEpisodiosPorTitulo() {
        System.out.println("Escribe el nombre del episodio que deseas buscar.");
        var nombreEpisodio = teclado.nextLine();
        List<Episodio> episodiosEncontrados = repository.episodiosPorNombre(nombreEpisodio);
        episodiosEncontrados.forEach(e -> System.out.printf("Serie: %s  Temporada %s Episodio %s Evaluacion %s\n",
                e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getEvaluacion()));
    }

    public void buscarTop5Episodios() {
        buscarSeriesPorTitulo();
        if (serieBuscada.isPresent()) {
            Serie serie = serieBuscada.get();
            List<Episodio> topEpisodios = repository.top5Episodios(serie);
            topEpisodios.forEach(e -> System.out.printf("Serie: %s # Temporada %s # Episodio %s # Evaluacion %s\n",
                    e.getSerie().getTitulo(), e.getTemporada(), e.getTitulo(), e.getEvaluacion()));
        }
    }
}

