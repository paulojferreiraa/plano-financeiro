package br.com.portifinanceiro.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PlanoFinanceiroApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlanoFinanceiroApplication.class, args);
    }
}
