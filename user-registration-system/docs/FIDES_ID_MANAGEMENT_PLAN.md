# FIDES ID Management — Project Plan

## Status: Complete (MVP)

Excel → Spring Boot web conversion is implemented end-to-end.

## Delivered

1. Normalized SQLite tables for FI + credentials + notices
2. Excel **User ID** sheet import (Apache POI)
3. Institution / credential CRUD + generators
4. **Result Notice** report form (Excel layout)
5. Notice save, view, print, history
6. Docs: Plan, Specs, Operation Guide

## Stack

- Java 17, Spring Boot 4.x
- Spring Data JPA + SQLite
- Thymeleaf + Bootstrap
- Apache POI (xlsx import)

## Optional later

- PDF export
- Encrypt credentials at rest
- Spring Security roles
- Master-sheet dedicated import
