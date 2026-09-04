package com.salespipeline.sales_pipeline.stage;

import com.salespipeline.sales_pipeline.model.Contact;
import com.salespipeline.sales_pipeline.model.EmailDraft;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GmailStage {

    @Value("${gmail.client.id}")
    private String clientId;

    @Value("${gmail.client.secret}")
    private String clientSecret;

    @Value("${gmail.refresh.token}")
    private String refreshToken;

    @Value("${gmail.sender.email}")
    private String senderEmail;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private com.salespipeline.sales_pipeline.util.HistoryManager historyManager;

    public void sendOutreach(List<Contact> contacts, EmailDraft draft)
    {
        String accessToken = fetchAccessToken();

        if (accessToken == null)
        {
            System.out.println("Could not obtain Gmail access token — aborting send");
            return;
        }

        int sentCount = 0;
        int failedCount = 0;

        for (Contact contact : contacts)
        {
            try
            {
                String personalBody = draft.getBody()
                        .replace("{firstName}", contact.getFirstName())
                        .replace("{companyName}", contact.getCompanyDomain())
                        .replace("{title}", contact.getTitle());

                String rawMessage = buildRawMessage(contact.getEmail(), draft.getSubject(), personalBody);

                sendViaGmailApi(accessToken, rawMessage);

                System.out.println("Sent to: " + contact.getEmail());
                historyManager.recordContacted(contact.getEmail());
                sentCount++;
            }
            catch (HttpClientErrorException e)
            {
                if (e.getStatusCode().value() == 401)
                {
                    System.out.println("Gmail access token invalid/expired — stopping");
                    break;
                }
                else if (e.getStatusCode().value() == 429)
                {
                    System.out.println("Gmail rate limit hit — stopping");
                    break;
                }
                else
                {
                    System.out.println("Failed to send to: " + contact.getEmail() + " — " + e.getMessage());
                    failedCount++;
                }
            }
            catch (Exception e)
            {
                System.out.println("Unexpected error for: " + contact.getEmail() + " — " + e.getMessage());
                failedCount++;
            }
        }

        System.out.println("\n========== SEND SUMMARY ==========");
        System.out.println("Successfully sent : " + sentCount);
        System.out.println("Failed            : " + failedCount);
        System.out.println("==================================");
    }

    private String fetchAccessToken()
    {
        String url = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try
        {
            Map response = restTemplate.postForObject(url, request, Map.class);
            return response != null ? (String) response.get("access_token") : null;
        }
        catch (Exception e)
        {
            System.out.println("Gmail token refresh error: " + e.getMessage());
            return null;
        }
    }

    private String buildRawMessage(String toEmail, String subject, String body)
    {
        String htmlBody = "<p>" + body.replace("\n", "<br/>") + "</p>";

        String message = "From: " + senderEmail + "\r\n"
                + "To: " + toEmail + "\r\n"
                + "Subject: " + subject + "\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n\r\n"
                + htmlBody;

        return Base64.getUrlEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
    }

    private void sendViaGmailApi(String accessToken, String rawMessage)
    {
        String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("raw", rawMessage);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);
    }
}