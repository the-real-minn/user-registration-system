package com.minnminn.user_registration_system.config;

import com.minnminn.user_registration_system.repository.FinancialInstitutionRepository;
import com.minnminn.user_registration_system.service.ExcelImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads all banks from data/FIDES_ID_Management.xlsx when the DB has no institutions,
 * or when started with --fides.import=true.
 */
@Component
@Order(2)
public class ExcelDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExcelDataLoader.class);

    private final ExcelImportService excelImportService;
    private final FinancialInstitutionRepository fiRepository;

    public ExcelDataLoader(
            ExcelImportService excelImportService,
            FinancialInstitutionRepository fiRepository) {
        this.excelImportService = excelImportService;
        this.fiRepository = fiRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean force = args.containsOption("fides.import")
                && args.getOptionValues("fides.import") != null
                && args.getOptionValues("fides.import").stream().anyMatch("true"::equalsIgnoreCase);

        Path excel = resolveExcelPath();
        if (excel == null) {
            log.info("No FIDES Excel file found under ./data — skip auto-import.");
            return;
        }

        long count = fiRepository.count();
        if (!force && count > 0) {
            log.info("Institutions already present ({}) — skip auto-import. Use --fides.import=true to reload.", count);
            return;
        }

        log.info("Importing FIDES Excel from {}", excel.toAbsolutePath());
        ExcelImportService.ImportResult result = excelImportService.replaceAllFromFile(excel);
        log.info(result.message());
    }

    private Path resolveExcelPath() {
        Path[] candidates = new Path[] {
                Paths.get("data", "FIDES_ID_Management.xlsx"),
                Paths.get("user-registration-system", "data", "FIDES_ID_Management.xlsx")
        };
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                return p.toAbsolutePath().normalize();
            }
        }
        return null;
    }
}
