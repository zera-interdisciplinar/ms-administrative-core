package com.zera.ms_administrative_core.infrastructure.http.controller;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.usecase.user.activateUser.ActivateUser;
import com.zera.ms_administrative_core.core.usecase.user.changeUserEmail.ChangeEmail;
import com.zera.ms_administrative_core.core.usecase.user.changeUserPassword.ChangePassword;
import com.zera.ms_administrative_core.core.usecase.user.changeUserPassword.ChangePasswordCommand;
import com.zera.ms_administrative_core.core.usecase.user.deactivateUser.DeactivateUser;
import com.zera.ms_administrative_core.core.usecase.user.findUser.FindAllUsers;
import com.zera.ms_administrative_core.core.usecase.user.findUser.FindUserByEmail;
import com.zera.ms_administrative_core.core.usecase.user.findUser.FindUserById;
import com.zera.ms_administrative_core.core.usecase.user.findUser.UserOutput;
import com.zera.ms_administrative_core.core.usecase.user.registerUser.RegisterUser;
import com.zera.ms_administrative_core.core.usecase.user.registerUser.RegisterUserOutput;
import com.zera.ms_administrative_core.core.usecase.user.renameUser.RenameUser;
import com.zera.ms_administrative_core.core.usecase.user.suspendUser.SuspendUser;
import com.zera.ms_administrative_core.infrastructure.http.request.ChangeEmailRequest;
import com.zera.ms_administrative_core.infrastructure.http.request.ChangePasswordRequest;
import com.zera.ms_administrative_core.infrastructure.http.request.RegisterUserRequest;
import com.zera.ms_administrative_core.infrastructure.http.request.RenameUserRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final RegisterUser registerUser;
    private final RenameUser renameUser;
    private final ChangeEmail changeEmail;
    private final ChangePassword changePassword;
    private final ActivateUser activateUser;
    private final DeactivateUser deactivateUser;
    private final SuspendUser suspendUser;
    private final FindUserByEmail findUserByEmail;
    private final FindAllUsers findAllUsers;
    private final FindUserById findUserById;

    public UserController(RegisterUser registerUser,
                          RenameUser renameUser,
                          ChangeEmail changeEmail,
                          ChangePassword changePassword,
                          ActivateUser activateUser,
                          DeactivateUser deactivateUser,
                          SuspendUser suspendUser,
                          FindUserByEmail findUserByEmail,
                          FindUserById findUserById,
                          FindAllUsers findAllUsers) {
        this.registerUser = registerUser;
        this.renameUser = renameUser;
        this.changeEmail = changeEmail;
        this.changePassword = changePassword;
        this.activateUser = activateUser;
        this.deactivateUser = deactivateUser;
        this.suspendUser = suspendUser;
        this.findUserByEmail = findUserByEmail;
        this.findAllUsers = findAllUsers;
        this.findUserById = findUserById;
    }

    @PostMapping
    public ResponseEntity<RegisterUserOutput> register(@RequestBody @Valid RegisterUserRequest request) {
        RegisterUserOutput output = registerUser.execute(request.toCommand());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(output.userId())
                .toUri();
        return ResponseEntity.created(location).body(output);
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<Void> rename(@PathVariable UUID id,
                                       @RequestBody @Valid RenameUserRequest request) {
        renameUser.execute(id, request.name());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/email")
    public ResponseEntity<Void> changeEmail(@PathVariable UUID id,
                                            @RequestBody @Valid ChangeEmailRequest request) {
        changeEmail.execute(id, request.email());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable UUID id,
                                               @RequestBody @Valid ChangePasswordRequest request) {
        changePassword.execute(new ChangePasswordCommand(
                id,
                request.rawPassword(),
                request.confirmPassword()
        ));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        activateUser.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        deactivateUser.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<Void> suspend(@PathVariable UUID id) {
        suspendUser.execute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UserOutput>> findAll(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (email != null) {
            return ResponseEntity.ok(List.of(findUserByEmail.execute(email)));
        }
        return ResponseEntity.ok(findAllUsers.execute(role, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserOutput> findById(@PathVariable UUID id) {
        UserOutput output = findUserById.execute(id);
        return ResponseEntity.ok(output);
    }
}
