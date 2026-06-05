package com.salespipeline.sales_pipeline.stage;

package com.salespipeline.sales_pipeline.stage;

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
public class EazyreachStage {

    @Value("${eazyreach.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    public List<Contact> resolveEmails(List<Contact> contacts) {
        List<Contact> resolvedContacts = new ArrayList<>();

        for (Contact contact : contacts) {
            try {
                String email = getEmailFromLinkedIn(contact.getLinkedinUrl());

                if (email != null && !email.isEmpty()) {
                    contact.setEmail(email);
                    resolvedContacts.add(contact);
                    System.out.println("Resolved email for: " + contact.getFirstName()
                            + " " + contact.getLastName() + " → " + email);
                } else {
                    System.out.println("No email found for: " + contact.getFirstName()
                            + " " + contact.getLastName() + " — skipping");
                }

            } catch (Exception e) {
                System.out.println("Failed to resolve email for: "
                        + contact.getFirstName() + " — " + e.getMessage());
            }
        }

        return resolvedContacts;
    }

    private String getEmailFromLinkedIn(String linkedinUrl) {
        String url = "https://api.eazyreach.com/v1/resolve";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "linkedin_url", linkedinUrl
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            // Call Eazyreach API
            Map response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("email")) {
                return (String) response.get("email");
            }

        } catch (Exception e) {
            System.out.println("Eazyreach API error: " + e.getMessage());
        }

        return null;
    }
}