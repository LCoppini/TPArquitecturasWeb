package com.ticketya.evento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ticketya.evento.client")
@SpringBootApplication
public class MsvcEventoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcEventoApplication.class, args);
    }

}
