package com.mojagap.mojanode.service.company;


import com.mojagap.mojanode.dto.ActionResponse;
import com.mojagap.mojanode.dto.company.CompanyDto;
import com.mojagap.mojanode.infrastructure.AppContext;
import com.mojagap.mojanode.infrastructure.ApplicationConstants;
import com.mojagap.mojanode.infrastructure.ErrorMessages;
import com.mojagap.mojanode.infrastructure.PowerValidator;
import com.mojagap.mojanode.infrastructure.exception.BadRequestException;
import com.mojagap.mojanode.infrastructure.utility.Util;
import com.mojagap.mojanode.model.account.Account;
import com.mojagap.mojanode.model.account.AccountType;
import com.mojagap.mojanode.model.branch.Branch;
import com.mojagap.mojanode.model.common.AuditEntity;
import com.mojagap.mojanode.model.company.Company;
import com.mojagap.mojanode.model.company.CompanyType;
import com.mojagap.mojanode.model.user.AppUser;
import com.mojagap.mojanode.model.wallet.Wallet;
import com.mojagap.mojanode.model.wallet.WalletCharge;
import com.mojagap.mojanode.repository.company.CompanyRepository;
import com.mojagap.mojanode.repository.wallet.WalletRepository;
import com.mojagap.mojanode.service.company.handler.CompanyCommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CompanyCommandService implements CompanyCommandHandler {
    private final CompanyRepository companyRepository;
    private final WalletRepository walletRepository;

    @Autowired
    public CompanyCommandService(CompanyRepository companyRepository, WalletRepository walletRepository) {
        this.companyRepository = companyRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    @Override
    public ActionResponse createCompany(CompanyDto companyDto) {
        companyDto.isValid();
        Account account = AppContext.getLoggedInUser().getAccount();
        PowerValidator.isPermittedAccountType(account.getAccountType(), AccountType.COMPANY);
        List<Integer> loggedInUserCompanyIds = AppContext.getCompaniesOfLoggedInUser().stream().map(Company::getId).collect(Collectors.toList());
        if (companyDto.getParentCompany() == null || companyDto.getParentCompany().getId() == null) {
            PowerValidator.throwBadRequestException(String.format(ErrorMessages.ENTITY_REQUIRED, "Parent company ID"));
        }
        if (!loggedInUserCompanyIds.contains(companyDto.getParentCompany().getId())) {
            PowerValidator.throwBadRequestException(ErrorMessages.NOT_PERMITTED_TO_PERFORM_ACTION_ON_COMPANY);
        }
        Company company = Util.copyProperties(companyDto, new Company());
        company.setCompanyType(CompanyType.valueOf(companyDto.getCompanyType()));
        Company parentCompany = companyRepository.findCompanyById(companyDto.getParentCompany().getId())
                .orElseThrow(() -> new BadRequestException(String.format(ErrorMessages.ENTITY_DOES_NOT_EXISTS, Company.class.getSimpleName(), "ID")));
        company.setParentCompany(parentCompany);
        company.setAccount(account);
        Branch branch = new Branch(ApplicationConstants.DEFAULT_OFFICE_NAME, company, account);
        AppContext.stamp(branch);
        company.getBranches().add(branch);
        AppContext.stamp(company);
        Wallet wallet = new Wallet();
        Optional<Wallet> defaultWallet = walletRepository.findDefaultWallet();
        if (defaultWallet.isPresent()) {
            Set<WalletCharge> walletCharges = defaultWallet.get().getWalletCharges();
            wallet.setWalletCharges(walletCharges);
        }
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setActualBalance(BigDecimal.ZERO);
        wallet.setAccount(account);
        wallet.setBranch(branch);
        wallet.setCompany(company);
        AppContext.stamp(wallet);
        company.getWallets().add(wallet);
        companyRepository.save(company);
        return new ActionResponse(company.getId());
    }

    @Override
    @Transactional
    public ActionResponse updateCompany(CompanyDto companyDto, Integer id) {
        companyDto.isValid();
        Account account = AppContext.getLoggedInUser().getAccount();
        PowerValidator.isPermittedAccountType(account.getAccountType(), AccountType.COMPANY);
        Company company = companyRepository.findCompanyById(id)
                .orElseThrow(() -> new BadRequestException(String.format(ErrorMessages.ENTITY_DOES_NOT_EXISTS, Company.class.getSimpleName(), "ID")));
        List<Integer> loggedInUserCompanyIds = AppContext.getCompaniesOfLoggedInUser().stream().map(Company::getId).collect(Collectors.toList());
        if (!loggedInUserCompanyIds.contains(id)) {
            PowerValidator.throwBadRequestException(ErrorMessages.NOT_PERMITTED_TO_PERFORM_ACTION_ON_COMPANY);
        }
        if (companyDto.getParentCompany() != null && companyDto.getParentCompany().getId() != null) {
            if (!loggedInUserCompanyIds.contains(companyDto.getParentCompany().getId())) {
                PowerValidator.throwBadRequestException(ErrorMessages.NOT_PERMITTED_TO_MIGRATE_COMPANY);
            }
            Company parentCompany = companyRepository.findCompanyById(companyDto.getParentCompany().getId())
                    .orElseThrow(() -> new BadRequestException(String.format(ErrorMessages.ENTITY_DOES_NOT_EXISTS, Company.class.getSimpleName(), "ID")));
            company.setParentCompany(parentCompany);
        }
        company.setEmail(companyDto.getEmail());
        company.setPhoneNumber(companyDto.getPhoneNumber());
        company.setAddress(companyDto.getAddress());
        company.setCompanyType(CompanyType.valueOf(companyDto.getCompanyType()));
        company.setName(companyDto.getName());
        company.setRegistrationDate(companyDto.getRegistrationDate());
        companyRepository.save(company);
        return new ActionResponse(id);
    }

    @Override
    @Transactional
    public ActionResponse closeCompany(Integer id) {
        AppUser loggedInUser = AppContext.getLoggedInUser();
        Account account = loggedInUser.getAccount();
        PowerValidator.isPermittedAccountType(account.getAccountType(), AccountType.COMPANY);
        Company company = companyRepository.findCompanyById(id)
                .orElseThrow(() -> new BadRequestException(String.format(ErrorMessages.ENTITY_DOES_NOT_EXISTS, Company.class.getSimpleName(), "ID")));
        List<Integer> loggedInUserCompanyIds = AppContext.getCompaniesOfLoggedInUser().stream().map(Company::getId).collect(Collectors.toList());
        if (!loggedInUserCompanyIds.contains(id) || loggedInUser.getCompany().getId().equals(id)) {
            PowerValidator.throwBadRequestException(ErrorMessages.NOT_PERMITTED_TO_PERFORM_ACTION_ON_COMPANY);
        }
        company.setRecordStatus(AuditEntity.RecordStatus.CLOSED);
        companyRepository.save(company);
        return new ActionResponse(id);
    }

}
