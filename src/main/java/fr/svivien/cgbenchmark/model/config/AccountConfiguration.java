package fr.svivien.cgbenchmark.model.config;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AccountConfiguration {

    @NotNull
    private String rememberMe;

    @NotNull
    private String accountId;

}
