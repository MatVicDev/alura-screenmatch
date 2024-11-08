package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";
    private List<DadosSerie> dadosSeries = new ArrayList<>();
    private SerieRepository repository;

    private List<Serie> series = new ArrayList<>();

    private Optional<Serie> serieBuscada;

    public Principal(SerieRepository repository) {
        this.repository = repository;
    }

    public void exibeMenu() {
        var opcao = -1;

        while (opcao != 0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Exibir séries procuradas
                    4 - Buscar série por título
                    5 - Buscar série por ator
                    6 - Top 5 séries
                    7 - Buscar séries por categoria
                    8 - Filtrar séries por temporada e avaliação
                    9 - Buscar trecho do episódio da série
                    10 - Buscar as 5 melhores séries
                    0 - Sair                                 
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarTopCincoSeries();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    filtrarSeriesPorTemporadaEAvaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodioPorSerie();
                    break;
                case 11:
                    buscarEpisodioDepoisDeUmaData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        repository.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.print("Digite o nome da série para busca: ");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie() {
        listarSeriesBuscadas();
        System.out.print("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = series.stream()
                .filter(s -> s.getTitulo().toLowerCase().contains(nomeSerie.toLowerCase()))
                .findFirst();

        if (serie.isPresent()) {
            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(t -> t.episodios().stream()
                            .map(e -> new Episodio(t.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);

            repository.save(serieEncontrada);
        } else {
            System.out.println("Série não encontrada!");
        }
    }

    private void listarSeriesBuscadas() {
        series = repository.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.print("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        this.serieBuscada = repository.findByTituloContainingIgnoreCase(nomeSerie);

        if (this.serieBuscada.isPresent()) {
            System.out.println("Dados da série: " + this.serieBuscada.get());
        } else {
            System.out.println("Série não encontrada!");
        }
    }

    private void buscarSeriesPorAtor() {
        System.out.print("Digite o nome do ator: ");
        var nomeAtor = leitura.nextLine();
        System.out.print("Avaliação a partir de: ");
        var avaliacao = leitura.nextDouble();
        List<Serie> series = repository.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        series.forEach(s -> System.out.println(s.getTitulo() + " [" + s.getAvaliacao() + "]"));
    }

    private void buscarTopCincoSeries() {
        List<Serie> primeirasCincoSeries = repository.findTop5ByOrderByAvaliacaoDesc();
        primeirasCincoSeries.forEach(s -> System.out.println(s.getTitulo() + " [" + s.getAvaliacao() + "]"));
    }

    private void buscarSeriesPorCategoria() {
        System.out.print("Buscar série por gênero: ");

        String genero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(genero);
        List<Serie> series = repository.findByGenero(categoria);

        series.forEach(s -> System.out.println(s.getTitulo()));
    }

    private void filtrarSeriesPorTemporadaEAvaliacao() {
        System.out.print("Filtrar quantas temporadas? ");
        var totalTemporadas = leitura.nextInt();
        leitura.nextLine();

        System.out.print("Digite uma avaliação: ");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();

        List<Serie> series = repository.seriesPorTemporadaEAvaliacao(totalTemporadas, avaliacao);
        System.out.println("Séries encontradas: ");

        series.forEach(s -> System.out.println(s.getTitulo() + " - avaliação: " + s.getAvaliacao()));
    }

    private void buscarEpisodioPorTrecho() {
        System.out.print("Buscar episódio: ");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodiosEncontrados = repository.episodioPorTrecho(trechoEpisodio);
        episodiosEncontrados.forEach(System.out::println);
    }

    private void topEpisodioPorSerie() {
        buscarSeriePorTitulo();

        if (this.serieBuscada.isPresent()) {
            Serie serie = serieBuscada.get();
            List<Episodio> topEpisodio = repository.topEpisodiosPorSerie(serie);
            topEpisodio.forEach(System.out::println);
        }
    }

    private void buscarEpisodioDepoisDeUmaData() {
        buscarSeriePorTitulo();

        if (this.serieBuscada.isPresent()) {
            Serie serie = serieBuscada.get();
            System.out.println("Digite o ano limite de lançamento");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();

            List<Episodio> episodiosAno = repository.episodioPorSerieEAno(serie, anoLancamento);

            episodiosAno.forEach(System.out::println);
        }
    }
}
