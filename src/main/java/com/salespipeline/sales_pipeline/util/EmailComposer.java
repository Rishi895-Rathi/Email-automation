package com.salespipeline.sales_pipeline.util;

import com.salespipeline.sales_pipeline.model.EmailDraft;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;

@Component
public class EmailComposer {

    private static final String TEMPLATE_FILE = "email-template.txt";

    public EmailDraft getEmailDraft(Scanner scanner) throws IOException {
        Path path = Path.of(TEMPLATE_FILE);

        // Create default template if none exists
        if (!Files.exists(path)) {
            createDefaultTemplate(path);
        }

        System.out.println("\n========== EMAIL COMPOSER ==========");
        System.out.println("1. Use existing template (" + TEMPLATE_FILE + ")");
        System.out.println("2. Write new message now");
        System.out.println("3. Edit subject line only");
        System.out.print("Choose (1/2/3): ");

        String choice = scanner.nextLine().trim();

        return switch (choice) {
            case "2" -> writeNewMessage(scanner, path);
            case "3" -> editSubjectOnly(scanner, path);
            default  -> loadFromFile(path);   // option 1 or anything else
        };
    }

    // Option 1 — load saved template
    private EmailDraft loadFromFile(Path path) throws IOException {
        String content = Files.readString(path);
        String subject = extractSubject(content);
        String body    = extractBody(content);
        System.out.println("Template loaded from " + TEMPLATE_FILE);
        return new EmailDraft(subject, body);
    }

    // Option 2 — user types new message
    private EmailDraft writeNewMessage(Scanner scanner, Path path) throws IOException {
        System.out.print("\nSubject: ");
        String subject = scanner.nextLine().trim();

        System.out.println("Body (type END on a new line when done):");
        StringBuilder body = new StringBuilder();
        String line;
        while (!(line = scanner.nextLine()).equals("END")) {
            body.append(line).append("\n");
        }

        // Save for next run
        String full = "SUBJECT: " + subject + "\n\n" + body;
        Files.writeString(path, full);
        System.out.println("Saved to " + TEMPLATE_FILE + " for future runs");

        return new EmailDraft(subject, body.toString());
    }

    // Option 3 — only change subject, keep existing body
    private EmailDraft editSubjectOnly(Scanner scanner, Path path) throws IOException {
        String content = Files.readString(path);
        System.out.print("\nNew Subject: ");
        String newSubject = scanner.nextLine().trim();
        String body = extractBody(content);
        System.out.println("Subject updated, body kept from template");
        return new EmailDraft(newSubject, body);
    }

    // Pull subject line out of template file
    private String extractSubject(String content) {
        return Arrays.stream(content.split("\n"))
                .filter(l -> l.startsWith("SUBJECT:"))
                .map(l -> l.replace("SUBJECT:", "").trim())
                .findFirst()
                .orElse("Quick intro from us");
    }

    // Pull body out of template file (everything after first blank line)
    private String extractBody(String content) {
        int idx = content.indexOf("\n\n");
        return idx != -1 ? content.substring(idx).trim() : content;
    }

    // Creates a default template file on first run
    private void createDefaultTemplate(Path path) throws IOException {
        String template = """
                SUBJECT: Quick intro — thought this might be relevant

                Hi {firstName},

                I came across {companyName} and wanted to reach out.

                We help teams like yours with [your value prop here].

                Worth a quick 15-min call this week?

                Best,
                [Your Name]
                """;
        Files.writeString(path, template);
        System.out.println("No template found — created " + TEMPLATE_FILE);
        System.out.println("   Edit it or write a new message when prompted.");
    }
}