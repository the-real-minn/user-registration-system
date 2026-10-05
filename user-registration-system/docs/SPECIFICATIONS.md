# FIDES ID Management — Specifications

## Purpose

Replace the Excel workbook **FIDES ID Management** with a Spring Boot web application.

## Modules

| Module | Excel equivalent | Routes |
|--------|------------------|--------|
| Institutions | User ID sheet | `/fides/institutions` |
| Credentials | Column groups VPN…MIB | `/fides/institutions/{id}` |
| Excel import | Manual copy | `/fides/import` |
| Result Notice | Result Notice sheet | `/fides/notices/new` |
| Notice history | Saved notices | `/fides/notices` |
| Password tools | Random Password / PSK | Generate Password / PSK buttons + notice checkboxes |

## FI + systems rule (simple)

**Every bank always has exactly 6 credential rows** (same as Excel User ID columns):

1. VPN  
2. CBM-NET related (PSS2)  
3. Inter-Bank Reporting  
4. Mobile Wallet  
5. Bank Account and Fraud Report  
6. Mobile and Internet Banking  

| Action | What happens |
|--------|----------------|
| **Add FI** | Creates bank + all 6 empty rows |
| **Excel import** | Fills rows that have Excel data; still keeps all 6 rows |
| **Open FI detail** | Auto-adds any missing system row |
| **Edit credential** | Fill User ID / Password / PSK for that system |
| **Result Notice** | VPN always; User Category picks one login system (3–6) |

Empty User ID / Password is OK. Fill only when needed.

## Password rules

- Length: 8 (default)
- Charset: `A–Z a–z 0–9` + `!@#$%^&*()-_+[]{}|`
- User ID: set by Excel import or Edit only (no generate button)

## Result Notice rules

1. Select BIC → load FI name + VPN credentials
2. Select User Category → load matching login credentials
3. VPN status: Updated | Pre-Shared Key Change | No Change
4. FIDES Login status: Updated | No Change
5. Optional regenerate password/PSK on save
6. If status is not `No Change`, live credentials are updated
7. Always save a notice snapshot for print/history

## Category → system mapping

| Category | SystemTypeCode |
|----------|----------------|
| N/A | (none) |
| CBM-NET related | PSS2 |
| Inter-Bank Reporting | INTER_BANK |
| Mobile Wallet | MOBILE_WALLET |
| Bank Account and Fraud Report | BANK_FRAUD |
| Mobile and Internet Banking | MIB |
