package com.minnminn.user_registration_system.service;

import com.minnminn.user_registration_system.entity.*;
import com.minnminn.user_registration_system.repository.CredentialRepository;
import com.minnminn.user_registration_system.repository.FinancialInstitutionRepository;
import com.minnminn.user_registration_system.repository.ResultNoticeRepository;
import com.minnminn.user_registration_system.repository.SystemTypeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Imports the "User ID" sheet from FIDES ID Management Excel into normalized tables.
 *
 * Column layout (0-based):
 * 0 Sr | 1 FI Code | 2 Bank Name | 3 Short Title
 * 4-7 VPN (userId, password, psk, updateDate)
 * 8-10 PSS2 | 11-13 Inter-Bank | 14-16 Mobile Wallet
 * 17-19 Bank Fraud | 20-22 MIB | 23 Random Password (ignored)
 */
@Service
public class ExcelImportService {

    private static final String SHEET_NAME = "User ID";
    /** Excel row 5 = first bank (0-based index 4). Rows 1-4 are titles/headers. */
    private static final int DATA_START_ROW = 4;

    private final FinancialInstitutionRepository fiRepository;
    private final CredentialRepository credentialRepository;
    private final SystemTypeRepository systemTypeRepository;
    private final ResultNoticeRepository noticeRepository;

    public ExcelImportService(
            FinancialInstitutionRepository fiRepository,
            CredentialRepository credentialRepository,
            SystemTypeRepository systemTypeRepository,
            ResultNoticeRepository noticeRepository) {
        this.fiRepository = fiRepository;
        this.credentialRepository = credentialRepository;
        this.systemTypeRepository = systemTypeRepository;
        this.noticeRepository = noticeRepository;
    }

    public record ImportResult(int institutions, int credentials, String message) {}

