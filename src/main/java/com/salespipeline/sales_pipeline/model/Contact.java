package com.salespipeline.sales_pipeline.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Contact {
    private String firstName;
    private String lastName;
    private String title;
    private String companyDomain;
    private String linkedinUrl;
    private String email;
}
