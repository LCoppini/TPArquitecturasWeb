package com.ticketya.preferencias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ticketya.preferencias.client")
@SpringBootApplication
public class MsvcPreferenciasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcPreferenciasApplication.class, args);
    }

}
