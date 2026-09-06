package com.salespipeline.sales_pipeline;

import com.salespipeline.sales_pipeline.model.Company;
import com.salespipeline.sales_pipeline.model.Contact;
import com.salespipeline.sales_pipeline.model.EmailDraft;
import com.salespipeline.sales_pipeline.stage.ApolloStage;
import com.salespipeline.sales_pipeline.stage.HunterEmailFinderStage;
import com.salespipeline.sales_pipeline.stage.ZeroBounceStage;
import com.salespipeline.sales_pipeline.stage.GmailStage;
import com.salespipeline.sales_pipeline.util.EmailComposer;
import com.salespipeline.sales_pipeline.util.SafetyCheckpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@SpringBootApplication
public class SalesPipelineApplication implements CommandLineRunner {

	@Autowired private ApolloStage apolloStage;
	@Autowired private HunterEmailFinderStage hunterEmailFinderStage;
	@Autowired private ZeroBounceStage zeroBounceStage;
	@Autowired private GmailStage gmailStage;
	@Autowired private EmailComposer emailComposer;
	@Autowired private SafetyCheckpoint safetyCheckpoint;
	@Autowired private com.salespipeline.sales_pipeline.util.HistoryManager historyManager;

	public static void main(String[] args) {
		SpringApplication.run(SalesPipelineApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java -jar pipeline.jar shopify.com,bigcommerce.com,woocommerce.com");
			return;
		}

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(System.in));

		String[] domains = args[0].split(",");
		List<Company> companies = new ArrayList<>();

		for (String domain : domains)
		{
			companies.add(new Company(domain.trim(), domain.trim(), "", 0));
		}

		System.out.println("Starting pipeline for " + companies.size() + " domain(s)");

		// Stage 1
		System.out.println("\n[Stage 1] Fetching decision-makers via Apollo...");
		List<Contact> contacts = apolloStage.getDecisionMakers(companies);
		System.out.println("Found " + contacts.size() + " contacts");

		// Stage 1.5
		System.out.println("\n[Stage 1.5] Finding emails via Hunter.io...");
		List<Contact> contactsWithEmail = hunterEmailFinderStage.findEmails(contacts);
		System.out.println("Emails found: " + contactsWithEmail.size());

		// Stage 2
		System.out.println("\n[Stage 2] Filtering verified emails via ZeroBounce...");
		List<Contact> verifiedContacts = zeroBounceStage.resolveEmails(contactsWithEmail);
		System.out.println("Verified " + verifiedContacts.size() + " emails");

		System.out.println("\n[Stage 2.5] Filtering out previously contacted...");
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
		safetyCheckpoint.confirm(newContacts, draft, reader);

		// Stage 3
		System.out.println("\n[Stage 3] Sending outreach via Gmail...");
		gmailStage.sendOutreach(newContacts, draft);
		System.out.println("\nPipeline complete!");

		reader.close();
	}
}