package com.salespipeline.sales_pipeline;

import com.salespipeline.sales_pipeline.model.Company;
import com.salespipeline.sales_pipeline.model.Contact;
import com.salespipeline.sales_pipeline.model.EmailDraft;
import com.salespipeline.sales_pipeline.stage.OceanStage;
import com.salespipeline.sales_pipeline.stage.ProspeoStage;
import com.salespipeline.sales_pipeline.stage.ZeroBounceStage;
import com.salespipeline.sales_pipeline.stage.BrevoStage;
import com.salespipeline.sales_pipeline.util.EmailComposer;
import com.salespipeline.sales_pipeline.util.SafetyCheckpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@SpringBootApplication
public class SalesPipelineApplication implements CommandLineRunner {

	@Autowired private OceanStage oceanStage;
	@Autowired private ProspeoStage prospeoStage;
	@Autowired private ZeroBounceStage eazyreachStage;
	@Autowired private BrevoStage brevoStage;
	@Autowired private EmailComposer emailComposer;
	@Autowired private SafetyCheckpoint safetyCheckpoint;
	@Autowired private com.salespipeline.sales_pipeline.util.HistoryManager historyManager;

	public static void main(String[] args) {
		SpringApplication.run(SalesPipelineApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java -jar pipeline.jar shopify.com");
			return;
		}

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(System.in));

		String seedDomain = args[0];
		System.out.println("Starting pipeline for seed domain: " + seedDomain);

		// Stage 1
		System.out.println("\n[Stage 1] Expanding via Ocean.io...");
		List<Company> companies = oceanStage.expand(seedDomain);
		System.out.println("Found " + companies.size() + " lookalike companies");

		// Stage 2
		System.out.println("\n[Stage 2] Fetching decision-makers via Prospeo...");
		List<Contact> contacts = prospeoStage.getDecisionMakers(companies);
		System.out.println("Found " + contacts.size() + " contacts");

		// Stage 3
		System.out.println("\n[Stage 3] Filtering verified emails...");
		List<Contact> verifiedContacts = eazyreachStage.resolveEmails(contacts);
		System.out.println("Verified " + verifiedContacts.size() + " emails"); // ✅

		System.out.println("\n[Stage 3.5] Filtering out previously contacted...");
		List<Contact> newContacts = historyManager.filterOutAlreadyContacted(verifiedContacts);
		System.out.println("New contacts to email: " + newContacts.size());

		if (newContacts.isEmpty()) {
			System.out.println("No new contacts to email. Pipeline complete!");
			reader.close();
			return;
		}

		// Email Composer
		EmailDraft draft = emailComposer.getEmailDraft(reader);

		// Safety Checkpoint
		safetyCheckpoint.confirm(newContacts, draft, reader);              // ✅

		// Stage 4
		System.out.println("\n[Stage 4] Sending outreach via Brevo...");
		brevoStage.sendOutreach(newContacts, draft);                       // ✅
		System.out.println("\nPipeline complete!");

		reader.close();
	}
}