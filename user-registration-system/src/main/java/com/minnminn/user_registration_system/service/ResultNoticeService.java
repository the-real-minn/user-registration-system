package com.minnminn.user_registration_system.service;

import com.minnminn.user_registration_system.dto.BatchNoticeRequest;
import com.minnminn.user_registration_system.dto.ResultNoticeForm;
import com.minnminn.user_registration_system.entity.*;
import com.minnminn.user_registration_system.repository.CredentialRepository;
import com.minnminn.user_registration_system.repository.FinancialInstitutionRepository;
import com.minnminn.user_registration_system.repository.ResultNoticeRepository;
import com.minnminn.user_registration_system.repository.SystemTypeRepository;
import com.minnminn.user_registration_system.util.NoticeDisplayRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ResultNoticeService {

    public static final List<String> VPN_STATUSES = List.of(
            "Updated", "Pre-Shared Key Change", "No Change");

    public static final List<String> FIDES_LOGIN_STATUSES = List.of(
            "Updated", "No Change");

    public static final List<String> USER_CATEGORIES = List.of(
            "N/A",
            "CBM-NET related",
            "Inter-Bank Reporting",
            "Mobile Wallet",
            "Bank Account and Fraud Report",
            "Mobile and Internet Banking");

    private final ResultNoticeRepository noticeRepository;
    private final FinancialInstitutionRepository fiRepository;
    private final CredentialRepository credentialRepository;
    private final SystemTypeRepository systemTypeRepository;
    private final PasswordGeneratorService passwordGeneratorService;

    public ResultNoticeService(
            ResultNoticeRepository noticeRepository,
            FinancialInstitutionRepository fiRepository,
            CredentialRepository credentialRepository,
            SystemTypeRepository systemTypeRepository,
            PasswordGeneratorService passwordGeneratorService) {
        this.noticeRepository = noticeRepository;
        this.fiRepository = fiRepository;
        this.credentialRepository = credentialRepository;
        this.systemTypeRepository = systemTypeRepository;
        this.passwordGeneratorService = passwordGeneratorService;
    }

    public ResultNoticeForm loadForm(Long fiId, String userCategory) {
        FinancialInstitution fi = fiRepository.findById(fiId)
                .orElseThrow(() -> new IllegalArgumentException("Institution not found"));

        ResultNoticeForm form = new ResultNoticeForm();
        form.setFinancialInstitutionId(fi.getId());
        form.setNoticeDate(LocalDate.now());
        form.setUserCategory(userCategory == null || userCategory.isBlank()
                ? "Mobile and Internet Banking"
                : userCategory);
        fillCredentials(form, fi);
        return form;
    }

    /**
     * Builds preview notices (not saved) for selected banks — same layout as Excel.
     */
    public List<ResultNotice> previewBatch(BatchNoticeRequest request) {
        List<FinancialInstitution> banks = resolveBanks(request);
        List<ResultNotice> previews = new ArrayList<>();
        for (FinancialInstitution fi : banks) {
            ResultNoticeForm form = buildFormForBank(fi, request);
            previews.add(toNoticeEntity(fi, form, false));
        }
        return previews;
    }

    /**
     * Saves one Result Notice per selected bank (1, many, or all 59).
     */
    @Transactional
    public List<ResultNotice> saveBatch(BatchNoticeRequest request) {
        List<FinancialInstitution> banks = resolveBanks(request);
        if (banks.isEmpty()) {
            throw new IllegalArgumentException("Select at least one bank");
        }

        List<ResultNotice> saved = new ArrayList<>();
        for (FinancialInstitution fi : banks) {
            ResultNoticeForm form = buildFormForBank(fi, request);
            saved.add(saveNotice(form));
        }
        return saved;
    }

    private List<FinancialInstitution> resolveBanks(BatchNoticeRequest request) {
        if (request.isSelectAll()) {
            return fiRepository.findAllByOrderBySortOrderAscBankNameAsc();
        }
        List<Long> ids = request.getInstitutionIds();
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        // Keep Excel sort order
        List<FinancialInstitution> all = fiRepository.findAllByOrderBySortOrderAscBankNameAsc();
        java.util.Set<Long> selected = new java.util.HashSet<>(ids);
        return all.stream().filter(fi -> selected.contains(fi.getId())).toList();
    }

    private ResultNoticeForm buildFormForBank(FinancialInstitution fi, BatchNoticeRequest request) {
        ResultNoticeForm form = new ResultNoticeForm();
        form.setFinancialInstitutionId(fi.getId());
        form.setNoticeDate(request.getNoticeDate() != null ? request.getNoticeDate() : LocalDate.now());
        form.setVpnStatus(request.getVpnStatus());
        form.setFidesLoginStatus(request.getFidesLoginStatus());
        form.setUserCategory(request.getUserCategory());
        form.setRegenerateVpnPassword(request.isRegenerateVpnPassword());
        form.setRegenerateVpnPsk(request.isRegenerateVpnPsk());
        form.setRegenerateLoginPassword(request.isRegenerateLoginPassword());
        fillCredentials(form, fi);
        return form;
    }

    private void fillCredentials(ResultNoticeForm form, FinancialInstitution fi) {
        SystemType vpnType = systemTypeRepository.findByCode(SystemTypeCode.VPN).orElseThrow();
        credentialRepository.findByFinancialInstitutionAndSystemType(fi, vpnType).ifPresent(c -> {
            form.setVpnUserId(c.getUserId());
            form.setVpnPassword(c.getPassword());
            form.setVpnPsk(c.getPreSharedKey());
        });

        SystemTypeCode categoryCode = form.resolveCategoryCode();
        if (categoryCode != null) {
            SystemType loginType = systemTypeRepository.findByCode(categoryCode).orElseThrow();
            Optional<Credential> login = credentialRepository.findByFinancialInstitutionAndSystemType(fi, loginType);
            if (login.isPresent()) {
                form.setLoginUserId(login.get().getUserId() != null ? login.get().getUserId() : "0");
                form.setLoginPassword(login.get().getPassword());
            } else {
                form.setLoginUserId("0");
                form.setLoginPassword("");
            }
        } else {
            form.setLoginUserId("N/A");
            form.setLoginPassword("");
        }
    }

    private ResultNotice toNoticeEntity(FinancialInstitution fi, ResultNoticeForm form, boolean applyRegen) {
        if (applyRegen) {
            if (form.isRegenerateVpnPassword()) {
                form.setVpnPassword(passwordGeneratorService.generateVpnPassword());
            }
            if (form.isRegenerateVpnPsk()) {
                form.setVpnPsk(passwordGeneratorService.generatePsk());
            }
            if (form.isRegenerateLoginPassword() && form.resolveCategoryCode() != null) {
                form.setLoginPassword(passwordGeneratorService.generateUserPassword());
            }
        }

        ResultNotice notice = new ResultNotice();
        notice.setFinancialInstitution(fi);
        notice.setNoticeDate(form.getNoticeDate() != null ? form.getNoticeDate() : LocalDate.now());
        notice.setVpnStatus(form.getVpnStatus());
        notice.setFidesLoginStatus(form.getFidesLoginStatus());
        notice.setUserCategory(form.getUserCategory());
        notice.setVpnUserId(form.getVpnUserId());
        notice.setVpnPassword(form.getVpnPassword());
        notice.setVpnPsk(form.getVpnPsk());
        notice.setLoginUserId(form.getLoginUserId());
        notice.setLoginPassword(form.getLoginPassword());
        notice.setCreatedAt(Instant.now());
        return notice;
    }

    /**
     * Applies optional password regeneration, updates live credentials when status is Updated,
     * and saves a notice snapshot.
     */
    @Transactional
    public ResultNotice saveNotice(ResultNoticeForm form) {
        FinancialInstitution fi = fiRepository.findById(form.getFinancialInstitutionId())
                .orElseThrow(() -> new IllegalArgumentException("Institution not found"));

        SystemType vpnType = systemTypeRepository.findByCode(SystemTypeCode.VPN).orElseThrow();
        Credential vpn = credentialRepository.findByFinancialInstitutionAndSystemType(fi, vpnType)
                .orElseGet(() -> {
                    Credential c = new Credential();
                    c.setFinancialInstitution(fi);
                    c.setSystemType(vpnType);
                    return c;
                });

        if (form.isRegenerateVpnPassword()) {
            form.setVpnPassword(passwordGeneratorService.generateVpnPassword());
        }
        if (form.isRegenerateVpnPsk()) {
            form.setVpnPsk(passwordGeneratorService.generatePsk());
        }

        String vpnStatus = form.getVpnStatus();
        if (NoticeDisplayRules.isVpnNoChange(vpnStatus)) {
            form.setVpnUserId(NoticeDisplayRules.NA);
            form.setVpnPassword(NoticeDisplayRules.NA);
            form.setVpnPsk(NoticeDisplayRules.NA);
        } else if (NoticeDisplayRules.isVpnPskChange(vpnStatus)) {
            // Only PSK changes in live credentials; ID/Password show N/A on notice
            vpn.setPreSharedKey(form.getVpnPsk());
            vpn.setUpdateDate(LocalDate.now());
            credentialRepository.save(vpn);
            form.setVpnUserId(NoticeDisplayRules.NA);
            form.setVpnPassword(NoticeDisplayRules.NA);
        } else {
            // Updated
            vpn.setUserId(form.getVpnUserId());
            vpn.setPassword(form.getVpnPassword());
            vpn.setPreSharedKey(form.getVpnPsk());
            vpn.setUpdateDate(LocalDate.now());
            credentialRepository.save(vpn);
        }

        SystemTypeCode categoryCode = form.resolveCategoryCode();
        if (categoryCode != null) {
            SystemType loginType = systemTypeRepository.findByCode(categoryCode).orElseThrow();
            Credential login = credentialRepository.findByFinancialInstitutionAndSystemType(fi, loginType)
                    .orElseGet(() -> {
                        Credential c = new Credential();
                        c.setFinancialInstitution(fi);
                        c.setSystemType(loginType);
                        return c;
                    });

            if (form.isRegenerateLoginPassword()) {
                form.setLoginPassword(passwordGeneratorService.generateUserPassword());
            }

            if (NoticeDisplayRules.isFidesNoChange(form.getFidesLoginStatus())) {
                form.setLoginUserId(NoticeDisplayRules.NA);
                form.setLoginPassword(NoticeDisplayRules.NA);
            } else {
                login.setUserId(form.getLoginUserId());
                login.setPassword(form.getLoginPassword());
                login.setUpdateDate(LocalDate.now());
                credentialRepository.save(login);
            }
        } else if (NoticeDisplayRules.isFidesNoChange(form.getFidesLoginStatus())) {
            form.setLoginUserId(NoticeDisplayRules.NA);
            form.setLoginPassword(NoticeDisplayRules.NA);
        }

        ResultNotice notice = toNoticeEntity(fi, form, false);
        return noticeRepository.save(notice);
    }

    public Optional<ResultNotice> findById(Long id) {
        return noticeRepository.findById(id);
    }

    public List<ResultNotice> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ResultNotice> found = noticeRepository.findAllById(ids);
        found.sort((a, b) -> {
            Integer oa = a.getFinancialInstitution().getSortOrder();
            Integer ob = b.getFinancialInstitution().getSortOrder();
            return Integer.compare(oa == null ? 0 : oa, ob == null ? 0 : ob);
        });
        return found;
    }

    public List<ResultNotice> findRecent(int limit) {
        List<ResultNotice> all = noticeRepository.findAll();
        all.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return all.size() <= limit ? all : all.subList(0, limit);
    }

    public List<ResultNotice> findByInstitution(Long fiId) {
        return noticeRepository.findByFinancialInstitutionIdOrderByCreatedAtDesc(fiId);
    }
}
