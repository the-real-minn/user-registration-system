# FIDES ID Management — Operation Guide

## Start with double-click (Mac + Windows)

Use the **same folder** on both computers:

`user-registration-system/user-registration-system/`

| OS | Start | Stop |
|----|-------|------|
| **macOS** | Double-click **Start FIDES.command** | **Stop FIDES.command** |
| **Windows** | Double-click **Start FIDES.bat** | **Stop FIDES.bat** |

Both open http://localhost:8080 and keep a console window open while the app runs.

### First time notes

- Install **JDK 17+** on that computer.
- Build once if the JAR is missing (the Start script builds automatically), or run:
  - Mac: `./mvnw -DskipTests package`
  - Windows: `mvnw.cmd -DskipTests package`
- Copy/share this whole module folder (include `target/*.jar`, `user_registration_system.db`, `data/`).
- macOS first open: right-click Start file → **Open** → **Open**.
- Windows SmartScreen: **More info** → **Run anyway** if prompted.

### Folder contents for sharing

```text
user-registration-system/
  Start FIDES.command      ← Mac start
  Stop FIDES.command       ← Mac stop
  Start FIDES.bat          ← Windows start
  Stop FIDES.bat           ← Windows stop
  mvnw / mvnw.cmd
  pom.xml
  user_registration_system.db
  data/FIDES_ID_Management.xlsx
  target/user-registration-system-0.0.1-SNAPSHOT.jar
```

## Start (development)

**Mac**
```bash
cd ~/Documents/user-registration-system/user-registration-system
./mvnw spring-boot:run
```

**Windows**
```bat
cd user-registration-system\user-registration-system
mvnw.cmd spring-boot:run
```

Open http://localhost:8080 → Register → Login.

## Build & run Executable JAR (production-style)

Spring Boot packages an **executable JAR** with embedded Tomcat. No external server needed.

### 1. Build

**Mac**
```bash
cd ~/Documents/user-registration-system/user-registration-system
./mvnw -DskipTests clean package
```

**Windows**
```bat
cd user-registration-system\user-registration-system
mvnw.cmd -DskipTests clean package
```

Output JAR:

`target/user-registration-system-0.0.1-SNAPSHOT.jar`

### 2. Run

Prefer double-click Start files above, or:

**Mac**
```bash
java -jar target/user-registration-system-0.0.1-SNAPSHOT.jar
```

**Windows**
```bat
java -jar target\user-registration-system-0.0.1-SNAPSHOT.jar
```

Then open http://localhost:8080

Stop with Ctrl+C, Stop scripts, or free port 8080:
- Mac: `lsof -ti :8080 | xargs kill`
- Windows: use **Stop FIDES.bat**

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

## Result Notice (batch report + ZIP)

1. Menu → **Result Notice**
2. Select banks and generate reports
3. Preview PDFs on screen
4. Click **Download ZIP (1 PDF per BIC)**

Each PDF matches the Excel Result Notice layout:
- black title bar, date `dd/MM/yyyy`
- gray label cells for ID / Password / PSK rows
- Password Reference at the bottom
- no yellow highlight backgrounds

ZIP example: `ResultNotice_20260731_111800.zip`

Inside the zip:
- `CBMYMMMY.pdf`
- `MYEBMMMY.pdf`
- …


### 4. History

Menu → **Notice History** → View / Print past notices.

## Tips

- Re-import updates existing FI by `FI Code` (upsert).
- Yellow fields match Excel highlight cells.
- Red / yellow institution rows keep Excel highlight status after import.
