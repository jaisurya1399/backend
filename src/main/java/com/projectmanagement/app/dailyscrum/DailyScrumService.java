package com.projectmanagement.app.dailyscrum;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.project.Project;
import com.projectmanagement.app.project.ProjectRepository;
import com.projectmanagement.app.user.User;
import com.projectmanagement.app.user.UserRepository;

@Service
@Transactional
public class DailyScrumService {

        private final DailyScrumRepository dailyScrumRepository;
        private final UserRepository userRepository;
        private final ProjectRepository projectRepository;

        public DailyScrumService(
                        DailyScrumRepository dailyScrumRepository,
                        UserRepository userRepository,
                        ProjectRepository projectRepository) {

                this.dailyScrumRepository = dailyScrumRepository;
                this.userRepository = userRepository;
                this.projectRepository = projectRepository;
        }

        /*
         * CREATE
         *
         * loggedInUserId = Team Lead/Admin
         * request.userId = Developer whose scrum is being created
         */
        public DailyScrumResponse create(
                        Long loggedInUserId,
                        DailyScrumRequest request) {

                validateDate(request.getScrumDate());

                User targetUser = getUser(request.getUserId());

                Project project = getProject(request.getProjectId());

                /*
                 * Permission:
                 *
                 * ADMIN -> allowed
                 * TEAM LEAD -> only own project members
                 */
                validateCanManageScrum(
                                loggedInUserId,
                                targetUser.getId(),
                                project.getId());

                boolean exists = dailyScrumRepository
                                .existsByUserIdAndProjectIdAndScrumDate(
                                                targetUser.getId(),
                                                project.getId(),
                                                request.getScrumDate());

                if (exists) {
                        throw new IllegalArgumentException(
                                        "Daily scrum already exists for this user, project and date");
                }

                DailyScrum scrum = DailyScrum.builder()
                                .user(targetUser)
                                .project(project)
                                .scrumDate(
                                                request.getScrumDate())
                                .yesterdayWork(
                                                request.getYesterdayWork().trim())
                                .todayWork(
                                                request.getTodayWork().trim())
                                .blockers(
                                                request.getBlockers() == null
                                                                ? ""
                                                                : request.getBlockers().trim())
                                .build();

                return toResponse(
                                dailyScrumRepository.save(scrum));
        }

        /*
         * UPDATE
         */
        public DailyScrumResponse update(
                        Long loggedInUserId,
                        Long id,
                        DailyScrumRequest request) {

                validateDate(request.getScrumDate());

                DailyScrum scrum = dailyScrumRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Daily scrum not found"));

                Project project = getProject(request.getProjectId());

                User targetUser = getUser(request.getUserId());

                validateCanManageScrum(
                                loggedInUserId,
                                targetUser.getId(),
                                project.getId());

                boolean changed = !scrum.getUser()
                                .getId()
                                .equals(targetUser.getId())
                                ||
                                !scrum.getProject()
                                                .getId()
                                                .equals(project.getId())
                                ||
                                !scrum.getScrumDate()
                                                .equals(request.getScrumDate());

                if (changed) {

                        boolean exists = dailyScrumRepository
                                        .existsByUserIdAndProjectIdAndScrumDate(
                                                        targetUser.getId(),
                                                        project.getId(),
                                                        request.getScrumDate());

                        if (exists &&
                                        !scrum.getId()
                                                        .equals(
                                                                        dailyScrumRepository
                                                                                        .findByUserIdAndProjectIdAndScrumDate(
                                                                                                        targetUser.getId(),
                                                                                                        project.getId(),
                                                                                                        request.getScrumDate())
                                                                                        .map(DailyScrum::getId)
                                                                                        .orElse(null))) {

                                throw new IllegalArgumentException(
                                                "Daily scrum already exists for this user, project and date");
                        }
                }

                scrum.setUser(targetUser);
                scrum.setProject(project);
                scrum.setScrumDate(
                                request.getScrumDate());

                scrum.setYesterdayWork(
                                request.getYesterdayWork().trim());

                scrum.setTodayWork(
                                request.getTodayWork().trim());

                scrum.setBlockers(
                                request.getBlockers() == null
                                                ? ""
                                                : request.getBlockers().trim());

                return toResponse(
                                dailyScrumRepository.save(scrum));
        }

        /*
         * ANY AUTHENTICATED USER CAN VIEW.
         */