    @Transactional
    public ImportResult importUserIdSheet(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select an Excel file (.xlsx)");
        }
        try (InputStream in = file.getInputStream()) {
            return importUserIdSheet(in);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Clears FI/credential/notice data, then loads all banks from the Excel file.
     */
    @Transactional
    public ImportResult replaceAllFromFile(java.nio.file.Path path) {
        if (path == null || !java.nio.file.Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Excel file not found: " + path);
        }
        noticeRepository.deleteAll();
        credentialRepository.deleteAll();
        fiRepository.deleteAll();
        try (InputStream in = java.nio.file.Files.newInputStream(path)) {
            ImportResult result = importUserIdSheet(in);
            return new ImportResult(result.institutions(), result.credentials(),
                    "Replaced database from Excel. Institutions: " + result.institutions()
                            + ", credentials: " + result.credentials());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import Excel: " + e.getMessage(), e);
        }
    }

    @Transactional
    public ImportResult importUserIdSheet(InputStream in) {
        Map<SystemTypeCode, SystemType> types = loadSystemTypes();
        int fiCount = 0;
        int credCount = 0;

        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (int r = DATA_START_ROW; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                String fiCode = cellAsString(row.getCell(1), formatter, evaluator);
                String bankName = cellAsString(row.getCell(2), formatter, evaluator);
                if (fiCode.isBlank() || bankName.isBlank()) {
                    continue;
                }
                // Skip header-like rows
                if ("FI Code".equalsIgnoreCase(fiCode) || "Bank Name".equalsIgnoreCase(bankName)
                        || "Sr".equalsIgnoreCase(fiCode)) {
                    continue;
                }

                FinancialInstitution fi = fiRepository.findByFiCode(fiCode).orElseGet(FinancialInstitution::new);
                boolean isNew = fi.getId() == null;
                fi.setFiCode(fiCode.trim());
                fi.setBankName(bankName.trim());
                fi.setShortTitle(blankToNull(cellAsString(row.getCell(3), formatter, evaluator)));
                fi.setSortOrder(parseSortOrder(row.getCell(0), formatter, evaluator, r));
                fi.setRowHighlight(detectHighlight(row, workbook));
                fiRepository.save(fi);
                if (isNew) {
                    fiCount++;
                }

                credCount += upsertGroup(fi, types.get(SystemTypeCode.VPN),
                        row, 4, 5, 6, 7, true, formatter, evaluator);
                credCount += upsertGroup(fi, types.get(SystemTypeCode.PSS2),
                        row, 8, 9, -1, 10, false, formatter, evaluator);
                credCount += upsertGroup(fi, types.get(SystemTypeCode.INTER_BANK),
                        row, 11, 12, -1, 13, false, formatter, evaluator);
                credCount += upsertGroup(fi, types.get(SystemTypeCode.MOBILE_WALLET),
                        row, 14, 15, -1, 16, false, formatter, evaluator);
                credCount += upsertGroup(fi, types.get(SystemTypeCode.BANK_FRAUD),
                        row, 17, 18, -1, 19, false, formatter, evaluator);
                credCount += upsertGroup(fi, types.get(SystemTypeCode.MIB),
                        row, 20, 21, -1, 22, false, formatter, evaluator);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import Excel: " + e.getMessage(), e);
        }

        return new ImportResult(fiCount, credCount,
                "Import finished. Institutions: " + fiCount + ", credentials upserted: " + credCount);
    }

    private int upsertGroup(
            FinancialInstitution fi,
            SystemType systemType,
            Row row,
            int userIdCol,
            int passwordCol,
            int pskCol,
            int dateCol,
            boolean isVpn,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {

        String userId = cellAsString(row.getCell(userIdCol), formatter, evaluator);
        String password = cellAsString(row.getCell(passwordCol), formatter, evaluator);
        String psk = isVpn && pskCol >= 0
                ? cellAsString(row.getCell(pskCol), formatter, evaluator)
                : null;
        LocalDate updateDate = cellAsDate(row.getCell(dateCol), formatter, evaluator);

        if (userId.isBlank() && password.isBlank() && (psk == null || psk.isBlank())) {
            return 0;
        }

        Credential credential = credentialRepository
                .findByFinancialInstitutionAndSystemType(fi, systemType)
                .orElseGet(Credential::new);
        credential.setFinancialInstitution(fi);
        credential.setSystemType(systemType);
        credential.setUserId(blankToNull(userId));
        credential.setPassword(blankToNull(password));
        if (isVpn) {
            credential.setPreSharedKey(blankToNull(psk));
        }
        credential.setUpdateDate(updateDate);
        credentialRepository.save(credential);
        return 1;
    }

    private Map<SystemTypeCode, SystemType> loadSystemTypes() {
        Map<SystemTypeCode, SystemType> map = new HashMap<>();
        for (SystemTypeCode code : SystemTypeCode.values()) {
            SystemType type = systemTypeRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalStateException("Missing system type: " + code));
            map.put(code, type);
        }
        return map;
    }

    private RowHighlight detectHighlight(Row row, Workbook workbook) {
        Cell cell = row.getCell(2);
        if (cell == null) {
            return RowHighlight.NORMAL;
        }
        CellStyle style = cell.getCellStyle();
        if (style == null) {
            return RowHighlight.NORMAL;
        }

        // XSSF path
        if (workbook instanceof XSSFWorkbook) {
            try {
                XSSFColor color = (XSSFColor) style.getFillForegroundColorColor();
                if (color != null && color.getRGB() != null) {
                    byte[] rgb = color.getRGB();
                    int r = rgb[0] & 0xFF;
                    int g = rgb[1] & 0xFF;
                    int b = rgb[2] & 0xFF;
                    if (isReddish(r, g, b)) {
                        return RowHighlight.HIGHLIGHT_RED;
                    }
                    if (isYellowish(r, g, b)) {
                        return RowHighlight.HIGHLIGHT_YELLOW;
                    }
                }
            } catch (Exception ignored) {
                // fall through to indexed colors
            }
        }

        short indexed = style.getFillForegroundColor();
        // This workbook uses indexed 2 = red row, indexed 5 = yellow row
        if (indexed == 2
                || indexed == IndexedColors.RED.getIndex()
                || indexed == IndexedColors.ROSE.getIndex()
                || indexed == IndexedColors.CORAL.getIndex()) {
            return RowHighlight.HIGHLIGHT_RED;
        }
        if (indexed == 5
                || indexed == IndexedColors.YELLOW.getIndex()
                || indexed == IndexedColors.LIGHT_YELLOW.getIndex()
                || indexed == IndexedColors.GOLD.getIndex()) {
            return RowHighlight.HIGHLIGHT_YELLOW;
        }
        return RowHighlight.NORMAL;
    }

    private boolean isReddish(int r, int g, int b) {
        return r > 180 && g < 140 && b < 140;
    }

    private boolean isYellowish(int r, int g, int b) {
        return r > 200 && g > 180 && b < 120;
    }

    private Integer parseSortOrder(
            Cell cell, DataFormatter formatter, FormulaEvaluator evaluator, int fallbackRow) {
        if (cell == null) {
            return fallbackRow;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String text = cellAsString(cell, formatter, evaluator);
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallbackRow;
        }
    }

    private String cellAsString(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private LocalDate cellAsDate(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        try {
            CellType type = cell.getCellType();
            if (type == CellType.FORMULA) {
                type = evaluator.evaluateFormulaCell(cell);
            }
            if (type == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            if (type == CellType.NUMERIC) {
                double n = cell.getNumericCellValue();
                // Excel serial date numbers are typically < 100000
                if (n > 20000 && n < 100000) {
                    return DateUtil.getJavaDate(n).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
                return null;
            }
            String text = cellAsString(cell, formatter, evaluator);
            if (text.isBlank()) {
                return null;
            }
            if (text.contains("/")) {
                String[] parts = text.split("/");
                if (parts.length == 3) {
                    int a = Integer.parseInt(parts[0].trim());
                    int b = Integer.parseInt(parts[1].trim());
                    int y = Integer.parseInt(parts[2].trim());
                    if (y < 100) {
                        y += 2000;
                    }
                    // Workbook uses day/month/year
                    return LocalDate.of(y, b, a);
                }
            }
            return LocalDate.parse(text);
        } catch (Exception e) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
