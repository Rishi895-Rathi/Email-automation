package com.salespipeline.sales_pipeline.stage;

import com.salespipeline.sales_pipeline.model.Company;
import com.salespipeline.sales_pipeline.model.Contact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ApolloStage {

    @Value("${apollo.api.key}")
    private String apiKey;

    @Value("${pipeline.lookalike.limit}")
    private int lookalikeLimit;

    @Autowired
    private RestTemplate restTemplate;

    private HttpHeaders headers()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public List<Company> expand(String seedDomain)
    {
        String seedIndustry = fetchSeedIndustry(seedDomain);

        if (seedIndustry == null)
        {
            System.out.println("Could not resolve seed company industry from Apollo");
            return List.of();
        }

        String url = "https://api.apollo.io/v1/mixed_companies/search";

        Map<String, Object> body = Map.of(
                "q_organization_keyword_tags", List.of(seedIndustry),
                "per_page", lookalikeLimit
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers());

        try
        {
            Map response = restTemplate.postForObject(url, request, Map.class);

            if (response == null)
            {
                System.out.println("Empty response from Apollo organization search");
                return List.of();
            }

            List<Map<String, Object>> organizations = (List<Map<String, Object>>) response.get("organizations");

            if (organizations == null || organizations.isEmpty())
            {
                System.out.println("No lookalike companies found");
                return List.of();
            }

            List<Company> companies = new ArrayList<>();

            for (Map<String, Object> org : organizations)
            {
                String domain = (String) org.get("primary_domain");

                if (domain == null || domain.equalsIgnoreCase(seedDomain))
                {
                    continue;
                }

                String name = (String) org.get("name");
                String industry = org.get("industry") != null ? (String) org.get("industry") : seedIndustry;
                Integer empCount = org.get("estimated_num_employees") != null
                        ? ((Number) org.get("estimated_num_employees")).intValue()
                        : 0;

                companies.add(new Company(domain, name, industry, empCount));
            }

            return companies;
        }
        catch (Exception e)
        {
            System.out.println("Apollo organization search error: " + e.getMessage());
            return List.of();
        }
    }

    private String fetchSeedIndustry(String seedDomain)
    {
        String url = "https://api.apollo.io/v1/organizations/enrich?domain=" + seedDomain;

        HttpEntity<Void> request = new HttpEntity<>(headers());

        try
        {
            Map response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

            if (response == null)
            {
                return null;
            }

            Map<String, Object> organization = (Map<String, Object>) response.get("organization");

            if (organization == null)
            {
                return null;
            }

            return (String) organization.get("industry");
        }
        catch (Exception e)
        {
            System.out.println("Apollo enrich error: " + e.getMessage());
            return null;
        }
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

                String personId = (String) person.get("id");
                String revealedEmail = revealEmail(personId);

                if (revealedEmail != null)
                {
                    contact.setEmail(revealedEmail);
                    System.out.println("Revealed: " + contact.getFirstName() + " -> " + revealedEmail);
                }

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

    private String revealEmail(String personId)
    {
        if (personId == null)
        {
            return null;
        }

        String url = "https://api.apollo.io/v1/people/match";

        Map<String, Object> body = Map.of(
                "id", personId,
                "reveal_personal_emails", true
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers());

        try
        {
            Map response = restTemplate.postForObject(url, request, Map.class);

            if (response == null)
            {
                return null;
            }

            Map<String, Object> person = (Map<String, Object>) response.get("person");

            if (person == null)
            {
                return null;
            }

            Object email = person.get("email");

            if (email != null && !email.toString().contains("email_not_unlocked"))
            {
                return email.toString();
            }
        }
        catch (Exception e)
        {
            System.out.println("Apollo match/reveal error: " + e.getMessage());
        }

        return null;
    }
}