package com.ticketya.entrada;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ticketya.entrada.client")
@SpringBootApplication
public class MsvcEntradaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcEntradaApplication.class, args);
    }

}
