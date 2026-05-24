package com.petbook.app.models;

public class CompanyProfile {
    private final Long id;
    private final Long ownerUserId;
    private final String companyName;
    private final String cnpj;
    private final String address;
    private final String phone;

    public CompanyProfile(
            Long id,
            Long ownerUserId,
            String companyName,
            String cnpj,
            String address,
            String phone
    ) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.companyName = companyName;
        this.cnpj = cnpj;
        this.address = address;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }
}
