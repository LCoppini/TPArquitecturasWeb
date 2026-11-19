package com.ticketya.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ticketya.notificaciones.client")
@SpringBootApplication
public class MsvcNotificacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcNotificacionesApplication.class, args);
    }

}
