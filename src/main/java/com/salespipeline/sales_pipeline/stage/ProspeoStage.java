package com.salespipeline.sales_pipeline.stage;

import com.salespipeline.sales_pipeline.model.Company;
import com.salespipeline.sales_pipeline.model.Contact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProspeoStage {

    @Value("${prospeo.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    public List<Contact> getDecisionMakers(List<Company> companies) {
        List<Contact> allContacts = new ArrayList<>();

        for (Company company : companies) {
            try {
                List<Contact> contacts = fetchContactsForCompany(company);
                allContacts.addAll(contacts);
                System.out.println("Found " + contacts.size()
                        + " contacts at: " + company.getDomain());

            } catch (Exception e) {
                System.out.println("Failed to fetch contacts for: "
                        + company.getDomain() + " — " + e.getMessage());
            }
        }

        return allContacts;
    }

    private List<Contact> fetchContactsForCompany(Company company) {
        String url = "https://api.prospeo.io/v1/domain-search";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-KEY", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "domain", company.getDomain(),
                "limit",  10
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map response = restTemplate.postForObject(url, request, Map.class);

        List<Contact> contacts = new ArrayList<>();

        if (response == null || !response.containsKey("response")) {
            return contacts;
        }

        // Parse each person from response
        List<Map<String, Object>> people =
                (List<Map<String, Object>>) response.get("response");

        for (Map<String, Object> person : people) {
            String title = (String) person.get("job_title");

            // Only keep C-suite and VP level
            if (isDecisionMaker(title)) {
                Contact contact = new Contact();
                contact.setFirstName((String) person.get("first_name"));
                contact.setLastName((String) person.get("last_name"));
                contact.setTitle(title);
                contact.setCompanyDomain(company.getDomain());
                contact.setLinkedinUrl((String) person.get("linkedin_url"));
                contacts.add(contact);
            }
        }

        return contacts;
    }

    // Only target decision makers
    private boolean isDecisionMaker(String title) {
        if (title == null) return false;
        String t = title.toLowerCase();
        return t.contains("ceo")
                || t.contains("cto")
                || t.contains("coo")
                || t.contains("cfo")
                || t.contains("vp")
                || t.contains("vice president")
                || t.contains("founder")
                || t.contains("director");
    }
}