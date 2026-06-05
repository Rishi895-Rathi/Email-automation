package com.salespipeline.sales_pipeline.util;

import com.salespipeline.sales_pipeline.model.Contact;
import com.salespipeline.sales_pipeline.model.EmailDraft;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Scanner;

@Component
public class SafetyCheckpoint {

    //Now accepts EmailDraft to show preview
    public void confirm(List<Contact> contacts, EmailDraft draft) {

        // Show email draft preview
        System.out.println("\n========== EMAIL PREVIEW ==========");
        System.out.println("Subject : " + draft.getSubject());
        System.out.println("Message :");
        System.out.println(draft.getBody());
        System.out.println("====================================");

        // Show contact list
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

        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine().trim();

        if (!input.equalsIgnoreCase("y")) {
            System.out.println("Aborted. No emails sent.");
            System.exit(0);
        }
    }
}