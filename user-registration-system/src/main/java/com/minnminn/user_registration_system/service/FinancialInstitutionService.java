package com.minnminn.user_registration_system.service;

import com.minnminn.user_registration_system.dto.CredentialLine;
import com.minnminn.user_registration_system.entity.Credential;
import com.minnminn.user_registration_system.entity.FinancialInstitution;
import com.minnminn.user_registration_system.entity.RowHighlight;
import com.minnminn.user_registration_system.entity.SystemType;
import com.minnminn.user_registration_system.entity.SystemTypeCode;
import com.minnminn.user_registration_system.repository.CredentialRepository;
import com.minnminn.user_registration_system.repository.FinancialInstitutionRepository;
import com.minnminn.user_registration_system.repository.ResultNoticeRepository;
import com.minnminn.user_registration_system.repository.SystemTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FinancialInstitutionService {

    private final FinancialInstitutionRepository fiRepository;
    private final CredentialRepository credentialRepository;
    private final SystemTypeRepository systemTypeRepository;
    private final ResultNoticeRepository noticeRepository;
    private final PasswordGeneratorService passwordGeneratorService;

    public FinancialInstitutionService(
            FinancialInstitutionRepository fiRepository,
            CredentialRepository credentialRepository,
            SystemTypeRepository systemTypeRepository,
            ResultNoticeRepository noticeRepository,
            PasswordGeneratorService passwordGeneratorService) {
        this.fiRepository = fiRepository;
        this.credentialRepository = credentialRepository;
        this.systemTypeRepository = systemTypeRepository;
        this.noticeRepository = noticeRepository;
        this.passwordGeneratorService = passwordGeneratorService;
    }

    public List<FinancialInstitution> findAll() {
        return fiRepository.findAllByOrderBySortOrderAscBankNameAsc();
    }

    public List<FinancialInstitution> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String q = keyword.trim();
        return fiRepository.findByBankNameContainingIgnoreCaseOrFiCodeContainingIgnoreCaseOrderBySortOrderAsc(q, q);
    }

    public Optional<FinancialInstitution> findById(Long id) {
        return fiRepository.findById(id);
    }

    public Optional<FinancialInstitution> findByFiCode(String fiCode) {
        return fiRepository.findByFiCode(fiCode);
    }

    public FinancialInstitution save(FinancialInstitution fi) {
        if (fi.getRowHighlight() == null) {
            fi.setRowHighlight(RowHighlight.NORMAL);
        }
        return fiRepository.save(fi);
    }

    @Transactional
    public void deleteInstitution(Long id) {
        noticeRepository.deleteByFinancialInstitutionId(id);
        List<Credential> credentials = credentialRepository.findByFinancialInstitutionId(id);
        credentialRepository.deleteAll(credentials);
        fiRepository.deleteById(id);
    }

    public List<Credential> findCredentials(Long fiId) {
        FinancialInstitution fi = idOrThrow(fiId);
        return credentialRepository.findByFinancialInstitutionOrderBySystemTypeSortOrderAsc(fi);
    }

    public Optional<Credential> findCredentialById(Long id) {
        return credentialRepository.findById(id);
    }

    public List<SystemType> findAllSystemTypes() {
        return systemTypeRepository.findAllByOrderBySortOrderAsc();
    }

    public List<SystemType> findActiveSystemTypes() {
        return systemTypeRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    public Optional<SystemType> findSystemTypeByCode(SystemTypeCode code) {
        return systemTypeRepository.findByCode(code);
    }

    @Transactional
    public Credential upsertCredential(Credential credential) {
        return credentialRepository.save(credential);
    }

    public Optional<Credential> findCredential(FinancialInstitution fi, SystemType systemType) {
        return credentialRepository.findByFinancialInstitutionAndSystemType(fi, systemType);
    }

    @Transactional
    public Credential createOrUpdateCredential(
            Long fiId,
            Long systemTypeId,
            String userId,
            String password,
            String preSharedKey,
            String notes,
            LocalDate updateDate) {

        FinancialInstitution fi = idOrThrow(fiId);
        SystemType systemType = systemTypeRepository.findById(systemTypeId)
                .orElseThrow(() -> new IllegalArgumentException("System type not found"));

        Credential credential = credentialRepository
                .findByFinancialInstitutionAndSystemType(fi, systemType)
                .orElseGet(Credential::new);
        credential.setFinancialInstitution(fi);
        credential.setSystemType(systemType);
        credential.setUserId(blankToNull(userId));
        credential.setPassword(blankToNull(password));
        if (systemType.getCode() == SystemTypeCode.VPN) {
            credential.setPreSharedKey(blankToNull(preSharedKey));
        } else {
            credential.setPreSharedKey(null);
        }
        credential.setNotes(blankToNull(notes));
        credential.setUpdateDate(updateDate != null ? updateDate : LocalDate.now());
        return credentialRepository.save(credential);
    }

    @Transactional
    public Credential generateAndSave(Long credentialId, String type) {
        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        switch (type) {
            case "psk" -> credential.setPreSharedKey(passwordGeneratorService.generatePsk());
            case "password" -> credential.setPassword(passwordGeneratorService.generateUserPassword());
            default -> throw new IllegalArgumentException("Unsupported generate type: " + type);
        }
        credential.setUpdateDate(LocalDate.now());
        return credentialRepository.save(credential);
    }

    @Transactional
    public FinancialInstitution createWithEmptyCredentials(FinancialInstitution fi) {
        return createWithCredentials(fi, null);
    }

    /**
     * Create FI and save credentials only for selected system types.
     */
    @Transactional
    public FinancialInstitution createWithCredentials(FinancialInstitution fi, List<Long> selectedSystemTypeIds, List<CredentialLine> lines) {
        if (fiRepository.findByFiCode(fi.getFiCode()).isPresent()) {
            throw new IllegalArgumentException("FI Code already exists: " + fi.getFiCode());
        }
        if (fi.getSortOrder() == null) {
            fi.setSortOrder((int) fiRepository.count() + 1);
        }
        FinancialInstitution saved = save(fi);
        if (selectedSystemTypeIds != null && !selectedSystemTypeIds.isEmpty()) {
            for (Long systemTypeId : selectedSystemTypeIds) {
                SystemType systemType = systemTypeRepository.findById(systemTypeId)
                        .orElseThrow(() -> new IllegalArgumentException("System type not found: " + systemTypeId));
                Credential c = new Credential();
                c.setFinancialInstitution(saved);
                c.setSystemType(systemType);
                credentialRepository.save(c);
            }
        }
        if (lines != null && !lines.isEmpty()) {
            applyCredentialLines(saved, lines);
        }
        return saved;
    }

    /**
     * Update FI fields and system credentials from create/edit form.
     */
    @Transactional
    public FinancialInstitution updateWithCredentials(Long id, FinancialInstitution form, List<Long> selectedSystemTypeIds, List<CredentialLine> lines) {
        FinancialInstitution fi = idOrThrow(id);
        fi.setFiCode(form.getFiCode());
        fi.setBankName(form.getBankName());
        fi.setShortTitle(form.getShortTitle());
        fi.setSortOrder(form.getSortOrder());
        fi.setRowHighlight(form.getRowHighlight());
        FinancialInstitution saved = save(fi);

        // Add credentials for newly selected system types
        if (selectedSystemTypeIds != null && !selectedSystemTypeIds.isEmpty()) {
            for (Long systemTypeId : selectedSystemTypeIds) {
                SystemType systemType = systemTypeRepository.findById(systemTypeId)
                        .orElseThrow(() -> new IllegalArgumentException("System type not found: " + systemTypeId));
                if (credentialRepository.findByFinancialInstitutionAndSystemType(saved, systemType).isEmpty()) {
                    Credential c = new Credential();
                    c.setFinancialInstitution(saved);
                    c.setSystemType(systemType);
                    credentialRepository.save(c);
                }
            }
        }

        if (lines != null) {
            applyCredentialLines(saved, lines);
        }
        return saved;
    }

    /**
     * Empty lines for create form (active systems only).
     */
    public List<CredentialLine> blankCredentialLines() {
        List<CredentialLine> lines = new ArrayList<>();
        for (SystemType type : findActiveSystemTypes()) {
            lines.add(toLine(type, null));
        }
        return lines;
    }

    /**
     * Filled lines for edit form (active systems only).
     */
    public List<CredentialLine> credentialLinesForInstitution(Long fiId) {
        Map<Long, Credential> byType = findCredentials(fiId).stream()
                .collect(Collectors.toMap(c -> c.getSystemType().getId(), Function.identity(), (a, b) -> a));

        List<CredentialLine> lines = new ArrayList<>();
        for (SystemType type : findActiveSystemTypes()) {
            lines.add(toLine(type, byType.get(type.getId())));
        }
        return lines;
    }

    /**
     * Get all system types with selection status for checkboxes.
     */
    public List<SystemTypeCheckbox> systemTypeCheckboxes(Long fiId) {
        List<SystemType> activeTypes = findActiveSystemTypes();
        Set<Long> existingTypeIds = new HashSet<>();
        if (fiId != null) {
            existingTypeIds = findCredentials(fiId).stream()
                    .map(c -> c.getSystemType().getId())
                    .collect(Collectors.toSet());
        }

        List<SystemTypeCheckbox> checkboxes = new ArrayList<>();
        for (SystemType type : activeTypes) {
            SystemTypeCheckbox cb = new SystemTypeCheckbox();
            cb.setId(type.getId());
            cb.setCode(type.getCode().name());
            cb.setDisplayName(type.getDisplayName());
            cb.setSelected(existingTypeIds.contains(type.getId()));
            checkboxes.add(cb);
        }
        return checkboxes;
    }

    public static class SystemTypeCheckbox {
        private Long id;
        private String code;
        private String displayName;
        private boolean selected;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
    }

    private void applyCredentialLines(FinancialInstitution fi, List<CredentialLine> lines) {
        for (CredentialLine line : lines) {
            if (line.getSystemTypeId() == null) {
                continue;
            }
            createOrUpdateCredential(
                    fi.getId(),
                    line.getSystemTypeId(),
                    line.getUserId(),
                    line.getPassword(),
                    line.isVpn() ? line.getPreSharedKey() : null,
                    line.getNotes(),
                    LocalDate.now());
        }
    }

    private CredentialLine toLine(SystemType type, Credential credential) {
        CredentialLine line = new CredentialLine();
        line.setSystemTypeId(type.getId());
        line.setSystemCode(type.getCode().name());
        line.setDisplayName(type.getDisplayName());
        line.setVpn(type.getCode() == SystemTypeCode.VPN);
        if (credential != null) {
            line.setUserId(credential.getUserId());
            line.setPassword(credential.getPassword());
            line.setPreSharedKey(credential.getPreSharedKey());
            line.setNotes(credential.getNotes());
        }
        return line;
    }

    /**
     * Rule: every FI always has one credential row per system type
     * (VPN, PSS2, Inter-Bank, Mobile Wallet, Fraud, MIB).
     * Empty rows are OK until filled by Edit or Excel import.
     */
    @Transactional
    public int ensureMissingCredentials(Long fiId) {
        FinancialInstitution fi = idOrThrow(fiId);
        int added = 0;
        for (SystemType type : findAllSystemTypes()) {
            if (credentialRepository.findByFinancialInstitutionAndSystemType(fi, type).isEmpty()) {
                Credential c = new Credential();
                c.setFinancialInstitution(fi);
                c.setSystemType(type);
                credentialRepository.save(c);
                added++;
            }
        }
        return added;
    }

    private FinancialInstitution idOrThrow(Long id) {
        return fiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Institution not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
