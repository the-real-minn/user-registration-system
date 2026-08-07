package com.minnminn.user_registration_system.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.minnminn.user_registration_system.entity.ResultNotice;
import com.minnminn.user_registration_system.util.NoticeDisplayRules;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Creates one Result Notice PDF per BIC Code, packaged as a ZIP download.
 * Layout matches the Excel Result Notice sheet.
 */
@Service
public class ResultNoticePdfService {

    private static final Color LABEL_GRAY = new Color(230, 230, 230);
    private static final Color TITLE_BG = new Color(0, 0, 0);
    private static final DateTimeFormatter NOTICE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record PdfExportResult(String zipFileName, int pdfCount, byte[] zipBytes) {}

    public byte[] buildPdfBytes(ResultNotice notice) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writePdf(notice, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to build PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Builds a ZIP in memory: one PDF per BIC Code.
     * Example: ResultNotice_20260731_111800.zip containing CBMYMMMY.pdf, MYEBMMMY.pdf, ...
     */
    public PdfExportResult buildZip(List<ResultNotice> notices) {
        if (notices == null || notices.isEmpty()) {
            throw new IllegalArgumentException("No notices to export");
        }

        String zipName = "ResultNotice_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".zip";

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            for (ResultNotice notice : notices) {
                String bic = safeFileName(notice.getFinancialInstitution().getFiCode());
                zos.putNextEntry(new ZipEntry(bic + ".pdf"));
                zos.write(buildPdfBytes(notice));
                zos.closeEntry();
            }
            zos.finish();
            return new PdfExportResult(zipName, notices.size(), bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create ZIP: " + e.getMessage(), e);
        }
    }

    private String safeFileName(String bic) {
        if (bic == null || bic.isBlank()) {
            return "UNKNOWN";
        }
        return bic.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private void writePdf(ResultNotice notice, OutputStream out) {
        Document document = new Document(com.lowagie.text.PageSize.A4, 50, 50, 40, 40);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.WHITE);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font refFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

            // Title bar (vertically centered text)
            PdfPTable title = new PdfPTable(1);
            title.setWidthPercentage(100);
            PdfPCell titleCell = new PdfPCell(new Phrase("CBM-FIDE VPN ID & Login User ID Notice", titleFont));
            titleCell.setBackgroundColor(TITLE_BG);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            titleCell.setFixedHeight(36f);
            titleCell.setPaddingTop(0);
            titleCell.setPaddingBottom(0);
            titleCell.setUseAscender(true);
            titleCell.setUseDescender(true);
            titleCell.setBorder(Rectangle.NO_BORDER);
            title.addCell(titleCell);
            document.add(title);
            document.add(spacer(8));

            // Date (right)
            Paragraph date = new Paragraph(formatDate(notice.getNoticeDate()), normalFont);
            date.setAlignment(Element.ALIGN_RIGHT);
            date.setSpacingAfter(14);
            document.add(date);

            // BIC / FI / VPN / FIDES — values share the same start column
            document.add(alignedField("BIC Code:", nullSafe(notice.getFinancialInstitution().getFiCode()),
                    boldFont, normalFont));
            document.add(alignedField("FI Name:", nullSafe(notice.getFinancialInstitution().getBankName()),
                    boldFont, normalFont));
            document.add(spacer(4));

            // Separator under bank info
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            line.setSpacingAfter(14);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorder(Rectangle.BOTTOM);
            lineCell.setBorderWidthBottom(1f);
            lineCell.setBorderColor(Color.BLACK);
            lineCell.setFixedHeight(2f);
            lineCell.setPadding(0);
            line.addCell(lineCell);
            document.add(line);

            // VPN section
            document.add(alignedField("VPN:", nullSafe(notice.getVpnStatus()), boldFont, normalFont));
            document.add(credentialTable(List.of(
                    new String[]{"ID", NoticeDisplayRules.vpnUserId(notice)},
                    new String[]{"Password", NoticeDisplayRules.vpnPassword(notice)},
                    new String[]{"Pre-Shared Key", NoticeDisplayRules.vpnPsk(notice)}
            ), boldFont, normalFont));
            document.add(spacer(14));

            // FIDES Login section
            document.add(alignedField("FIDES Login:", nullSafe(notice.getFidesLoginStatus()),
                    boldFont, normalFont));
            document.add(credentialTable(List.of(
                    new String[]{"User Category", nullSafe(notice.getUserCategory())},
                    new String[]{"Login ID", NoticeDisplayRules.loginUserId(notice)},
                    new String[]{"Password", NoticeDisplayRules.loginPassword(notice)}
            ), boldFont, normalFont));
            document.add(spacer(18));

            // Password Reference
            Paragraph refLabel = new Paragraph("Password Reference:", boldFont);
            refLabel.setSpacingAfter(4);
            document.add(refLabel);
            document.add(new Paragraph(PasswordGeneratorService.PASSWORD_REFERENCE, refFont));
        } catch (Exception e) {
            throw new RuntimeException("PDF write failed: " + e.getMessage(), e);
        } finally {
            document.close();
        }
    }

    /**
     * Label + value in a fixed two-column row so all values start at the same X position.
     * Label width fits the longest label: "FIDES Login:".
     */
    private PdfPTable alignedField(String label, String value, Font labelFont, Font valueFont) {
        PdfPTable table = new PdfPTable(new float[]{22, 78});
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(2);
        labelCell.setPaddingRight(6);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell valueCell = new PdfPCell(new Phrase(nullSafe(value), valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(2);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(labelCell);
        table.addCell(valueCell);
        return table;
    }

    private PdfPTable credentialTable(List<String[]> rows, Font labelFont, Font valueFont) {
        PdfPTable table = new PdfPTable(new float[]{35, 65});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(4);
        for (String[] row : rows) {
            table.addCell(labelCell(row[0], labelFont));
            table.addCell(valueCell(row[1], valueFont));
        }
        return table;
    }

    private PdfPCell labelCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(nullSafe(text), font));
        cell.setPadding(8);
        cell.setBackgroundColor(LABEL_GRAY);
        cell.setBorderColor(Color.BLACK);
        cell.setBorderWidth(0.6f);
        return cell;
    }

    private PdfPCell valueCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(nullSafe(text), font));
        cell.setPadding(8);
        cell.setBorderColor(Color.BLACK);
        cell.setBorderWidth(0.6f);
        return cell;
    }

    private Paragraph spacer(float size) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(size);
        p.setSpacingAfter(0);
        return p;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(NOTICE_DATE);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
