package com.minnminn.user_registration_system.controller;

import com.minnminn.user_registration_system.dto.BatchNoticeRequest;
import com.minnminn.user_registration_system.dto.CredentialLineListForm;
import com.minnminn.user_registration_system.dto.ResultNoticeForm;
import com.minnminn.user_registration_system.entity.*;
import com.minnminn.user_registration_system.service.ExcelImportService;
import com.minnminn.user_registration_system.service.FinancialInstitutionService;
import com.minnminn.user_registration_system.service.ResultNoticePdfService;
import com.minnminn.user_registration_system.service.ResultNoticeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/fides")
public class FidesController {

    private final FinancialInstitutionService fiService;
    private final ExcelImportService excelImportService;
    private final ResultNoticeService noticeService;
    private final ResultNoticePdfService pdfService;

    public FidesController(
            FinancialInstitutionService fiService,
            ExcelImportService excelImportService,
            ResultNoticeService noticeService,
            ResultNoticePdfService pdfService) {
        this.fiService = fiService;
        this.excelImportService = excelImportService;
        this.noticeService = noticeService;
        this.pdfService = pdfService;
    }

    // ---- Institutions ----

    @GetMapping("/institutions")
    public String listInstitutions(
            @RequestParam(required = false) String q,
            HttpSession session,
            Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        model.addAttribute("institutions", fiService.search(q));
        model.addAttribute("q", q == null ? "" : q);
        return "fides/institutions";
    }

    @GetMapping("/institutions/new")
    public String newInstitution(HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        model.addAttribute("institution", new FinancialInstitution());
        model.addAttribute("highlights", RowHighlight.values());
        model.addAttribute("credentialLines", fiService.blankCredentialLines());
        model.addAttribute("systemTypeCheckboxes", fiService.systemTypeCheckboxes(null));
        model.addAttribute("mode", "create");
        return "fides/institution-form";
    }

