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
public class ApolloStage {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${apollo.api.key}")
    private String apiKey;

    private HttpHeaders headers()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public List<Contact> getDecisionMakers(List<Company> companies)
    {
        List<Contact> allContacts = new ArrayList<>();

        for (Company company : companies)
        {
            try
            {
                Thread.sleep(1000);

                List<Contact> contacts = fetchContactsForCompany(company);
                allContacts.addAll(contacts);
                System.out.println("Found " + contacts.size() + " contacts at: " + company.getDomain());
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            catch (Exception e)
            {
                System.out.println("Failed to fetch contacts for: " + company.getDomain() + " - " + e.getMessage());
            }
        }

        return allContacts;
    }

    private List<Contact> fetchContactsForCompany(Company company)
    {
        String url = "https://api.apollo.io/v1/mixed_people/search";

        Map<String, Object> body = Map.of(
                "person_titles", List.of("Founder", "Owner", "CEO", "CTO", "VP", "Director"),
                "q_organization_domains", company.getDomain(),
                "per_page", 5
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers());

        try
        {
            Map response = restTemplate.postForObject(url, request, Map.class);
            List<Contact> contacts = new ArrayList<>();

            if (response == null)
            {
                return contacts;
            }

            List<Map<String, Object>> people = (List<Map<String, Object>>) response.get("people");

            if (people == null || people.isEmpty())
            {
                return contacts;
            }

            for (Map<String, Object> person : people)
            {
                Contact contact = new Contact();
                contact.setFirstName((String) person.get("first_name"));
                contact.setLastName((String) person.get("last_name"));
                contact.setTitle(person.get("title") != null ? (String) person.get("title") : "");
                contact.setCompanyDomain(company.getDomain());
                contact.setLinkedinUrl((String) person.get("linkedin_url"));
                contact.setEmail(null);

                contacts.add(contact);
            }

            return contacts;
        }
        catch (Exception e)
        {
            System.out.println("Apollo people search error: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}