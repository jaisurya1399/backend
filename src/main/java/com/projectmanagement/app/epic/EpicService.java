package com.projectmanagement.app.epic;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.project.Project;
import com.projectmanagement.app.project.ProjectRepository;

@Service
@Transactional
public class EpicService {

    private final EpicRepository epicRepository;
    private final ProjectRepository projectRepository;

    public EpicService(
            EpicRepository epicRepository,
            ProjectRepository projectRepository) {
        this.epicRepository = epicRepository;
        this.projectRepository = projectRepository;
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Transactional(readOnly = true)
    public List<EpicResponse> getAllEpics() {

        return epicRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET ACTIVE
    // =========================================================

    @Transactional(readOnly = true)
    public List<EpicResponse> getActiveEpics() {

        return epicRepository.findByDeletedAtIsNull()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public EpicResponse getEpicById(Long id) {

        Epic epic = epicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Epic not found with id: " + id));

        return toResponse(epic);
    }

    // =========================================================
    // GET BY PROJECT
    // =========================================================

    @Transactional(readOnly = true)
    public List<EpicResponse> getEpicsByProject(
            Long projectId) {

        validateProject(projectId);

        return epicRepository
                .findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET ACTIVE BY PROJECT
    // =========================================================

    @Transactional(readOnly = true)
    public List<EpicResponse> getActiveEpicsByProject(
            Long projectId) {

        validateProject(projectId);

        return epicRepository
                .findByProjectIdAndDeletedAtIsNull(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET ROOT EPICS
    // =========================================================

    @Transactional(readOnly = true)
    public List<EpicResponse> getRootEpicsByProject(
            Long projectId) {

        validateProject(projectId);

        return epicRepository
                .findByProjectIdAndParentIsNull(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET CHILD EPICS
    // =========================================================

    @Transactional(readOnly = true)
    public List<EpicResponse> getChildEpics(
            Long parentId) {

        validateEpic(parentId);

        return epicRepository
                .findByParentId(parentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET ACTIVE CHILD EPICS
    // =========================================================

    @Transactional(readOnly = true)
    public List<EpicResponse> getActiveChildEpics(
            Long parentId) {

        validateEpic(parentId);

        return epicRepository
                .findByParentIdAndDeletedAtIsNull(parentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET BY PROJECT + PARENT
    // =========================================================

    @Transactional(readOnly = true)
    public List<EpicResponse> getEpicsByProjectAndParent(
            Long projectId,
            Long parentId) {

        validateProject(projectId);
        validateEpic(parentId);

        return epicRepository
                .findByProjectIdAndParentId(
                        projectId,
                        parentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET BY NAME
    // =========================================================

    @Transactional(readOnly = true)
    public EpicResponse getEpicByProjectAndName(
            Long projectId,
            String name) {

        validateProject(projectId);

        Epic epic = epicRepository
                .findByProjectIdAndNameAndDeletedAtIsNull(
                        projectId,
                        name)
                .orElseThrow(() -> new RuntimeException(
                        "Epic not found with name: " + name));

        return toResponse(epic);
    }

    // =========================================================
    // CREATE
    // =========================================================

    public EpicResponse createEpic(
            EpicRequest request) {

        validateProject(request.getProjectId());

        validateDateRange(
                request.getStartsAt(),
                request.getEndsAt());

        validateEpicName(
                request.getProjectId(),
                request.getName(),
                null);

        Project project = projectRepository
                .findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException(
                        "Project not found with id: "
                                + request.getProjectId()));

        Epic parent = null;

        if (request.getParentId() != null) {

            parent = getValidParent(
                    request.getParentId(),
                    request.getProjectId(),
                    null);
        }

        Epic epic = Epic.builder()
                .project(project)
                .name(request.getName())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .parent(parent)
                .build();

        return toResponse(
                epicRepository.save(epic));
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public EpicResponse updateEpic(
            Long id,
            EpicRequest request) {

        Epic epic = epicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Epic not found with id: " + id));

        validateProject(request.getProjectId());

        validateDateRange(
                request.getStartsAt(),
                request.getEndsAt());

        validateEpicName(
                request.getProjectId(),
                request.getName(),
                id);

        Project project = projectRepository
                .findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException(
                        "Project not found with id: "
                                + request.getProjectId()));

        Epic parent = null;

        if (request.getParentId() != null) {

            parent = getValidParent(
                    request.getParentId(),
                    request.getProjectId(),
                    id);
        }

        epic.setProject(project);
        epic.setName(request.getName());
        epic.setStartsAt(request.getStartsAt());
        epic.setEndsAt(request.getEndsAt());
        epic.setParent(parent);

        return toResponse(
                epicRepository.save(epic));
    }

    // =========================================================
    // SOFT DELETE
    // =========================================================

    public void deleteEpic(Long id) {

        Epic epic = epicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Epic not found with id: " + id));

        epic.setDeletedAt(
                LocalDateTime.now());

        epicRepository.save(epic);
    }

    // =========================================================
    // RESTORE
    // =========================================================

    public EpicResponse restoreEpic(Long id) {

        Epic epic = epicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Epic not found with id: " + id));

        epic.setDeletedAt(null);

        return toResponse(
                epicRepository.save(epic));
    }

    // =========================================================
    // PERMANENT DELETE
    // =========================================================

    public void permanentlyDeleteEpic(Long id) {

        if (!epicRepository.existsById(id)) {

            throw new RuntimeException(
                    "Epic not found with id: " + id);
        }

        epicRepository.deleteById(id);
    }

    // =========================================================
    // COUNT
    // =========================================================

    @Transactional(readOnly = true)
    public long countEpicsByProject(
            Long projectId) {

        validateProject(projectId);

        return epicRepository.countByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public long countChildEpics(
            Long parentId) {

        validateEpic(parentId);

        return epicRepository.countByParentId(parentId);
    }

    // =========================================================
    // VALIDATE PROJECT
    // =========================================================

    private void validateProject(Long projectId) {

        if (!projectRepository.existsById(projectId)) {

            throw new RuntimeException(
                    "Project not found with id: " + projectId);
        }
    }

    // =========================================================
    // VALIDATE EPIC
    // =========================================================

    private void validateEpic(Long epicId) {

        if (!epicRepository.existsById(epicId)) {

            throw new RuntimeException(
                    "Epic not found with id: " + epicId);
        }
    }

    // =========================================================
    // VALIDATE NAME
    // =========================================================

    private void validateEpicName(
            Long projectId,
            String name,
            Long currentId) {

        boolean exists;

        if (currentId == null) {

            exists = epicRepository
                    .existsByProjectIdAndName(
                            projectId,
                            name);

        } else {

            exists = epicRepository
                    .existsByProjectIdAndNameAndIdNot(
                            projectId,
                            name,
                            currentId);
        }

        if (exists) {

            throw new RuntimeException(
                    "Epic already exists in this project with name: "
                            + name);
        }
    }

    // =========================================================
    // VALIDATE DATE RANGE
    // =========================================================

    private void validateDateRange(
            java.time.LocalDate startsAt,
            java.time.LocalDate endsAt) {

        if (startsAt == null || endsAt == null) {
            return;
        }

        if (endsAt.isBefore(startsAt)) {

            throw new RuntimeException(
                    "End date must be on or after start date");
        }
    }

    // =========================================================
    // VALIDATE PARENT
    // =========================================================

    private Epic getValidParent(
            Long parentId,
            Long projectId,
            Long currentEpicId) {

        if (currentEpicId != null &&
                currentEpicId.equals(parentId)) {

            throw new RuntimeException(
                    "An epic cannot be its own parent");
        }

        Epic parent = epicRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException(
                        "Parent epic not found with id: "
                                + parentId));

        if (!parent.getProject().getId()
                .equals(projectId)) {

            throw new RuntimeException(
                    "Parent epic must belong to the same project");
        }

        if (parent.getDeletedAt() != null) {

            throw new RuntimeException(
                    "Cannot assign a deleted epic as parent");
        }

        return parent;
    }

    // =========================================================
    // RESPONSE MAPPER
    // =========================================================

    private EpicResponse toResponse(Epic epic) {

        Project project = epic.getProject();
        Epic parent = epic.getParent();

        return EpicResponse.builder()

                .id(epic.getId())

                .projectId(
                        project != null
                                ? project.getId()
                                : null)

                .projectName(
                        project != null
                                ? project.getName()
                                : null)

                .name(epic.getName())

                .startsAt(epic.getStartsAt())
                .endsAt(epic.getEndsAt())

                .parentId(
                        parent != null
                                ? parent.getId()
                                : null)

                .parentName(
                        parent != null
                                ? parent.getName()
                                : null)

                .deletedAt(epic.getDeletedAt())
                .createdAt(epic.getCreatedAt())
                .updatedAt(epic.getUpdatedAt())

                .build();
    }
}