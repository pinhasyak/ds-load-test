package hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;

@SpringBootApplication
public class Application {
    private static final String HOST = "http://localhost:8894";

    private static final String KIBANA_CSV = "/Users/pinhas/Desktop/kibana_stage.csv";

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);


    public static void main(String args[]) {
        SpringApplication.run(Application.class);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public CommandLineRunner run(RestTemplate restTemplate) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1000);
        return args -> {
            try (Stream<String> stream = Files.lines(Paths.get(KIBANA_CSV))) {
                stream.filter(e -> e.contains("GET"))
                        .map(e -> e.substring(e.indexOf("GET/") + 3, e.indexOf(" HTTP/1.1")))
                        .map(e -> HOST + e)
                        .forEach(url -> requestDs(pool, restTemplate, url)
                                        .thenApply(response -> checkResponse(url, response))
                                        .thenAccept(LOGGER::info));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    private CompletableFuture<String> requestDs(ExecutorService pool, RestTemplate restTemplate, String url){
        return CompletableFuture
                .supplyAsync(() -> (restTemplate.getForObject(url, String.class)), pool);
    }

    private String checkResponse(String url, String response){
        return (response.contains("\"status\":{\"code\":\"OK\"}")) ? url +": OK" : url+ ": FAIL";
    }

}