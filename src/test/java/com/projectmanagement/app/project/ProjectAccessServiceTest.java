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
import com.projectmanagement.app.workspace.Workspace;
import com.projectmanagement.app.workspace.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProjectUserRepository projectUserRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    private ProjectAccessService service;
    private Project project;

    @BeforeEach
    void setUp() {
        service = new ProjectAccessService(
                currentUserService,
                projectUserRepository,
                workspaceMemberRepository);

        User owner = User.builder()
                .id(10L)
                .name("Owner")
                .email("owner@example.com")
                .build();

        Workspace workspace = Workspace.builder()
                .id(20L)
                .name("Workspace")
                .slug("workspace")
                .build();

        project = Project.builder()
                .id(30L)
                .owner(owner)
                .workspace(workspace)
                .build();

        /*
         * This is used by all normal-user tests.
         *
         * Do NOT put the workspace membership stub here because
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

        when(workspaceMemberRepository
                .existsByWorkspaceIdAndUserId(20L, 11L))
                .thenReturn(true);

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

        when(workspaceMemberRepository
                .existsByWorkspaceIdAndUserId(20L, 11L))
                .thenReturn(true);

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

        when(workspaceMemberRepository
                .existsByWorkspaceIdAndUserId(20L, 11L))
                .thenReturn(true);

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
    void workspaceOutsiderCannotViewEvenWhenProjectMembershipExists() {

        /*
         * The workspace check must fail first.
         */
        when(workspaceMemberRepository
                .existsByWorkspaceIdAndUserId(20L, 11L))
                .thenReturn(false);

        /*
         * Deliberately DO NOT stub project membership here.
         *
         * If workspace access is denied, the service should never
         * need to check project membership.
         */

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
         * Do NOT stub workspace membership.
         *
         * A system admin should bypass both workspace and project
         * membership checks.
         */

        assertDoesNotThrow(
                () -> service.requireManager(project));

        verifyNoInteractions(projectUserRepository);
        verifyNoInteractions(workspaceMemberRepository);
    }
}