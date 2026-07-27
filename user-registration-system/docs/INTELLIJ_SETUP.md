# IntelliJ setup (fix red errors)

## Cause

Your machine has **JDK 26** only, but IntelliJ was set to **JDK 21** (missing). That makes the whole project red.

## Fix in IntelliJ (do this once)

1. **File → Project Structure → Project**
   - SDK: select **openjdk-26.0.2** (or add it from  
     `/Users/minnminnaung/Library/Java/JavaVirtualMachines/openjdk-26.0.2/Contents/Home`)
   - Language level: **17** (matches `pom.xml`)

2. **File → Project Structure → Modules → user-registration-system**
   - Language level: **17**

3. **Settings → Build, Execution, Deployment → Compiler → Annotation Processors**
   - Enable annotation processing ✅ (needed for **Lombok**)

4. Open `user-registration-system/pom.xml`
   - Right-click → **Maven → Reload project**

5. **Build → Rebuild Project**

6. **Run configuration working directory** (important)
   - Run → Edit Configurations → your Spring Boot run
   - Working directory:  
     `.../Documents/user-registration-system/user-registration-system`
   - Or use the saved config: **UserRegistrationSystemApplication**

7. If you see **Port 8080 already in use**:
   - Stop the other run (Terminal/Cursor), or in Terminal:
     `lsof -ti :8080 | xargs kill`
   - Then Run again in IntelliJ

## Correct folder to open

Open either:

- `Documents/user-registration-system` (current), **or**
- better: open only `Documents/user-registration-system/user-registration-system` (the Maven module with `pom.xml`)

## Run from IntelliJ

Run class: `UserRegistrationSystemApplication`  
Or Maven: `spring-boot:run`

Then open http://localhost:8080

## If Lombok still red

Install plugin: **Settings → Plugins → Lombok** → Enable → Restart IDE.
