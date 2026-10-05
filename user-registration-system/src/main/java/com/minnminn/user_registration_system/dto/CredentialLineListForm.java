package com.minnminn.user_registration_system.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper so Spring can bind lines[0].userId from FI create/edit form.
 */
public class CredentialLineListForm {

    private List<CredentialLine> lines = new ArrayList<>();

    public List<CredentialLine> getLines() {
        return lines;
    }

    public void setLines(List<CredentialLine> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }
}
