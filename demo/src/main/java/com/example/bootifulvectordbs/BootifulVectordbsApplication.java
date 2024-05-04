package com.example.bootifulvectordbs;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class BootifulVectordbsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootifulVectordbsApplication.class, args);
    }

    @Bean
    TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    private final boolean ingest = false;

    @Bean
    ApplicationRunner demo(
            TokenTextSplitter tokenTextSplitter,
            JdbcClient db, VectorStore vectorStore) {
        return args -> {

            var products = db.sql("select * from products")
                    .query(new DataClassRowMapper<>(Product.class))
                    .list();

            if (this.ingest) {

                products
                        .parallelStream()
                        .forEach(product -> {
                            var document = new Document(
                                    product.name() + " " + product.description(),
                                    Map.of("price", product.price(),
                                            "id", product.id(),
                                            "sku", product.sku(),
                                            "name", product.name(),
                                            "description", product.description()
                                    ));

                            var split = tokenTextSplitter.apply(List.of(document));
                            vectorStore.add(split);
                        });

            }

//            var single = products
//                    .parallelStream()
//                    .filter(product -> product.id() == 115)
//                    .toList()
//                    .getFirst();

            var similar = vectorStore
                    .similaritySearch(  SearchRequest.query("cold weather").withTopK(10) );
            System.out.println("count: "+similar.size());
            for (var s : similar) {
                System.out.println("===========");
                var id = (s.getMetadata().get("id"));
                System.out.println("id: " + id);
                s.getMetadata().forEach( (k,v) -> System.out.println(k +'='+v ));
            }


        };
    }

}


record Product(int id, String name, String sku, String description, float price) {
}
