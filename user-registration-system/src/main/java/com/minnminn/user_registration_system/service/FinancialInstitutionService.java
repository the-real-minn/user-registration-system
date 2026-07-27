package com.minnminn.user_registration_system.service;

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
import java.util.List;
import java.util.Optional;

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
        credential.setUpdateDate(updateDate != null ? updateDate : LocalDate.now());
        return credentialRepository.save(credential);
    }

    @Transactional
    public Credential generateAndSave(Long credentialId, String type) {
        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        switch (type) {
            case "psk" -> credential.setPreSharedKey(passwordGeneratorService.generatePsk());
            case "userId" -> {
                String suffix = credential.getSystemType().getIdSuffix();
                credential.setUserId(passwordGeneratorService.generateUserId(
                        credential.getFinancialInstitution().getFiCode(), suffix));
            }
            default -> credential.setPassword(passwordGeneratorService.generateUserPassword());
        }
        credential.setUpdateDate(LocalDate.now());
        return credentialRepository.save(credential);
    }

    @Transactional
    public FinancialInstitution createWithEmptyCredentials(FinancialInstitution fi) {
        if (fiRepository.findByFiCode(fi.getFiCode()).isPresent()) {
            throw new IllegalArgumentException("FI Code already exists: " + fi.getFiCode());
        }
        if (fi.getSortOrder() == null) {
            fi.setSortOrder((int) fiRepository.count() + 1);
        }
        FinancialInstitution saved = save(fi);
        for (SystemType type : findAllSystemTypes()) {
            Credential c = new Credential();
            c.setFinancialInstitution(saved);
            c.setSystemType(type);
            c.setUserId(passwordGeneratorService.generateUserId(saved.getFiCode(), type.getIdSuffix()));
            credentialRepository.save(c);
        }
        return saved;
    }

    private FinancialInstitution idOrThrow(Long id) {
        return fiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Institution not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
