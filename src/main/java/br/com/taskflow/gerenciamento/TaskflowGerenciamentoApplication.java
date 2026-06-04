package br.com.taskflow.gerenciamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@EnableCaching
public class TaskflowGerenciamentoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskflowGerenciamentoApplication.class, args);
	}
}