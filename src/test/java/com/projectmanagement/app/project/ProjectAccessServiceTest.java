package com.projectmanagement.app.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.projectmanagement.app.auth.CurrentUserService;
import com.projectmanagement.app.user.User;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProjectUserRepository projectUserRepository;

    private ProjectAccessService service;
    private Project project;

    @BeforeEach
    void setUp() {
        service = new ProjectAccessService(
                currentUserService,
                projectUserRepository);

        User owner = User.builder()
                .id(10L)
                .name("Owner")
                .email("owner@example.com")
                .build();

        project = Project.builder()
                .id(30L)
                .owner(owner)
                .build();

        /*
         * This is used by all normal-user tests.
         *
         * Do NOT put the currentUserId stub anywhere else, since
         * system-admin tests intentionally bypass membership checks.
         */
        when(currentUserService.getCurrentUserId()).thenReturn(11L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user",
                        null));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void viewerCanReadButCannotEdit() {

        ProjectUser membership = ProjectUser.builder()
                .role("VIEWER")
                .build();

        when(projectUserRepository
                .existsByProjectIdAndUserId(30L, 11L))
                .thenReturn(true);

        when(projectUserRepository
                .findByProjectIdAndUserId(30L, 11L))
                .thenReturn(Optional.of(membership));

        assertTrue(service.canView(project));

        assertThrows(
                RuntimeException.class,
                () -> service.requireEditor(project));

        assertThrows(
                RuntimeException.class,
                () -> service.requireManager(project));
    }

    @Test
    void memberCanEditButCannotManage() {

        ProjectUser membership = ProjectUser.builder()
                .role("MEMBER")
                .build();

        when(projectUserRepository
                .existsByProjectIdAndUserId(30L, 11L))
                .thenReturn(true);

        when(projectUserRepository
                .findByProjectIdAndUserId(30L, 11L))
                .thenReturn(Optional.of(membership));

        assertDoesNotThrow(
                () -> service.requireEditor(project));

        assertThrows(
                RuntimeException.class,
                () -> service.requireManager(project));
    }

    @Test
    void adminCanManage() {

        ProjectUser membership = ProjectUser.builder()
                .role("ADMIN")
                .build();

        when(projectUserRepository
                .existsByProjectIdAndUserId(30L, 11L))
                .thenReturn(true);

        when(projectUserRepository
                .findByProjectIdAndUserId(30L, 11L))
                .thenReturn(Optional.of(membership));

        assertDoesNotThrow(
                () -> service.requireManager(project));
    }

    @Test
    void nonMemberNonOwnerCannotView() {

        /*
         * Neither the project owner nor an assigned project member.
         */
        when(projectUserRepository
                .existsByProjectIdAndUserId(30L, 11L))
                .thenReturn(false);

        assertFalse(service.canView(project));

        assertThrows(
                RuntimeException.class,
                () -> service.requireView(project));
    }

    @Test
    void systemAdminBypassesMembershipChecks() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"))));

        /*
         * A system admin should bypass project membership checks
         * entirely, so the project-user repository is never consulted.
         */

        assertDoesNotThrow(
                () -> service.requireManager(project));

        verifyNoInteractions(projectUserRepository);
    }
}
