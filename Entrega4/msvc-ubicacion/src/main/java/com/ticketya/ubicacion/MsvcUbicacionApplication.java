package com.ticketya.ubicacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ticketya.ubicacion.infrastructure.client")
@SpringBootApplication
public class MsvcUbicacionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcUbicacionApplication.class, args);
    }

}
