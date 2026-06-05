package com.salespipeline.sales_pipeline;

import com.salespipeline.sales_pipeline.model.Company;
import com.salespipeline.sales_pipeline.model.Contact;
import com.salespipeline.sales_pipeline.model.EmailDraft;
import com.salespipeline.sales_pipeline.stage.OceanStage;
import com.salespipeline.sales_pipeline.stage.ProspeoStage;
import com.salespipeline.sales_pipeline.stage.EazyreachStage;
import com.salespipeline.sales_pipeline.stage.BrevoStage;
import com.salespipeline.sales_pipeline.util.EmailComposer;
import com.salespipeline.sales_pipeline.util.SafetyCheckpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class SalesPipelineApplication implements CommandLineRunner {

	@Autowired private OceanStage oceanStage;
	@Autowired private ProspeoStage prospeoStage;
	@Autowired private EazyreachStage eazyreachStage;
	@Autowired private BrevoStage brevoStage;
	@Autowired private EmailComposer emailComposer;        // ← added
	@Autowired private SafetyCheckpoint safetyCheckpoint;

	public static void main(String[] args) {
		SpringApplication.run(SalesPipelineApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: ./gradlew bootRun --args='stripe.com'");
			return;
		}

		Scanner scanner = new Scanner(System.in);          // ← added
		String seedDomain = args[0];
		System.out.println("Starting pipeline for seed domain: " + seedDomain);

		// Stage 1 — Lookalike companies
		System.out.println("\n[Stage 1] Expanding via Ocean.io...");
		List<Company> companies = oceanStage.expand(seedDomain);
		System.out.println("Found " + companies.size() + " lookalike companies");

		// Stage 2 — Decision makers
		System.out.println("\n[Stage 2] Fetching decision-makers via Prospeo...");
		List<Contact> contacts = prospeoStage.getDecisionMakers(companies);
		System.out.println("Found " + contacts.size() + " contacts");

		// Stage 3 — Resolve emails
		System.out.println("\n[Stage 3] Resolving emails via Eazyreach...");
		List<Contact> contactsWithEmails = eazyreachStage.resolveEmails(contacts);
		System.out.println("Verified " + contactsWithEmails.size() + " emails");

		// User writes/edits email message
		EmailDraft draft = emailComposer.getEmailDraft(scanner); // ← added

		// Safety Checkpoint
		safetyCheckpoint.confirm(contactsWithEmails, draft);     // ← added draft

		// Stage 4 — Send emails
		System.out.println("\n[Stage 4] Sending outreach via Brevo...");
		brevoStage.sendOutreach(contactsWithEmails, draft);      // ← added draft
		System.out.println("\nPipeline complete!");
	}
}