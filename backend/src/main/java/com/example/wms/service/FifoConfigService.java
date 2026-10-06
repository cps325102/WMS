package com.example.wms.service;

import com.example.wms.storage.DataStorage;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class FifoConfigService {
    private final DataStorage storage;
    public FifoConfigService(DataStorage storage) { this.storage = storage; }

    public Map<String, Object> getActiveConfig() {
        List<Map<String, Object>> configs = storage.findAllFifoConfigs();
        return configs.stream().filter(c -> "Y".equals(c.get("active"))).findFirst()
                .orElseGet(() -> {
                    Map<String, Object> def = new LinkedHashMap<>();
                    def.put("id", 0L);
                    def.put("mode", "loose");
                    def.put("active", "Y");
                    return def;
                });
    }

    public Map<String, Object> switchMode(String mode) {
        if (!List.of("strict", "loose").contains(mode)) throw new RuntimeException("模式必须为 strict 或 loose");
        for (Map<String, Object> cfg : storage.findAllFifoConfigs()) {
            cfg.put("active", "N");
            storage.saveFifoConfig(cfg);
        }
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("mode", mode);
        config.put("active", "Y");
        return storage.saveFifoConfig(config);
    }

    public boolean isStrictMode() {
        return "strict".equals(getActiveConfig().get("mode"));
    }

    public List<Map<String, Object>> listAll() {
        return storage.findAllFifoConfigs();
    }

    public Map<String, Object> updateConfig(Long id, Map<String, Object> body) {
        Map<String, Object> config = storage.findFifoConfigById(id);
        if (config == null) throw new RuntimeException("FIFO配置不存在");
        if (body.containsKey("mode")) config.put("mode", body.get("mode"));
        if (body.containsKey("active")) config.put("active", body.get("active"));
        return storage.saveFifoConfig(config);
    }
}
