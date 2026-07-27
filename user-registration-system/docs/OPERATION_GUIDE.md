# FIDES ID Management — Operation Guide

## Start

```bash
cd user-registration-system
./mvnw spring-boot:run
```

Open http://localhost:8080 → Register → Login.

## Daily workflow

### 1. Import Excel (59 banks)

The real workbook is at:

`data/FIDES_ID_Management.xlsx`

(copied from your Downloads file)

**Option A — UI**
1. Login → **Import Excel**
2. Upload the `.xlsx`
3. Tick **Replace all banks** → Import

**Option B — Bundled reload**
On Import page → **Reload bundled Excel (59 banks)**

Current DB load: **59 institutions**, **268 credentials** from the User ID sheet.


### 2. Manage credentials

1. **Institutions** → open a bank
2. **Edit** a system row, or click **Password** / **PSK** / **User ID** to generate
3. Use **Edit FI** to change name/code/highlight

## Result Notice (batch report)

1. Menu → **Result Notice**
2. Set dropdowns (like Excel):
   - VPN status: Updated / Pre-Shared Key Change / No Change
   - FIDES Login status: Updated / No Change
   - User Category: N/A, CBM-NET, Inter-Bank, Mobile Wallet, Fraud, MIB
3. Select banks:
   - one bank (checkbox)
   - multiple banks
   - **Select all** (all 59)
4. Optional: tick password / PSK generate
5. **Generate Result Notice report(s)**
6. Review → **Print all** (one page per bank)


### 4. History

Menu → **Notice History** → View / Print past notices.

## Tips

- Re-import updates existing FI by `FI Code` (upsert).
- Yellow fields match Excel highlight cells.
- Red / yellow institution rows keep Excel highlight status after import.
