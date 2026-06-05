package com.salespipeline.sales_pipeline.stage;

import com.salespipeline.sales_pipeline.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OceanStage {

    @Value("${ocean.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    public List<Company> expand(String seedDomain) {
        String url = "https://api.ocean.io/v1/lookalikes";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "domain", seedDomain,
                "limit", 50
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        // Parse response into Company list
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) response.getBody().get("companies");

        return results.stream()
                .map(r -> new Company(
                        (String) r.get("domain"),
                        (String) r.get("name"),
                        (String) r.get("industry"),
                        (Integer) r.get("employee_count")
                ))
                .collect(Collectors.toList());
    }
}