    @PostMapping("/institutions")
    public String createInstitution(
            @ModelAttribute FinancialInstitution institution,
            @RequestParam(value = "selectedSystemTypeIds", required = false) List<Long> selectedSystemTypeIds,
            @ModelAttribute CredentialLineListForm linesForm,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            FinancialInstitution saved = fiService.createWithCredentials(
                    institution, selectedSystemTypeIds, linesForm != null ? linesForm.getLines() : null);
            redirectAttributes.addFlashAttribute("success",
                    "Institution created with selected systems.");
            return "redirect:/fides/institutions/" + saved.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fides/institutions/new";
        }
    }

    @GetMapping("/institutions/{id}")
    public String institutionDetail(@PathVariable Long id, HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        FinancialInstitution fi = fiService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Institution not found"));
        fiService.ensureMissingCredentials(id);
        model.addAttribute("institution", fi);
        model.addAttribute("credentials", fiService.findCredentials(id));
        model.addAttribute("notices", noticeService.findByInstitution(id));
        return "fides/institution-detail";
    }

    @GetMapping("/institutions/{id}/edit")
    public String editInstitution(@PathVariable Long id, HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        FinancialInstitution fi = fiService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Institution not found"));
        model.addAttribute("institution", fi);
        model.addAttribute("highlights", RowHighlight.values());
        model.addAttribute("credentialLines", fiService.credentialLinesForInstitution(id));
        model.addAttribute("systemTypeCheckboxes", fiService.systemTypeCheckboxes(id));
        model.addAttribute("mode", "edit");
        return "fides/institution-form";
    }

    @PostMapping("/institutions/{id}")
    public String updateInstitution(
            @PathVariable Long id,
            @ModelAttribute FinancialInstitution form,
            @RequestParam(value = "selectedSystemTypeIds", required = false) List<Long> selectedSystemTypeIds,
            @ModelAttribute CredentialLineListForm linesForm,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            fiService.updateWithCredentials(id, form, selectedSystemTypeIds, linesForm != null ? linesForm.getLines() : null);
            redirectAttributes.addFlashAttribute("success", "Institution and system credentials updated.");
            return "redirect:/fides/institutions/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fides/institutions/" + id + "/edit";
        }
    }

    @PostMapping("/institutions/{id}/delete")
    public String deleteInstitution(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        fiService.deleteInstitution(id);
        redirectAttributes.addFlashAttribute("success", "Institution deleted.");
        return "redirect:/fides/institutions";
    }

    // ---- Credentials ----

    @GetMapping("/institutions/{fiId}/credentials/edit")
    public String editCredentialForm(
            @PathVariable Long fiId,
            @RequestParam(required = false) Long credentialId,
            @RequestParam(required = false) Long systemTypeId,
            HttpSession session,
            Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        FinancialInstitution fi = fiService.findById(fiId)
                .orElseThrow(() -> new IllegalArgumentException("Institution not found"));

        Credential credential;
        if (credentialId != null) {
            credential = fiService.findCredentialById(credentialId)
                    .orElseThrow(() -> new IllegalArgumentException("Credential not found"));
        } else {
            credential = new Credential();
            credential.setFinancialInstitution(fi);
            if (systemTypeId != null) {
                fiService.findAllSystemTypes().stream()
                        .filter(t -> t.getId().equals(systemTypeId))
                        .findFirst()
                        .ifPresent(credential::setSystemType);
            }
        }

        model.addAttribute("institution", fi);
        model.addAttribute("credential", credential);
        model.addAttribute("systemTypes", fiService.findAllSystemTypes());
        return "fides/credential-form";
    }

    @PostMapping("/institutions/{fiId}/credentials")
    public String saveCredential(
            @PathVariable Long fiId,
            @RequestParam Long systemTypeId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String preSharedKey,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) LocalDate updateDate,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        fiService.createOrUpdateCredential(fiId, systemTypeId, userId, password, preSharedKey, notes, updateDate);
        redirectAttributes.addFlashAttribute("success", "Credential saved.");
        return "redirect:/fides/institutions/" + fiId;
    }

    @PostMapping("/credentials/{id}/generate")
    public String generatePassword(
            @PathVariable Long id,
            @RequestParam Long institutionId,
            @RequestParam(defaultValue = "password") String type,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        fiService.generateAndSave(id, type);
        redirectAttributes.addFlashAttribute("success", "Generated new " + type + ".");
        return "redirect:/fides/institutions/" + institutionId;
    }

    // ---- Excel import ----

    @GetMapping("/import")
    public String importPage(HttpSession session) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        return "fides/import";
    }

    @PostMapping("/import")
    public String importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean replaceAll,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            ExcelImportService.ImportResult result;
            if (replaceAll) {
                java.nio.file.Path temp = java.nio.file.Files.createTempFile("fides-import-", ".xlsx");
                file.transferTo(temp);
                result = excelImportService.replaceAllFromFile(temp);
                java.nio.file.Files.deleteIfExists(temp);
            } else {
                result = excelImportService.importUserIdSheet(file);
            }
            redirectAttributes.addFlashAttribute("success", result.message());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/fides/import";
    }

    @PostMapping("/import/reload-bundled")
    public String reloadBundledExcel(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("data", "FIDES_ID_Management.xlsx");
            ExcelImportService.ImportResult result = excelImportService.replaceAllFromFile(path);
            redirectAttributes.addFlashAttribute("success", result.message());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/fides/import";
    }

    // ---- Result Notice (report) ----

    @GetMapping("/notices")
    public String noticeList(HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        model.addAttribute("notices", noticeService.findRecent(100));
        return "fides/notice-list";
    }

    @GetMapping("/notices/new")
    public String noticeForm(HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        model.addAttribute("institutions", fiService.findAll());
        model.addAttribute("vpnStatuses", ResultNoticeService.VPN_STATUSES);
        model.addAttribute("fidesStatuses", ResultNoticeService.FIDES_LOGIN_STATUSES);
        model.addAttribute("categories", ResultNoticeService.USER_CATEGORIES);
        model.addAttribute("request", new BatchNoticeRequest());
        return "fides/notice-form";
    }

    @PostMapping("/notices/batch")
    public String generateBatch(
            @ModelAttribute BatchNoticeRequest request,
            @RequestParam(value = "institutionIds", required = false) List<Long> institutionIds,
            @RequestParam(value = "selectAll", defaultValue = "false") String selectAllRaw,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            boolean selectAll = "true".equalsIgnoreCase(selectAllRaw) || "on".equalsIgnoreCase(selectAllRaw);
            request.setSelectAll(selectAll);
            if (institutionIds != null) {
                request.setInstitutionIds(institutionIds);
            }
            List<ResultNotice> notices = noticeService.saveBatch(request);
            List<Long> ids = notices.stream().map(ResultNotice::getId).toList();
            session.setAttribute("batchNoticeIds", ids);
            redirectAttributes.addFlashAttribute("success",
                    "Generated " + notices.size() + " Result Notice report(s).");
            return "redirect:/fides/notices/batch";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fides/notices/new";
        }
    }

    @GetMapping("/notices/batch")
    public String viewBatch(HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) session.getAttribute("batchNoticeIds");
        if (ids == null || ids.isEmpty()) {
            return "redirect:/fides/notices/new";
        }
        model.addAttribute("notices", noticeService.findByIds(ids));
        return "fides/notice-batch";
    }

    /** Browser PDF preview for one notice (one BIC). */
    @GetMapping("/notices/{id}/pdf")
    public ResponseEntity<byte[]> previewPdf(@PathVariable Long id, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/login").build();
        }
        ResultNotice notice = noticeService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found"));
        byte[] pdf = pdfService.buildPdfBytes(notice);
        String bic = notice.getFinancialInstitution().getFiCode();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + bic + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Download ZIP: one PDF file per BIC Code inside the zip.
     */
    @PostMapping("/notices/batch/export-pdf")
    public ResponseEntity<?> exportPdfZip(HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/login").build();
        }
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) session.getAttribute("batchNoticeIds");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/fides/notices/new").build();
        }
        List<ResultNotice> notices = noticeService.findByIds(ids);
        ResultNoticePdfService.PdfExportResult result = pdfService.buildZip(notices);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.zipFileName() + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(result.zipBytes().length)
                .body(result.zipBytes());
    }

    @GetMapping("/notices/batch/print")
    public String printBatch(HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) session.getAttribute("batchNoticeIds");
        if (ids == null || ids.isEmpty()) {
            return "redirect:/fides/notices/new";
        }
        model.addAttribute("notices", noticeService.findByIds(ids));
        return "fides/notice-batch-print";
    }

    @PostMapping("/notices")
    public String saveNotice(
            @ModelAttribute ResultNoticeForm form,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            ResultNotice notice = noticeService.saveNotice(form);
            redirectAttributes.addFlashAttribute("success", "Result Notice saved.");
            return "redirect:/fides/notices/" + notice.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fides/notices/new";
        }
    }

    @GetMapping("/notices/{id}")
    public String viewNotice(@PathVariable Long id, HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        ResultNotice notice = noticeService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found"));
        model.addAttribute("notice", notice);
        return "fides/notice-view";
    }

    @GetMapping("/notices/{id}/print")
    public String printNotice(@PathVariable Long id, HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        ResultNotice notice = noticeService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found"));
        model.addAttribute("notice", notice);
        return "fides/notice-print";
    }

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loggedUser") instanceof User;
    }
}
