package com.xiaoliao.api.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 知识库检索（原来在小程序 common.js 里做，现搬到后端）：
 * 直接读取 uploads/rag_index.json（segments + index），按关键词计分、对话指南优先，最多取 3 段。
 * 文件更新后按修改时间自动重载。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final ObjectMapper objectMapper;

    @Value("${xiaoliao.upload.dir:./uploads}")
    private String uploadDir;

    private volatile JsonNode cache;
    private volatile long cacheModified = -1;

    /** 默认取 3 段（与前端 ragSearch 默认值一致） */
    public List<String[]> search(String text) {
        return search(text, 3);
    }

    public List<String[]> search(String text, int maxSeg) {
        JsonNode root = load();
        if (root == null || text == null || text.isEmpty()) {
            return List.of();
        }
        JsonNode segments = root.get("segments");
        JsonNode index = root.get("index");
        if (segments == null || index == null || !segments.isArray()) {
            return List.of();
        }
        // 与前端一致：遍历 index 关键词，命中则给对应段落计分（LinkedHashMap 保持首次命中顺序，排序稳定）
        Map<Integer, Integer> scores = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = index.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            if (!text.contains(entry.getKey())) {
                continue;
            }
            for (JsonNode idNode : entry.getValue()) {
                int id = idNode.asInt(-1);
                if (id >= 0) {
                    scores.merge(id, 1, Integer::sum);
                }
            }
        }
        if (scores.isEmpty()) {
            return List.of();
        }
        List<Integer> ranked = new ArrayList<>(scores.keySet());
        ranked.sort((a, b) -> Integer.compare(scores.get(b), scores.get(a)));
        List<String[]> guide = new ArrayList<>();
        List<String[]> other = new ArrayList<>();
        for (int id : ranked) {
            if (id >= segments.size()) {
                continue;
            }
            JsonNode seg = segments.get(id);
            if (seg == null || !seg.isArray() || seg.size() < 2) {
                continue;
            }
            String[] row = {seg.get(0).asText(""), seg.get(1).asText("")};
            if ("对话指南".equals(row[0])) {
                guide.add(row);
            } else {
                other.add(row);
            }
        }
        List<String[]> all = new ArrayList<>(guide);
        all.addAll(other);
        return all.size() > maxSeg ? new ArrayList<>(all.subList(0, maxSeg)) : all;
    }

    private JsonNode load() {
        try {
            Path path = Paths.get(uploadDir, "rag_index.json").toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                return cache;
            }
            long modified = Files.getLastModifiedTime(path).toMillis();
            JsonNode local = cache;
            if (local != null && modified == cacheModified) {
                return local;
            }
            synchronized (this) {
                if (cache != null && modified == cacheModified) {
                    return cache;
                }
                cache = objectMapper.readTree(Files.readAllBytes(path));
                cacheModified = modified;
                log.info("[RAG] 已加载知识库 {} ({} KB)", path, Files.size(path) / 1024);
            }
            return cache;
        } catch (Exception e) {
            log.warn("[RAG] 加载失败: {}", e.getMessage());
            return cache;
        }
    }
}
