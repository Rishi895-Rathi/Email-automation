package com.salespipeline.sales_pipeline.util;

import com.salespipeline.sales_pipeline.model.EmailDraft;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.io.BufferedReader;

@Component
public class EmailComposer {

    @Value("${pipeline.email.subject}")
    private String defaultSubject;

    private static final String TEMPLATE_FILE = "email-template.txt";

    public EmailDraft getEmailDraft(BufferedReader reader) throws IOException {
        Path path = Path.of(TEMPLATE_FILE);

        if (!Files.exists(path)) {
            createDefaultTemplate(path);
        }

        System.out.println("\n========== EMAIL COMPOSER ==========");
        System.out.println("1. Use existing template (" + TEMPLATE_FILE + ")");
        System.out.println("2. Write new message now");
        System.out.println("3. Edit subject line only");
        System.out.print("Choose (1/2/3): ");

        String choice = reader.readLine();

        if (choice == null) {
            System.out.println("No input detected - loading template automatically");
            return loadFromFile(path);
        }

        choice = choice.trim();
        return switch (choice) {
            case "2" -> writeNewMessage(reader, path);
            case "3" -> editSubjectOnly(reader, path);
            default  -> loadFromFile(path);
        };
    }

    private EmailDraft loadFromFile(Path path) throws IOException {
        String content = Files.readString(path);
        String subject = extractSubject(content);
        String body    = extractBody(content);
        System.out.println("Template loaded from " + TEMPLATE_FILE);
        return new EmailDraft(subject, body);
    }

    private EmailDraft writeNewMessage(BufferedReader reader, Path path)
            throws IOException {
        System.out.print("\nSubject: ");
        String subjectLine = reader.readLine();

        if (subjectLine == null) {
            System.out.println("No input detected - loading template automatically");
            return loadFromFile(path);
        }

        String subject = subjectLine.trim();

        System.out.println("Body (type END on a new line when done):");
        StringBuilder body = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null && !line.equals("END")) {
            body.append(line).append("\n");
        }

        String full = "SUBJECT: " + subject + "\n\n" + body;
        Files.writeString(path, full);
        System.out.println("Saved to " + TEMPLATE_FILE + " for future runs");

        return new EmailDraft(subject, body.toString());
    }

    private EmailDraft editSubjectOnly(BufferedReader reader, Path path)
            throws IOException {
        String content = Files.readString(path);
        System.out.print("\nNew Subject: ");
        String newSubjectLine = reader.readLine();

        if (newSubjectLine == null) {
            System.out.println("No input detected - keeping existing subject");
            return loadFromFile(path);
        }

        String newSubject = newSubjectLine.trim();
        String body = extractBody(content);
        System.out.println("Subject updated, body kept from template");
        return new EmailDraft(newSubject, body);
    }

    private String extractSubject(String content) {
        return Arrays.stream(content.split("\n"))
                .filter(l -> l.startsWith("SUBJECT:"))
                .map(l -> l.replace("SUBJECT:", "").trim())
                .findFirst()
                .orElse(defaultSubject);
    }

    private String extractBody(String content) {
        int idx = content.indexOf("\n\n");
        return idx != -1 ? content.substring(idx).trim() : content;
    }

    private void createDefaultTemplate(Path path) throws IOException {
        String template = """
                SUBJECT: """ + defaultSubject + """


                Hi {firstName},

                I hope this message finds you well.

                My name is [Your Name], and I am reaching out from [Your Company].
                I came across {companyName} and was genuinely impressed by the work
                your team is doing in the industry.

                We specialize in [your value proposition], and I believe there could
                be some interesting areas where our work aligns with what you are
                building at {companyName}.

                Would you be open to a short 15-minute call at your convenience
                to explore if there is a potential fit?

                I look forward to hearing from you.

                Warm regards,
                [Your Name]
                [Your Title]
                [Your Company]

                ---
                This outreach was initiated through an automated prospecting pipeline.
                If you would prefer not to receive further emails, please reply with
                Unsubscribe and you will be removed from our list immediately.
                """;
        Files.writeString(path, template);
        System.out.println("No template found - created " + TEMPLATE_FILE);
        System.out.println("Edit it or write a new message when prompted.");
    }
}