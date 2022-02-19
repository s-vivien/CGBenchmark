package fr.svivien.cgbenchmark.model;

import lombok.Data;

/**
 * Configured CG account
 */
@Data
public class CGAccount {

    private String accountName;

    private String accountCookie;

    private String accountIde;

    private Integer userId;

    public CGAccount(String rememberMe) {
        this.accountCookie = "rememberMe=" + rememberMe + ";";
    }
}
