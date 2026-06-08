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
                Thread.sleep(1000);

                List<Contact> contacts = fetchContactsForCompany(company);
                allContacts.addAll(contacts);
                System.out.println("Found " + contacts.size()
                        + " contacts at: " + company.getDomain());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("Failed to fetch contacts for: "
                        + company.getDomain() + " - " + e.getMessage());
            }
        }

        return allContacts;
    }

    private List<Contact> fetchContactsForCompany(Company company) {
        String url = "https://api.prospeo.io/search-person";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-KEY", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> filters = Map.of(
                "person_seniority", Map.of(
                        "include", List.of(
                                "Founder/Owner",
                                "C-Suite",
                                "Vice President",
                                "Director"
                        )
                ),
                "company", Map.of(
                        "websites", Map.of(
                                "include", List.of(company.getDomain())
                        )
                ),
                "max_person_per_company", 5
        );

        Map<String, Object> body = Map.of(
                "filters", filters,
                "page", 1
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map response = restTemplate.postForObject(url, request, Map.class);
            List<Contact> contacts = new ArrayList<>();

            if (response == null) return contacts;

            if (Boolean.TRUE.equals(response.get("error"))) {
                System.out.println("Prospeo error for: " + company.getDomain()
                        + " - " + response.get("error_code"));
                return contacts;
            }

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.get("results");

            if (results == null || results.isEmpty()) return contacts;

            for (Map<String, Object> result : results) {
                Map<String, Object> person =
                        (Map<String, Object>) result.get("person");

                if (person == null) continue;

                String linkedinUrl = (String) person.get("linkedin_url");

                Contact contact = new Contact();
                contact.setFirstName((String) person.get("first_name"));
                contact.setLastName((String) person.get("last_name"));
                contact.setTitle(person.get("current_job_title") != null ?
                        (String) person.get("current_job_title") : "");
                contact.setCompanyDomain(company.getDomain());
                contact.setLinkedinUrl(linkedinUrl);

                // Get email from Prospeo response
                Map<String, Object> emailObj =
                        (Map<String, Object>) person.get("email");

                if (emailObj != null
                        && emailObj.get("email") != null
                        && !emailObj.get("email").toString().contains("*")) {
                    //Full email available directly
                    contact.setEmail((String) emailObj.get("email"));
                    System.out.println("Email from Prospeo: "
                            + contact.getFirstName()
                            + " -> " + contact.getEmail());

                } else if (linkedinUrl != null && !linkedinUrl.isEmpty()) {
                    //Email masked — reveal via LinkedIn finder
                    System.out.println("Revealing email for: "
                            + contact.getFirstName() + "...");
                    String revealedEmail = revealEmailFromProspeo(linkedinUrl);
                    if (revealedEmail != null) {
                        contact.setEmail(revealedEmail);
                        System.out.println("Revealed: "
                                + contact.getFirstName()
                                + " -> " + revealedEmail);
                    }
                }

                contacts.add(contact);
            }

            return contacts;

        } catch (Exception e) {
            System.out.println("Prospeo API error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String revealEmailFromProspeo(String linkedinUrl) {
        String url = "https://api.prospeo.io/enrich-person";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-KEY", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "only_verified_email", true,
                "data", Map.of(
                        "linkedin_url", linkedinUrl
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map response = restTemplate.postForObject(url, request, Map.class);

            if (response == null) return null;

            if (Boolean.TRUE.equals(response.get("error"))) {
                System.out.println("Reveal error: "
                        + response.get("error_code"));
                return null;
            }

            Map<String, Object> person =
                    (Map<String, Object>) response.get("person");

            if (person == null) return null;

            Map<String, Object> emailObj =
                    (Map<String, Object>) person.get("email");

            if (emailObj != null
                    && emailObj.get("email") != null
                    && !emailObj.get("email").toString().contains("*")) {
                return (String) emailObj.get("email");
            }

        } catch (Exception e) {
            System.out.println("Prospeo enrich error: " + e.getMessage());
        }

        return null;
    }
}