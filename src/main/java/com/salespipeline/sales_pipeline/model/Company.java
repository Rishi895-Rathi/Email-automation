package com.salespipeline.sales_pipeline.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Company {
    private String domain;
    private String name;
    private String industry;
    private int employeeCount;
}

