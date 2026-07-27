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
| Password tools | Random Password / formulas | Generate buttons + notice checkboxes |

## Database

- `financial_institutions` — FI Code, name, short title, highlight, sort
- `system_types` — VPN, PSS2, INTER_BANK, MOBILE_WALLET, BANK_FRAUD, MIB
- `credentials` — user_id, password, psk, update_date per FI+system
- `result_notices` — printable snapshot of a notice
- `users` — app login (existing)

## Password rules

- Length: 8 (default)
- Charset: `A–Z a–z 0–9` + `!@#$%^&*()-_+[]{}|`
- User ID rule: `LEFT(fi_code, 4) + system.id_suffix`

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
