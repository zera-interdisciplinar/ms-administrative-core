package com.zera.ms_administrative_core.infrastructure.http.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.zera.ms_administrative_core.core.usecase.user.findInvitation.FindPendingInvitations;
import com.zera.ms_administrative_core.core.usecase.user.findInvitation.PendingInvitationOutput;
import com.zera.ms_administrative_core.core.usecase.user.generateInvitationCode.GenerateInvitationCode;
import com.zera.ms_administrative_core.core.usecase.user.generateInvitationCode.GenerateInvitationCodeOutput;
import com.zera.ms_administrative_core.core.usecase.user.registerUser.RegisterUserOutput;
import com.zera.ms_administrative_core.core.usecase.user.registerWithInvitationCode.RegisterWithInvitationCode;
import com.zera.ms_administrative_core.infrastructure.http.request.GenerateInvitationCodeRequest;
import com.zera.ms_administrative_core.infrastructure.http.request.RegisterWithInvitationCodeRequest;
import com.zera.ms_administrative_core.infrastructure.security.Authz;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final GenerateInvitationCode generateInvitationCode;
    private final RegisterWithInvitationCode registerWithInvitationCode;
    private final FindPendingInvitations findPendingInvitations;

    public InvitationController(GenerateInvitationCode generateInvitationCode,
            RegisterWithInvitationCode registerWithInvitationCode,
            FindPendingInvitations findPendingInvitations) {
        this.generateInvitationCode = generateInvitationCode;
        this.registerWithInvitationCode = registerWithInvitationCode;
        this.findPendingInvitations = findPendingInvitations;
    }

    @PostMapping
    @PreAuthorize(Authz.MANAGER)
    public ResponseEntity<GenerateInvitationCodeOutput> generate(
            @RequestBody @Valid GenerateInvitationCodeRequest request) {
        GenerateInvitationCodeOutput output = generateInvitationCode.execute(request.managerId(), request.inviteeName());
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    @GetMapping("/pending")
    @PreAuthorize(Authz.MANAGER)
    public ResponseEntity<List<PendingInvitationOutput>> findPending(@RequestParam UUID managerId) {
        return ResponseEntity.ok(findPendingInvitations.execute(managerId));
    }

    @PostMapping("/redeem")
    public ResponseEntity<RegisterUserOutput> redeem(
            @RequestBody @Valid RegisterWithInvitationCodeRequest request) {
        RegisterUserOutput output = registerWithInvitationCode.execute(request.toCommand());
        URI location = ServletUriComponentsBuilder
                .fromPath("/api/v1/users/{id}")
                .buildAndExpand(output.userId())
                .toUri();
        return ResponseEntity.created(location).body(output);
    }
}
