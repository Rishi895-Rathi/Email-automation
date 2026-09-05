package com.salespipeline.sales_pipeline.stage;

import com.salespipeline.sales_pipeline.model.Contact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HunterEmailFinderStage {

    @Value("${hunter.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    public List<Contact> findEmails(List<Contact> contacts)
    {
        List<Contact> contactsWithEmail = new ArrayList<>();

        for (Contact contact : contacts)
        {
            try
            {
                Thread.sleep(1000);

                String email = findEmail(contact);

                if (email != null)
                {
                    contact.setEmail(email);
                    contactsWithEmail.add(contact);
                    System.out.println("Found email: " + contact.getFirstName() + " -> " + email);
                }
                else
                {
                    System.out.println("No email found for: " + contact.getFirstName() + " at " + contact.getCompanyDomain());
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            catch (Exception e)
            {
                System.out.println("Hunter lookup failed for: " + contact.getFirstName() + " - " + e.getMessage());
            }
        }

        return contactsWithEmail;
    }

    private String findEmail(Contact contact)
    {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.hunter.io/v2/email-finder")
                .queryParam("domain", contact.getCompanyDomain())
                .queryParam("first_name", contact.getFirstName())
                .queryParam("last_name", contact.getLastName())
                .queryParam("api_key", apiKey)
                .toUriString();

        try
        {
            Map response = restTemplate.getForObject(url, Map.class);

            if (response == null)
            {
                return null;
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");

            if (data == null)
            {
                return null;
            }

            String email = (String) data.get("email");
            Integer score = data.get("score") != null ? ((Number) data.get("score")).intValue() : 0;

            if (email != null && score >= 50)
            {
                return email;
            }
        }
        catch (Exception e)
        {
            System.out.println("Hunter API error for " + contact.getFirstName() + ": " + e.getMessage());
        }

        return null;
    }
}