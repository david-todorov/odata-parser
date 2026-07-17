package io.github.davidtodorov.odataparser.demo;

import java.math.BigDecimal;

public class CaseModel {

    private Integer id;
    private String reference;
    private String title;
    private BigDecimal amount;
    private Integer priority;
    private Boolean deleted;
    private Boolean closed;

    private UserModel owner;
    private UserModel reviewer;
    private DepartmentModel department;
}