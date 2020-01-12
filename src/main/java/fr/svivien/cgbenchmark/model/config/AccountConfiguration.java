package fr.svivien.cgbenchmark.model.config;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AccountConfiguration {

    @NotNull
    private String accountName;

    private String accountCookie;

    private String accountIde;

    @NotNull
    private String accountLogin;

    @NotNull
    private String accountPassword;

}
