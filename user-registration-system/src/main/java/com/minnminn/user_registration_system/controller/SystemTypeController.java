package com.minnminn.user_registration_system.controller;

import com.minnminn.user_registration_system.entity.SystemType;
import com.minnminn.user_registration_system.entity.SystemTypeCode;
import com.minnminn.user_registration_system.repository.SystemTypeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/fides/system-types")
public class SystemTypeController {

    private final SystemTypeRepository systemTypeRepository;

    public SystemTypeController(SystemTypeRepository systemTypeRepository) {
        this.systemTypeRepository = systemTypeRepository;
    }

    @GetMapping
    public String listSystemTypes(HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        model.addAttribute("systemTypes", systemTypeRepository.findAllByOrderBySortOrderAsc());
        return "fides/system-type-list";
    }

    @GetMapping("/new")
    public String newSystemType(HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        model.addAttribute("systemType", new SystemType());
        model.addAttribute("codes", SystemTypeCode.values());
        model.addAttribute("mode", "create");
        return "fides/system-type-form";
    }

    @PostMapping
    public String createSystemType(
            @ModelAttribute SystemType systemType,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            systemTypeRepository.save(systemType);
            redirectAttributes.addFlashAttribute("success", "System type created: " + systemType.getDisplayName());
            return "redirect:/fides/system-types";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fides/system-types/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editSystemType(@PathVariable Long id, HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        SystemType systemType = systemTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("System type not found"));
        model.addAttribute("systemType", systemType);
        model.addAttribute("codes", SystemTypeCode.values());
        model.addAttribute("mode", "edit");
        return "fides/system-type-form";
    }

    @PostMapping("/{id}")
    public String updateSystemType(
            @PathVariable Long id,
            @ModelAttribute SystemType form,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            SystemType systemType = systemTypeRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("System type not found"));
            systemType.setCode(form.getCode());
            systemType.setDisplayName(form.getDisplayName());
            systemType.setIdSuffix(form.getIdSuffix());
            systemType.setSortOrder(form.getSortOrder());
            systemType.setActive(form.isActive());
            systemTypeRepository.save(systemType);
            redirectAttributes.addFlashAttribute("success", "System type updated.");
            return "redirect:/fides/system-types";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fides/system-types/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteSystemType(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        systemTypeRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "System type deleted.");
        return "redirect:/fides/system-types";
    }

    @PostMapping("/{id}/toggle")
    public String toggleActive(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        SystemType systemType = systemTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("System type not found"));
        systemType.setActive(!systemType.isActive());
        systemTypeRepository.save(systemType);
        redirectAttributes.addFlashAttribute("success",
                "System type " + (systemType.isActive() ? "activated" : "deactivated") + ".");
        return "redirect:/fides/system-types";
    }

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loggedUser") != null;
    }
}