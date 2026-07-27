package com.minnminn.user_registration_system.config;

import com.minnminn.user_registration_system.entity.SystemType;
import com.minnminn.user_registration_system.entity.SystemTypeCode;
import com.minnminn.user_registration_system.repository.SystemTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SystemTypeDataLoader implements CommandLineRunner {

    private final SystemTypeRepository systemTypeRepository;

    public SystemTypeDataLoader(SystemTypeRepository systemTypeRepository) {
        this.systemTypeRepository = systemTypeRepository;
    }

    @Override
    public void run(String... args) {
        int order = 1;
        for (SystemTypeCode code : SystemTypeCode.values()) {
            if (systemTypeRepository.findByCode(code).isEmpty()) {
                SystemType type = new SystemType();
                type.setCode(code);
                type.setDisplayName(code.getDisplayName());
                type.setSortOrder(order);
                // Default suffixes — adjust after reviewing Master sheet
                type.setIdSuffix(defaultSuffix(code));
                systemTypeRepository.save(type);
            }
            order++;
        }
    }

    private String defaultSuffix(SystemTypeCode code) {
        return switch (code) {
            case VPN -> "VPN";
            case PSS2 -> "PSS";
            case INTER_BANK -> "IBR";
            case MOBILE_WALLET -> "MW";
            case BANK_FRAUD -> "BFR";
            case MIB -> "MIB";
        };
    }
}
