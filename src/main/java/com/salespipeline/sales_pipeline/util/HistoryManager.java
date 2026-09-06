package com.salespipeline.sales_pipeline.util;

import com.salespipeline.sales_pipeline.model.Contact;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class HistoryManager {

    private static final String HISTORY_FILE = "history.txt";

    public List<Contact> filterOutAlreadyContacted(List<Contact> contacts) {
        Set<String> history = loadHistory();
        return contacts.stream()
                .filter(c -> c.getEmail() != null && !history.contains(c.getEmail().toLowerCase()))
                .collect(Collectors.toList());
    }

    public void recordContacted(String email) {
        if (email == null) {
            return;
        }

        try {
            Path path = Paths.get(HISTORY_FILE);
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.writeString(path, email.toLowerCase() + System.lineSeparator(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Warning: Failed to save to history: " + e.getMessage());
        }
    }

    private Set<String> loadHistory() {
        try {
            Path path = Paths.get(HISTORY_FILE);
            if (Files.exists(path)) {
                return Files.readAllLines(path).stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toCollection(HashSet::new));
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to read history: " + e.getMessage());
        }
        return new HashSet<>();
    }
}