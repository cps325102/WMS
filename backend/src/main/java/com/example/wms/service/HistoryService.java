package com.example.wms.service;

import com.example.wms.storage.DataStorage;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HistoryService {
    private final DataStorage storage;
    public HistoryService(DataStorage storage) { this.storage = storage; }

    public List<Map<String, Object>> listLogs(String businessType, String keyword, String startTime, String endTime, String operator) {
        List<Map<String, Object>> logs = new ArrayList<>();
        // 从流水表获取
        for (Map<String, Object> r : storage.findAllRecords()) {
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("logType", r.get("businessType"));
            log.put("businessNo", r.get("businessNo"));
            log.put("materialCode", r.get("materialCode"));
            log.put("materialName", r.get("materialName"));
            log.put("batchNo", r.get("batchNo"));
            log.put("changeQty", r.get("changeQty"));
            log.put("beforeQty", r.get("beforeQty"));
            log.put("afterQty", r.get("afterQty"));
            log.put("operator", r.get("operator"));
            log.put("operateTime", r.get("operateTime"));
            log.put("remark", r.get("remark"));
            logs.add(log);
        }
        // 过滤
        if (businessType != null && !businessType.isBlank()) {
            logs = logs.stream().filter(l -> businessType.equals(l.get("logType"))).collect(Collectors.toList());
        }
        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.toLowerCase();
            logs = logs.stream().filter(l -> safeToString(l.get("materialCode")).toLowerCase().contains(lower)
                    || safeToString(l.get("materialName")).toLowerCase().contains(lower)
                    || safeToString(l.get("batchNo")).toLowerCase().contains(lower)
                    || safeToString(l.get("businessNo")).toLowerCase().contains(lower)).collect(Collectors.toList());
        }
        if (startTime != null && !startTime.isBlank()) {
            logs = logs.stream().filter(l -> safeToString(l.get("operateTime")).compareTo(startTime) >= 0).collect(Collectors.toList());
        }
        if (endTime != null && !endTime.isBlank()) {
            logs = logs.stream().filter(l -> safeToString(l.get("operateTime")).compareTo(endTime) <= 0).collect(Collectors.toList());
        }
        if (operator != null && !operator.isBlank()) {
            logs = logs.stream().filter(l -> operator.equals(l.get("operator"))).collect(Collectors.toList());
        }
        logs.sort((a, b) -> -safeToString(a.get("operateTime")).compareTo(safeToString(b.get("operateTime"))));
        return logs;
    }

    public List<Map<String, Object>> traceByBatch(String batchNo) {
        List<Map<String, Object>> trace = new ArrayList<>();
        for (Map<String, Object> r : storage.findAllRecords()) {
            if (batchNo.equals(r.get("batchNo"))) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("businessType", r.get("businessType"));
                t.put("businessNo", r.get("businessNo"));
                t.put("changeQty", r.get("changeQty"));
                t.put("beforeQty", r.get("beforeQty"));
                t.put("afterQty", r.get("afterQty"));
                t.put("operator", r.get("operator"));
                t.put("operateTime", r.get("operateTime"));
                t.put("remark", r.get("remark"));
                trace.add(t);
            }
        }
        // 也查入库记录
        for (Map<String, Object> r : storage.findAllInboundRecords()) {
            if (batchNo.equals(r.get("batchNo"))) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("businessType", "入库");
                t.put("businessNo", r.get("orderNo"));
                t.put("changeQty", r.get("receiveQty"));
                t.put("operator", r.get("operator"));
                t.put("operateTime", r.get("operateTime"));
                t.put("remark", r.get("remark"));
                trace.add(t);
            }
        }
        trace.sort((a, b) -> -safeToString(a.get("operateTime")).compareTo(safeToString(b.get("operateTime"))));
        return trace;
    }

    public List<Map<String, Object>> traceByMaterial(String materialCode) {
        List<Map<String, Object>> trace = new ArrayList<>();
        for (Map<String, Object> r : storage.findAllRecords()) {
            if (materialCode.equals(r.get("materialCode"))) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("businessType", r.get("businessType"));
                t.put("businessNo", r.get("businessNo"));
                t.put("batchNo", r.get("batchNo"));
                t.put("changeQty", r.get("changeQty"));
                t.put("beforeQty", r.get("beforeQty"));
                t.put("afterQty", r.get("afterQty"));
                t.put("operator", r.get("operator"));
                t.put("operateTime", r.get("operateTime"));
                t.put("remark", r.get("remark"));
                trace.add(t);
            }
        }
        trace.sort((a, b) -> -safeToString(a.get("operateTime")).compareTo(safeToString(b.get("operateTime"))));
        return trace;
    }

    public String exportCsv(String businessType, String keyword, String startTime, String endTime, String operator) {
        List<Map<String, Object>> logs = listLogs(businessType, keyword, startTime, endTime, operator);
        StringBuilder sb = new StringBuilder();
        sb.append("业务类型,单据编号,物料编码,物料名称,批次号,变动数量,操作前,操作后,操作人,操作时间,备注\n");
        for (Map<String, Object> log : logs) {
            sb.append(safeToString(log.get("logType"))).append(",");
            sb.append(safeToString(log.get("businessNo"))).append(",");
            sb.append(safeToString(log.get("materialCode"))).append(",");
            sb.append(safeToString(log.get("materialName"))).append(",");
            sb.append(safeToString(log.get("batchNo"))).append(",");
            sb.append(log.get("changeQty")).append(",");
            sb.append(log.get("beforeQty")).append(",");
            sb.append(log.get("afterQty")).append(",");
            sb.append(safeToString(log.get("operator"))).append(",");
            sb.append(safeToString(log.get("operateTime"))).append(",");
            sb.append(safeToString(log.get("remark"))).append("\n");
        }
        return sb.toString();
    }

    private String safeToString(Object obj) { return obj == null ? "" : obj.toString(); }
}
