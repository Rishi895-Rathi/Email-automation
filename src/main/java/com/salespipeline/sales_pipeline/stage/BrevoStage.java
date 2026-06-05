package com.salespipeline.sales_pipeline.stage;

import com.salespipeline.sales_pipeline.model.Contact;
import com.salespipeline.sales_pipeline.model.EmailDraft;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class BrevoStage {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    // Now accepts EmailDraft — no more hardcoded message
    public void sendOutreach(List<Contact> contacts, EmailDraft draft) {
        String url = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (Contact contact : contacts) {

            // Personalise message for each contact
            String personalBody = draft.getBody()
                    .replace("{firstName}",   contact.getFirstName())
                    .replace("{companyName}", contact.getCompanyDomain())
                    .replace("{title}",       contact.getTitle());

            // Convert line breaks to HTML
            String htmlBody = "<p>" + personalBody.replace("\n", "<br/>") + "</p>";

            Map<String, Object> emailBody = Map.of(
                    "sender",      Map.of(
                            "name",  "Your Name",
                            "email", "you@yourcompany.com"),
                    "to",          List.of(Map.of(
                            "email", contact.getEmail(),
                            "name",  contact.getFirstName())),
                    "subject",     draft.getSubject(),   // ✅ from user input
                    "htmlContent", htmlBody              // ✅ personalised body
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailBody, headers);
            restTemplate.postForEntity(url, request, String.class);

            System.out.println("Sent to: " + contact.getEmail());
        }
    }
}