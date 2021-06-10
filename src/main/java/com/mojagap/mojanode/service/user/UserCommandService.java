package com.mojagap.mojanode.service.user;

import com.mojagap.mojanode.controller.user.entity.AppUserSummary;
import com.mojagap.mojanode.infrastructure.AppContext;
import com.mojagap.mojanode.infrastructure.ApplicationConstants;
import com.mojagap.mojanode.infrastructure.security.AppUserDetails;
import com.mojagap.mojanode.model.http.ExternalUser;
import com.mojagap.mojanode.model.user.AppUser;
import com.mojagap.mojanode.repository.user.AppUserRepository;
import com.mojagap.mojanode.repository.user.OrganizationRepository;
import com.mojagap.mojanode.service.httpgateway.RestTemplateService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Date;

@Service
public class UserCommandService {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RestTemplateService restTemplateService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    protected HttpServletResponse httpServletResponse;


    public AppUserSummary createUser(AppUserSummary appUserSummary) {
        AppUser loggedInUser = AppContext.getLoggedInUser();
        AppUser appUser = new AppUser(appUserSummary);
        if (loggedInUser != null) {
            appUser.setOrganization(loggedInUser.getOrganization());
        } else {
            appUser.setCreatedBy(appUser);
            appUser.setModifiedBy(appUser);
        }
        appUser.setPassword(passwordEncoder.encode(appUserSummary.getPassword()));
        appUser = appUserRepository.saveAndFlush(appUser);
        appUserSummary.setId(appUser.getId());
        return appUserSummary;
    }

    public AppUserSummary authenticateUser(AppUserSummary appUserSummary) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(appUserSummary.getEmail(), appUserSummary.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Date expiryDate = new Date(System.currentTimeMillis() + ApplicationConstants.JWT_EXPIRATION_TIME);
        String secretKey = Base64.getEncoder().encodeToString(ApplicationConstants.JWT_SECRET_KEY.getBytes());

        AppUser appUser = ((AppUserDetails) authentication.getPrincipal()).getAppUser();
        Claims claims = Jwts.claims().setSubject(appUser.getEmail());
        claims.put("userId", appUser.getId());

        String authenticationToken = Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, secretKey).setExpiration(expiryDate).compact();
        appUserSummary.setAuthentication(authenticationToken);
        BeanUtils.copyProperties(appUser, appUserSummary);
        appUserSummary.setPassword(null);
        if (appUser.getOrganization() != null) {
            appUserSummary.setOrganizationId(appUser.getOrganization().getId());
            appUserSummary.setOrganizationName(appUser.getOrganization().getName());
        }
        httpServletResponse.setHeader(ApplicationConstants.AUTHENTICATION_HEADER_NAME, authenticationToken);
        return appUserSummary;
    }

    public AppUserSummary updateUser(AppUserSummary appUserSummary) {
        return appUserSummary;
    }

    public AppUserSummary removeUser(Integer userId) {
        return new AppUserSummary();
    }

    public ExternalUser createExternalUser(ExternalUser externalUser) {
        return restTemplateService.doHttpPost(ApplicationConstants.BANK_TRANSFER_BASE_URL + "/users", externalUser, ExternalUser.class);

    }
}
