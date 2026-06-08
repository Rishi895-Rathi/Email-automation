package com.salespipeline.sales_pipeline.stage;

import com.salespipeline.sales_pipeline.model.Contact;
import com.salespipeline.sales_pipeline.model.EmailDraft;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class BrevoStage {

    @Value("${brevo.api.key}")
    private String apiKey;

    // Sender details from application.properties
    @Value("${brevo.sender.name}")
    private String senderName;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Autowired
    private RestTemplate restTemplate;

    public void sendOutreach(List<Contact> contacts, EmailDraft draft) {
        String url = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        int sentCount    = 0;
        int failedCount  = 0;

        for (Contact contact : contacts) {
            try {
                // Personalise message for each contact
                String personalBody = draft.getBody()
                        .replace("{firstName}",   contact.getFirstName())
                        .replace("{companyName}", contact.getCompanyDomain())
                        .replace("{title}",       contact.getTitle());

                // Convert line breaks to HTML
                String htmlBody = "<p>" + personalBody
                        .replace("\n", "<br/>") + "</p>";

                Map<String, Object> emailBody = Map.of(
                        "sender",      Map.of(
                                "name",  senderName,    // ✅ from properties
                                "email", senderEmail),  // ✅ from properties
                        "to",          List.of(Map.of(
                                "email", contact.getEmail(),
                                "name",  contact.getFirstName())),
                        "subject",     draft.getSubject(),
                        "htmlContent", htmlBody
                );

                HttpEntity<Map<String, Object>> request =
                        new HttpEntity<>(emailBody, headers);
                restTemplate.postForEntity(url, request, String.class);

                System.out.println("Sent to: " + contact.getEmail());
                sentCount++;

            } catch (HttpClientErrorException e) {
                // Handle specific Brevo errors
                if (e.getStatusCode().value() == 401) {
                    System.out.println("Invalid Brevo API key — stopping");
                    break;
                } else if (e.getStatusCode().value() == 429) {
                    System.out.println("Rate limit hit — stopping");
                    break;
                } else {
                    System.out.println("Failed to send to: "
                            + contact.getEmail()
                            + " — " + e.getMessage());
                    failedCount++;
                }
            } catch (Exception e) {
                System.out.println("Unexpected error for: "
                        + contact.getEmail()
                        + " — " + e.getMessage());
                failedCount++;
            }
        }

        // Final summary
        System.out.println("\n========== SEND SUMMARY ==========");
        System.out.println("Successfully sent : " + sentCount);
        System.out.println("Failed            : " + failedCount);
        System.out.println("==================================");
    }
}