        @Transactional(readOnly = true)
        public List<DailyScrumResponse> getAll() {

                return dailyScrumRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public DailyScrumResponse getById(
                        Long id) {

                return toResponse(
                                dailyScrumRepository.findById(id)
                                                .orElseThrow(() -> new RuntimeException(
                                                                "Daily scrum not found")));
        }

        @Transactional(readOnly = true)
        public List<DailyScrumResponse> getByUser(
                        Long userId) {

                return dailyScrumRepository
                                .findByUserIdOrderByScrumDateDesc(
                                                userId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<DailyScrumResponse> getByUserAndRange(
                        Long userId,
                        LocalDate startDate,
                        LocalDate endDate) {

                validateRange(
                                startDate,
                                endDate);

                return dailyScrumRepository
                                .findByUserIdAndScrumDateBetweenOrderByScrumDateAsc(
                                                userId,
                                                startDate,
                                                endDate)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<DailyScrumResponse> getByDateRange(
                        LocalDate startDate,
                        LocalDate endDate) {

                validateRange(
                                startDate,
                                endDate);

                return dailyScrumRepository
                                .findByScrumDateBetweenOrderByScrumDateAsc(
                                                startDate,
                                                endDate)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<DailyScrumResponse> getByProjectAndRange(
                        Long projectId,
                        LocalDate startDate,
                        LocalDate endDate) {

                validateRange(
                                startDate,
                                endDate);

                return dailyScrumRepository
                                .findByProjectIdAndScrumDateBetweenOrderByScrumDateAsc(
                                                projectId,
                                                startDate,
                                                endDate)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        /*
         * DELETE
         *
         * Same permission as Add/Update.
         */
        public void delete(
                        Long loggedInUserId,
                        Long id) {

                DailyScrum scrum = dailyScrumRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Daily scrum not found"));

                validateCanManageScrum(
                                loggedInUserId,
                                scrum.getUser().getId(),
                                scrum.getProject().getId());

                dailyScrumRepository.delete(scrum);
        }

        /*
         * =========================================================
         * PERMISSION
         * =========================================================
         *
         * IMPORTANT:
         *
         * Replace this method's implementation with your exact
         * ProjectUserRepository implementation.
         */
        private void validateCanManageScrum(
                        Long loggedInUserId,
                        Long targetUserId,
                        Long projectId) {

                /*
                 * TODO:
                 *
                 * 1. Check logged-in user ADMIN
                 * 2. Check Team Lead assignment
                 * 3. Check target user project assignment
                 *
                 * Example expected logic:
                 *
                 * if admin:
                 * return
                 *
                 * if teamLead of project:
                 * if target user assigned:
                 * return
                 *
                 * throw 403
                 */

                if (loggedInUserId == null) {
                        throw new RuntimeException(
                                        "Unauthenticated user");
                }

                /*
                 * Temporary place for exact ProjectUser
                 * integration.
                 *
                 * DO NOT remove authorization.
                 */
                throw new RuntimeException(
                                "Daily Scrum permission check is not configured. "
                                                + "Connect ProjectUserRepository here.");
        }

        private User getUser(Long id) {

                return userRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "User not found: " + id));
        }

        private Project getProject(Long id) {

                return projectRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found: " + id));
        }

        private void validateDate(
                        LocalDate date) {

                if (date == null) {
                        throw new IllegalArgumentException(
                                        "Scrum date is required");
                }

                if (date.isAfter(LocalDate.now())) {
                        throw new IllegalArgumentException(
                                        "Scrum date cannot be in the future");
                }
        }

        private void validateRange(
                        LocalDate startDate,
                        LocalDate endDate) {

                if (startDate == null ||
                                endDate == null) {

                        throw new IllegalArgumentException(
                                        "Start date and end date are required");
                }

                if (startDate.isAfter(endDate)) {

                        throw new IllegalArgumentException(
                                        "Start date cannot be after end date");
                }
        }

        private DailyScrumResponse toResponse(
                        DailyScrum scrum) {

                return DailyScrumResponse.builder()

                                .id(scrum.getId())

                                .userId(
                                                scrum.getUser().getId())
                                .userName(
                                                scrum.getUser().getName())
                                .userEmail(
                                                scrum.getUser().getEmail())

                                .projectId(
                                                scrum.getProject().getId())
                                .projectName(
                                                scrum.getProject().getName())

                                .scrumDate(
                                                scrum.getScrumDate())

                                .yesterdayWork(
                                                scrum.getYesterdayWork())

                                .todayWork(
                                                scrum.getTodayWork())

                                .blockers(
                                                scrum.getBlockers())

                                .createdAt(
                                                scrum.getCreatedAt())

                                .updatedAt(
                                                scrum.getUpdatedAt())

                                .build();
        }
}