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
public class ZeroBounceStage {

    @Value("${zerobounce.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    public List<Contact> resolveEmails(List<Contact> contacts)
    {
        List<Contact> resolvedContacts = new ArrayList<>();

        for (Contact contact : contacts)
        {
            if (contact.getEmail() == null || contact.getEmail().isEmpty())
            {
                continue;
            }

            if (isValid(contact.getEmail()))
            {
                resolvedContacts.add(contact);
            }
        }

        System.out.println("\nEmails resolved: " + resolvedContacts.size() + "/" + contacts.size());

        return resolvedContacts;
    }

    private boolean isValid(String email)
    {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.zerobounce.net/v2/validate")
                .queryParam("api_key", apiKey)
                .queryParam("email", email)
                .toUriString();

        try
        {
            Map response = restTemplate.getForObject(url, Map.class);

            if (response == null)
            {
                return false;
            }

            String status = (String) response.get("status");
            return "valid".equalsIgnoreCase(status) || "catch-all".equalsIgnoreCase(status);
        }
        catch (Exception e)
        {
            System.out.println("ZeroBounce error for " + email + ": " + e.getMessage());
            return false;
        }
    }
}