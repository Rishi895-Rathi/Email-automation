package com.salespipeline.sales_pipeline.util;

import com.salespipeline.sales_pipeline.model.Contact;
import com.salespipeline.sales_pipeline.model.EmailDraft;
import org.springframework.stereotype.Component;
import java.util.List;
import java.io.IOException;
import java.io.BufferedReader;

@Component
public class SafetyCheckpoint {

    public void confirm(List<Contact> contacts, EmailDraft draft,
                        BufferedReader reader) throws IOException {

        System.out.println("\n========== EMAIL PREVIEW ==========");
        System.out.println("Subject : " + draft.getSubject());
        System.out.println("Message :");
        System.out.println(draft.getBody());
        System.out.println("====================================");

        System.out.println("\n========== RECIPIENTS ==========");
        contacts.forEach(c ->
                System.out.printf("  %-25s %-30s %s%n",
                        c.getFirstName() + " " + c.getLastName(),
                        c.getTitle(),
                        c.getEmail())
        );
        System.out.println("=================================");
        System.out.println("About to send " + contacts.size()
                + " emails. Proceed? (y/n): ");

        String input = reader.readLine();

        if (input == null || !input.trim().equalsIgnoreCase("y")) {
            System.out.println("Aborted. No emails sent.");
            System.exit(0);
        }
    }
}