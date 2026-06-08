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

    @Value("${pipeline.lookalike.limit}")
    private int lookalikeLimit;

    @Autowired
    private RestTemplate restTemplate;

    public List<Company> expand(String seedDomain) {
        String url = "https://api.ocean.io/v3/search/companies";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Token", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "companiesFilters", Map.of(
                        "lookalikeDomains", List.of(seedDomain)
                ),
                "size", lookalikeLimit
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, request, Map.class);

            if (response.getBody() == null) {
                System.out.println("Empty response from Ocean.io");
                return List.of();
            }

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.getBody().get("companies");

            if (results == null || results.isEmpty()) {
                System.out.println("No companies found in response");
                return List.of();
            }

            return results.stream()
                    .map(r -> {
                        Map<String, Object> company =
                                (Map<String, Object>) r.get("company");

                        if (company == null) return null;

                        String domain = (String) company.get("domain");
                        String name   = (String) company.get("name");

                        List<String> industries =
                                (List<String>) company.get("industries");
                        String industry = (industries != null && !industries.isEmpty())
                                ? industries.get(0) : "";

                        Integer empCount = company.get("employeeCountLinkedin") != null
                                ? ((Number) company.get("employeeCountLinkedin")).intValue()
                                : 0;

                        return new Company(domain, name, industry, empCount);
                    })
                    .filter(c -> c != null && c.getDomain() != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.out.println("Ocean.io API error: " + e.getMessage());
            return List.of();
        }
    }
}