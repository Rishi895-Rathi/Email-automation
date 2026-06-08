package com.salespipeline.sales_pipeline.stage;

import com.salespipeline.sales_pipeline.model.Contact;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EazyreachStage {

    public List<Contact> resolveEmails(List<Contact> contacts) {

        // Skip Eazyreach — no credits available
        // Return only contacts that already have emails
        List<Contact> resolvedContacts = contacts.stream()
                .filter(c -> c.getEmail() != null
                        && !c.getEmail().isEmpty())
                .collect(Collectors.toList());

        System.out.println("\nEmails resolved: "
                + resolvedContacts.size() + "/" + contacts.size());

        return resolvedContacts;
    }
}