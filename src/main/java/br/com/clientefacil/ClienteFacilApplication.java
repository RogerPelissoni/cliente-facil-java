package br.com.clientefacil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: liga o job de limpeza de dados (DataRetentionService) — único uso de
// @Scheduled no projeto hoje.
@EnableScheduling
@SpringBootApplication
public class ClienteFacilApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClienteFacilApplication.class, args);
    }

}
