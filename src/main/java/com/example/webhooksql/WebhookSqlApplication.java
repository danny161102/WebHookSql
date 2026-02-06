package com.example.webhooksql;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class WebhookSqlApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(WebhookSqlApplication.class, args);
    }

    @Override
    public void run(String... args) {

        RestTemplate restTemplate = new RestTemplate();

        String generateWebhookUrl =
                "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "John Doe");
        requestBody.put("regNo", "REG12347"); // odd -> Question 1
        requestBody.put("email", "john@example.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> requestEntity =
                new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                generateWebhookUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

        String webhookUrl = (String) response.getBody().get("webhook");
        String accessToken = (String) response.getBody().get("accessToken");

        String finalSqlQuery =
                "SELECT d.DEPARTMENT_NAME, " +
                "SUM(p.AMOUNT) AS SALARY, " +
                "CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS EMPLOYEE_NAME, " +
                "TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE " +
                "FROM EMPLOYEE e " +
                "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                "JOIN PAYMENTS p ON e.EMP_ID = p.EMP_ID " +
                "WHERE DAY(p.PAYMENT_TIME) <> 1 " +
                "GROUP BY d.DEPARTMENT_NAME, e.EMP_ID " +
                "HAVING SUM(p.AMOUNT) = ( " +
                "SELECT MAX(total_salary) FROM ( " +
                "SELECT SUM(p2.AMOUNT) AS total_salary " +
                "FROM EMPLOYEE e2 " +
                "JOIN PAYMENTS p2 ON e2.EMP_ID = p2.EMP_ID " +
                "WHERE e2.DEPARTMENT = e.DEPARTMENT " +
                "AND DAY(p2.PAYMENT_TIME) <> 1 " +
                "GROUP BY e2.EMP_ID " +
                ") t )";

        HttpHeaders submitHeaders = new HttpHeaders();
        submitHeaders.setContentType(MediaType.APPLICATION_JSON);
        submitHeaders.set("Authorization", accessToken);

        Map<String, String> submitBody = new HashMap<>();
        submitBody.put("finalQuery", finalSqlQuery);

        HttpEntity<Map<String, String>> submitEntity =
                new HttpEntity<>(submitBody, submitHeaders);

        restTemplate.exchange(
                webhookUrl,
                HttpMethod.POST,
                submitEntity,
                String.class
        );
    }
}
