package fr.svivien.cgbenchmark.model.config;

public class AccountConfiguration {

    private String accountName;
    private String accountCookie;
    private String accountIde;
    private String accountLogin;
    private String accountPassword;

    public void setAccountIde(String accountIde) {
        this.accountIde = accountIde;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    public String getAccountLogin() {
        return accountLogin;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getAccountCookie() {
        return accountCookie;
    }

    public String getAccountIde() {
        return accountIde;
    }

    public void setAccountCookie(String accountCookie) {
        this.accountCookie = accountCookie;
    }
}
