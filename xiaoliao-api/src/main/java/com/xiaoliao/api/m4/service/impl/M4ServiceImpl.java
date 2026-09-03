package com.xiaoliao.api.m4.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoliao.api.m4.dto.ConsentRequest;
import com.xiaoliao.api.m4.dto.EraRequest;
import com.xiaoliao.api.m4.dto.HelpSubmitRequest;
import com.xiaoliao.api.m4.dto.MatchImageRequest;
import com.xiaoliao.api.m4.dto.MomentGenerateRequest;
import com.xiaoliao.api.m4.dto.NodeRequest;
import com.xiaoliao.api.m4.dto.PhotoUploadRequest;
import com.xiaoliao.api.m4.dto.StorySaveRequest;
import com.xiaoliao.api.m4.entity.M4Era;
import com.xiaoliao.api.m4.entity.M4PrivacyConsent;
import com.xiaoliao.api.m4.entity.M4MomentCopy;
import com.xiaoliao.api.m4.entity.M4Photo;
import com.xiaoliao.api.m4.entity.M4ShareHelp;
import com.xiaoliao.api.m4.entity.M4StockImage;
import com.xiaoliao.api.m4.entity.M4Story;
import com.xiaoliao.api.m4.entity.M4TimelineNode;
import com.xiaoliao.api.m4.mapper.M4EraMapper;
import com.xiaoliao.api.m4.mapper.M4PrivacyConsentMapper;
import com.xiaoliao.api.m4.mapper.M4MomentCopyMapper;
import com.xiaoliao.api.m4.mapper.M4PhotoMapper;
import com.xiaoliao.api.m4.mapper.M4ShareHelpMapper;
import com.xiaoliao.api.m4.mapper.M4StockImageMapper;
import com.xiaoliao.api.m4.mapper.M4StoryMapper;
import com.xiaoliao.api.m4.mapper.M4TimelineNodeMapper;
import com.xiaoliao.api.m4.service.M4Service;
import com.xiaoliao.api.m4.vo.ConsentVO;
import com.xiaoliao.api.m4.vo.EraVO;
import com.xiaoliao.api.m4.vo.ExportVO;
import com.xiaoliao.api.m4.vo.GardenVO;
import com.xiaoliao.api.m4.vo.HelpDetailVO;
import com.xiaoliao.api.m4.vo.MatchResultVO;
import com.xiaoliao.api.m4.vo.MomentVO;
import com.xiaoliao.api.m4.vo.NodeVO;
import com.xiaoliao.api.m4.vo.PhotoVO;
import com.xiaoliao.api.m4.vo.ShareHelpVO;
import com.xiaoliao.api.m4.vo.StockImageVO;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.xiaoliao.api.m4.vo.StoryVO;
import com.xiaoliao.api.user.UserService;
import com.xiaoliao.api.util.CryptoTypeHandler;
import com.xiaoliao.common.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * M4 回忆与传承实现（云存储版）
 * <p>
 * 设计说明：
 * - 人生时光：预设七阶段节点链，首次访问自动建节点，可增删改；
 * - 年代记忆：年代列表可增删，故事按年代/节点挂载；
 * - 朋友圈文案：本期规则模板兜底，AI 引擎接入后替换为真实生成；
 * - 素材库匹配：按描述分词与素材标签打分，返回前 N 个候选；
 * - 家人帮忙：免登录 token 分享，7 天有效，老人确认后采用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class M4ServiceImpl implements M4Service {

    private final M4TimelineNodeMapper nodeMapper;
    private final M4EraMapper eraMapper;
    private final M4PhotoMapper photoMapper;
    private final M4StoryMapper storyMapper;
    private final M4MomentCopyMapper momentMapper;
    private final M4ShareHelpMapper shareHelpMapper;
    private final M4StockImageMapper stockImageMapper;
    private final M4PrivacyConsentMapper consentMapper;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    /** 预设人生阶段（stage -> 默认展示名） */
    private static final Map<String, String> PRESET_STAGES = new LinkedHashMap<>();
    static {
        PRESET_STAGES.put("childhood", "出生与童年");
        PRESET_STAGES.put("school", "上学");
        PRESET_STAGES.put("work", "工作/当兵");
        PRESET_STAGES.put("marriage", "结婚成家");
        PRESET_STAGES.put("parenting", "生儿育女");
        PRESET_STAGES.put("grandchildren", "孙辈");
        PRESET_STAGES.put("retirement", "退休与现在");
    }

    private static final int SHARE_HELP_VALID_DAYS = 7;

    /** Pexels 图库 API Key（生产用环境变量 XIAOLIAO_PEXELS_KEY；为空时不联网搜图） */
    @Value("${xiaoliao.pexels.key:}")
    private String pexelsKey;

    private static final HttpClient WEB_CLIENT = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .build();

    /** 中文描述 → 英文检索词候选（Pexels 英文命中率高） */
    private static final Map<String, String[]> PEXELS_QUERY_MAP = Map.ofEntries(
            Map.entry("土房", new String[]{"mud house", "old house"}),
            Map.entry("茅草", new String[]{"thatched house"}),
            Map.entry("老屋", new String[]{"old house", "village house"}),
            Map.entry("泥墙", new String[]{"mud wall", "old house"}),
            Map.entry("绿皮火车", new String[]{"vintage train", "green train"}),
            Map.entry("火车", new String[]{"train station vintage"}),
            Map.entry("自行车", new String[]{"vintage bicycle"}),
            Map.entry("军装", new String[]{"army uniform"}),
            Map.entry("当兵", new String[]{"soldier vintage"}),
            Map.entry("全家福", new String[]{"family portrait vintage"}),
            Map.entry("老照片", new String[]{"old photo vintage"}),
            Map.entry("黑白", new String[]{"black and white photo"}),
            Map.entry("街道", new String[]{"old street"}),
            Map.entry("农村", new String[]{"village old"}),
            Map.entry("工厂", new String[]{"old factory"}),
            Map.entry("车间", new String[]{"factory workshop"}));
    private static final int SUGGESTION_LIMIT = 5;
    private static final int DAILY_MOMENT_LIMIT = 20;
    private static final int DAILY_MATCH_LIMIT = 30;
    private static final int DAILY_HELP_LIMIT = 20;
    private static final Pattern WORD_SPLIT = Pattern.compile("[，。、；,.!！?？\\s]+");

    // ==================== 照片 ====================

    @Override
    public PhotoVO savePhoto(String userId, PhotoUploadRequest request) {
        requireUser(userId);
        M4Photo photo = new M4Photo();
        photo.setUserId(userId);
        photo.setSource(request.getSource());
        photo.setUrl(request.getUrl().trim());
        photo.setIsIllustration(request.getIsIllustration() != null ? request.getIsIllustration() : 0);
        photo.setEraTag(request.getEraTag());
        photo.setNodeId(request.getNodeId());
        photo.setEraId(request.getEraId());
        photo.setCreateTime(LocalDateTime.now());
        photo.setUpdateTime(LocalDateTime.now());
        photoMapper.insert(photo);
        log.info("照片保存成功: userId={}, id={}, source={}", userId, photo.getId(), photo.getSource());
        return toPhotoVO(photo);
    }

    @Override
    public List<PhotoVO> listPhotos(String userId) {
        requireUser(userId);
        return photoMapper.selectList(new LambdaQueryWrapper<M4Photo>()
                        .eq(M4Photo::getUserId, userId)
                        .orderByDesc(M4Photo::getCreateTime))
                .stream().map(this::toPhotoVO).collect(Collectors.toList());
    }

    @Override
    public MatchResultVO matchImage(String userId, MatchImageRequest request) {
        requireUser(userId);
        if (overDailyLimit("match:" + userId, DAILY_MATCH_LIMIT)) {
            throw new BusinessException(429, "今天已经找过很多次图啦，明天再来吧～");
        }
        MatchResultVO result = new MatchResultVO();
        if (request.getPhotoId() != null) {
            M4Photo photo = photoMapper.selectOne(new LambdaQueryWrapper<M4Photo>()
                    .eq(M4Photo::getId, request.getPhotoId())
                    .eq(M4Photo::getUserId, userId));
            if (photo != null) {
                result.setPhoto(toPhotoVO(photo));
            }
        }

        // 检索词：优先用引擎提取的关键词，否则按描述分词
        final List<String> tokens;
        if (StringUtils.hasText(request.getKeywords())) {
            tokens = Arrays.stream(request.getKeywords().split("[,，]"))
                    .map(String::trim).filter(StringUtils::hasText).collect(Collectors.toList());
        } else {
            tokens = Arrays.stream(WORD_SPLIT.split(request.getDescription()))
                    .map(String::trim).filter(t -> t.length() >= 2).collect(Collectors.toList());
        }

        List<M4StockImage> all = stockImageMapper.selectList(new LambdaQueryWrapper<M4StockImage>()
                .orderByDesc(M4StockImage::getCreateTime));

        // 打分：命中 token 数排序。搜不到就明确返回 matched=false，绝不硬塞不相关图
        List<M4StockImage> matched = all.stream()
                .map(img -> new Object[]{img, score(img, tokens) + scoreWhole(img, request.getDescription())})
                .filter(entry -> (Long) entry[1] > 0)
                .sorted((a, b) -> Long.compare((Long) b[1], (Long) a[1]))
                .limit(SUGGESTION_LIMIT)
                .map(entry -> (M4StockImage) entry[0])
                .collect(Collectors.toList());
        result.setMatched(!matched.isEmpty());
        result.setSuggestions(matched.stream().map(this::toStockVO).collect(Collectors.toList()));
        log.info("年代示意图匹配: userId={}, description={}, matched={}, count={}",
                userId, request.getDescription(), result.isMatched(), result.getSuggestions().size());
        // 本地素材库没命中 → 联网搜公版图（Pexels），搜到缓存进素材库；没 key/失败则保持 matched=false 不硬配
        if (!result.isMatched()) {
            List<M4StockImage> webImages = searchWebImages(request.getDescription(), tokens);
            if (!webImages.isEmpty()) {
                result.setMatched(true);
                result.setSuggestions(webImages.stream().map(this::toStockVO).collect(Collectors.toList()));
            }
        }
        return result;
    }

    /**
     * 外网搜公版图（Pexels）：本地素材库没命中时兜底。
     * 返回空列表 = 没搜到/没配 key/网络失败，绝不硬配图。
     */
    private List<M4StockImage> searchWebImages(String description, List<String> tokens) {
        if (!StringUtils.hasText(pexelsKey)) {
            log.warn("联网搜图跳过：未配置 XIAOLIAO_PEXELS_KEY");
            return Collections.emptyList();
        }
        // 检索词：中文关键词词典优先 → 描述分词兜底 → 最后 "old photo"
        List<String> queries = new ArrayList<>();
        if (StringUtils.hasText(description)) {
            for (Map.Entry<String, String[]> e : PEXELS_QUERY_MAP.entrySet()) {
                if (description.contains(e.getKey())) {
                    queries.addAll(Arrays.asList(e.getValue()));
                }
            }
        }
        for (String t : tokens) {
            if (!PEXELS_QUERY_MAP.containsKey(t) && StringUtils.hasText(t)) {
                queries.add(t);
            }
        }
        if (queries.isEmpty()) {
            queries.add("old photo");
        }
        List<String> uniq = queries.stream().distinct().limit(3).collect(Collectors.toList());
        for (String q : uniq) {
            try {
                URI uri = URI.create("https://api.pexels.com/v1/search?query="
                        + URLEncoder.encode(q, StandardCharsets.UTF_8) + "&per_page=5");
                HttpRequest req = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(8))
                        .header("Authorization", pexelsKey)
                        .GET()
                        .build();
                HttpResponse<String> resp = WEB_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    log.warn("联网搜图非200 query={} status={}", q, resp.statusCode());
                    continue;
                }
                List<M4StockImage> imgs = parsePexels(resp.body(), q);
                if (!imgs.isEmpty()) {
                    // 缓存进素材库（按 url 判重），下次直接命中本地，减少外网请求
                    for (M4StockImage img : imgs) {
                        Long exist = stockImageMapper.selectCount(new LambdaQueryWrapper<M4StockImage>()
                                .eq(M4StockImage::getUrl, img.getUrl()));
                        if (exist == null || exist == 0) {
                            stockImageMapper.insert(img);
                        }
                    }
                    log.info("联网搜图成功 query={} count={}", q, imgs.size());
                    return imgs;
                }
            } catch (Exception e) {
                log.warn("联网搜图失败 query={} 根因={}", q, e.getMessage());
            }
        }
        return Collections.emptyList();
    }

    /** 轻量解析 Pexels 响应（不引第三方 JSON 库）：取 src.medium 与 photographer */
    private List<M4StockImage> parsePexels(String body, String query) {
        List<M4StockImage> out = new ArrayList<>();
        try {
            Matcher m = Pattern.compile("\"src\"\\s*:\\s*\\{[^}]*?\"medium\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
            Matcher p = Pattern.compile("\"photographer\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
            List<String> urls = new ArrayList<>();
            while (m.find()) {
                urls.add(m.group(1));
            }
            while (p.find()) {
                // 仅推进匹配，photographer 不单独使用
            }
            for (int i = 0; i < Math.min(urls.size(), 5); i++) {
                M4StockImage img = new M4StockImage();
                img.setUrl(urls.get(i));
                img.setKeywords(query);
                img.setScene(query);
                img.setLicense("pexels-free");
                img.setSource("web");
                img.setCreateTime(LocalDateTime.now());
                out.add(img);
            }
        } catch (Exception e) {
            log.warn("解析 Pexels 结果失败: {}", e.getMessage());
        }
        return out;
    }
    /** 整段描述包含素材库关键词也算命中（解决"以前是土房"这类长句切分不到关键词的问题） */
    private long scoreWhole(M4StockImage img, String description) {
        if (!StringUtils.hasText(description) || !StringUtils.hasText(img.getKeywords())) {
            return 0;
        }
        long hit = 0;
        for (String kw : img.getKeywords().split("[,，]")) {
            String k = kw.trim();
            if (StringUtils.hasText(k) && description.contains(k)) {
                hit++;
            }
        }
        return hit;
    }
    private long score(M4StockImage img, List<String> tokens) {
        String haystack = img.getKeywords() + "," + (img.getEra() == null ? "" : img.getEra())
                + "," + (img.getScene() == null ? "" : img.getScene());
        return tokens.stream().filter(t -> haystack.contains(t)).count();
    }

    // ==================== 故事 ====================

    @Override
    public StoryVO saveStory(String userId, StorySaveRequest request) {
        requireUser(userId);
        M4Story story = new M4Story();
        story.setUserId(userId);
        story.setNodeId(request.getNodeId());
        story.setEraId(request.getEraId());
        story.setPhotoId(request.getPhotoId());
        story.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : null);
        story.setOriginalText(CryptoTypeHandler.encrypt(request.getOriginalText().trim()));
        story.setPolishedText(StringUtils.hasText(request.getPolishedText()) ? CryptoTypeHandler.encrypt(request.getPolishedText().trim()) : null);
        story.setSummary(StringUtils.hasText(request.getSummary()) ? request.getSummary().trim() : null);
        story.setMood(request.getMood());
        story.setMusic(request.getMusic());
        story.setStatus(1);
        story.setCreateTime(LocalDateTime.now());
        story.setUpdateTime(LocalDateTime.now());
        storyMapper.insert(story);
        log.info("故事保存成功: userId={}, id={}, nodeId={}, eraId={}", userId, story.getId(), story.getNodeId(), story.getEraId());
        return toStoryVO(story);
    }

    @Override
    public List<StoryVO> listStories(String userId, Long nodeId) {
        requireUser(userId);
        LambdaQueryWrapper<M4Story> wrapper = new LambdaQueryWrapper<M4Story>()
                .eq(M4Story::getUserId, userId)
                .orderByDesc(M4Story::getCreateTime);
        if (nodeId != null) {
            wrapper.eq(M4Story::getNodeId, nodeId);
        }
        return storyMapper.selectList(wrapper).stream().map(this::toStoryVO).collect(Collectors.toList());
    }

    // ==================== 朋友圈文案 ====================

    @Override
    public List<MomentVO> generateMoments(String userId, MomentGenerateRequest request) {
        requireUser(userId);
        if (overDailyLimit("moments:" + userId, DAILY_MOMENT_LIMIT)) {
            throw new BusinessException(429, "今天已经写过很多次啦，明天再来吧～");
        }
        String style = StringUtils.hasText(request.getStyle()) ? request.getStyle() : "simple";
        // 优先按故事润色/原文生成；没故事或没内容时退回规则模板兜底
        List<String> templates = new ArrayList<>();
        if (request.getStoryId() != null) {
            M4Story story = storyMapper.selectById(request.getStoryId());
            if (story != null && userId.equals(story.getUserId())) {
                List<String> fromStory = momentCopyForStory(style, story);
                if (fromStory != null) {
                    templates.addAll(fromStory);
                }
            }
        }
        if (templates.isEmpty()) {
            templates.addAll(Arrays.asList(momentTemplates(style)));
        }
        List<MomentVO> result = new ArrayList<>();
        for (String template : templates) {
            M4MomentCopy copy = new M4MomentCopy();
            copy.setUserId(userId);
            copy.setPhotoId(request.getPhotoId());
            copy.setStoryId(request.getStoryId());
            copy.setContent(template);
            copy.setStyle(style);
            copy.setSelected(0);
            copy.setCreateTime(LocalDateTime.now());
            copy.setUpdateTime(LocalDateTime.now());
            momentMapper.insert(copy);
            result.add(toMomentVO(copy));
        }
        log.info("朋友圈文案生成: userId={}, style={}, count={}", userId, style, result.size());
        return result;
    }

    /** 依据故事润色/原文拼出 3 条朋友圈文案；无文本可引用时返回 null（走规则模板兜底） */
    private List<String> momentCopyForStory(String style, M4Story story) {
        String content = stripOuterQuotes(CryptoTypeHandler.decrypt(story.getPolishedText()));
        if (!StringUtils.hasText(content)) {
            content = stripOuterQuotes(CryptoTypeHandler.decrypt(story.getOriginalText()));
        }
        if (!StringUtils.hasText(content)) {
            return null;
        }
        List<String> sentences = new ArrayList<>();
        for (String piece : content.split("(?<=[。！？!?…])")) {
            if (StringUtils.hasText(piece)) {
                sentences.add(piece.trim());
            }
        }
        if (sentences.isEmpty()) {
            sentences.add(content.trim());
        }
        String full = String.join("", sentences);
        String shortPart = sentences.get(0);
        boolean cut = sentences.size() > 1;
        if (shortPart.length() <= 90 && sentences.size() > 1) {
            shortPart += sentences.get(1);
            cut = sentences.size() > 2;
        }
        String[] frames = styleFrames(style == null ? "" : style);
        List<String> result = new ArrayList<>();
        result.add(frames[0] + full);
        result.add(frames[1] + shortPart + (cut ? "……" : ""));
        result.add(full + "\n" + frames[2]);
        return result;
    }

    private String stripOuterQuotes(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String t = text.trim();
        if (t.length() >= 2) {
            char first = t.charAt(0);
            char last = t.charAt(t.length() - 1);
            if ((first == '"' && last == '"') || (first == '“' && last == '”')
                    || (first == '「' && last == '」')) {
                t = t.substring(1, t.length() - 1).trim();
            }
        }
        return t;
    }

    private String[] styleFrames(String style) {
        switch (style) {
            case "warm":
                return new String[]{
                        "看到这张老照片，心里一下子就暖了：",
                        "那些暖心的日子，好像就在昨天：",
                        "这么多年了，再想起来，心里头还是热乎乎的。"};
            case "humorous":
                return new String[]{
                        "哈哈，看到这张老照片就想起从前：",
                        "现在跟小辈说起从前，自己都忍不住笑：",
                        "说给孩子们听，他们都不信，可这真的都是从前。"};
            case "proud":
                return new String[]{
                        "看看这张老照片，心里挺骄傲：",
                        "一步一个脚印走过来，看着老照片就想说：",
                        "从那时候走到今天，每一步都值。"};
            default: // simple 朴实
                return new String[]{
                        "翻出老照片，想起从前的日子：",
                        "照片泛黄了，从前的事还在眼前：",
                        "一晃好多年了，如今想起这些，心里还是踏实的。"};
        }
    }

    private String[] momentTemplates(String style) {
        switch (style == null ? "" : style) {
            case "warm":
                return new String[]{
                        "以前的日子虽然苦，现在想起来都是甜的。",
                        "老地方变了样，可心里的那些日子，一点都没变。",
                        "翻出这张照片，想起从前，心里还是暖的。"};
            case "humorous":
                return new String[]{
                        "那时候哪想得到，日子能过成现在这样。",
                        "从前是土房，现在是楼房，这一觉睡得可真长。",
                        "年纪大了爱念叨，看到这照片就忍不住说两句。"};
            case "proud":
                return new String[]{
                        "这一辈子，最不后悔的就是把日子踏踏实实过下来了。",
                        "从那时候走到今天，每一步都不容易，都值。",
                        "看着现在的好日子，想想当年，心里有底。"};
            default: // simple 朴实
                return new String[]{
                        "以前是土房，现在是楼房。日子越过越好，老屋还记得。",
                        "翻出老照片，想起以前的日子，一晃几十年了。",
                        "这张照片里，有我最念的从前。"};
        }
    }

    @Override
    public List<MomentVO> listMoments(String userId) {
        requireUser(userId);
        return momentMapper.selectList(new LambdaQueryWrapper<M4MomentCopy>()
                        .eq(M4MomentCopy::getUserId, userId)
                        .orderByDesc(M4MomentCopy::getCreateTime))
                .stream().map(this::toMomentVO).collect(Collectors.toList());
    }

    @Override
    public void selectMoment(String userId, Long momentId) {
        requireUser(userId);
        M4MomentCopy target = momentMapper.selectOne(new LambdaQueryWrapper<M4MomentCopy>()
                .eq(M4MomentCopy::getId, momentId)
                .eq(M4MomentCopy::getUserId, userId));
        if (target == null) {
            throw new BusinessException(404, "文案不存在");
        }
        // 取消其他选中
        List<M4MomentCopy> mine = momentMapper.selectList(new LambdaQueryWrapper<M4MomentCopy>()
                .eq(M4MomentCopy::getUserId, userId));
        for (M4MomentCopy copy : mine) {
            if (copy.getSelected() != null && copy.getSelected() == 1 && !copy.getId().equals(momentId)) {
                copy.setSelected(0);
                copy.setUpdateTime(LocalDateTime.now());
                momentMapper.updateById(copy);
            }
        }
        target.setSelected(1);
        target.setUpdateTime(LocalDateTime.now());
        momentMapper.updateById(target);
    }

    // ==================== 人生时光（节点） ====================

    @Override
    public List<NodeVO> timeline(String userId) {
        requireUser(userId);
        ensurePresetNodes(userId);
        List<M4TimelineNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<M4TimelineNode>()
                .eq(M4TimelineNode::getUserId, userId)
                .orderByAsc(M4TimelineNode::getSortOrder));
        return nodes.stream().map(node -> {
            NodeVO vo = toNodeVO(node);
            vo.setStoryCount(storyMapper.selectCount(new LambdaQueryWrapper<M4Story>()
                    .eq(M4Story::getUserId, userId)
                    .eq(M4Story::getNodeId, node.getId())));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public NodeVO saveNode(String userId, NodeRequest request) {
        requireUser(userId);
        M4TimelineNode node = new M4TimelineNode();
        node.setUserId(userId);
        node.setStage(request.getStage());
        node.setCustomName(StringUtils.hasText(request.getCustomName()) ? request.getCustomName().trim() : null);
        node.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        node.setYearFrom(request.getYearFrom());
        node.setYearTo(request.getYearTo());
        node.setCreateTime(LocalDateTime.now());
        node.setUpdateTime(LocalDateTime.now());
        nodeMapper.insert(node);
        return toNodeVO(node);
    }

    @Override
    public NodeVO updateNode(String userId, Long id, NodeRequest request) {
        requireUser(userId);
        M4TimelineNode node = findMyNode(userId, id);
        node.setStage(request.getStage());
        node.setCustomName(StringUtils.hasText(request.getCustomName()) ? request.getCustomName().trim() : null);
        node.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        node.setYearFrom(request.getYearFrom());
        node.setYearTo(request.getYearTo());
        node.setUpdateTime(LocalDateTime.now());
        nodeMapper.updateById(node);
        return toNodeVO(node);
    }

    @Override
    public void deleteNode(String userId, Long id) {
        requireUser(userId);
        findMyNode(userId, id);
        nodeMapper.deleteById(id);
    }

    /** 首次访问自动创建预设七阶段节点 */
    private void ensurePresetNodes(String userId) {
        Long count = nodeMapper.selectCount(new LambdaQueryWrapper<M4TimelineNode>()
                .eq(M4TimelineNode::getUserId, userId));
        if (count > 0) {
            return;
        }
        int sort = 0;
        for (Map.Entry<String, String> entry : PRESET_STAGES.entrySet()) {
            M4TimelineNode node = new M4TimelineNode();
            node.setUserId(userId);
            node.setStage(entry.getKey());
            node.setSortOrder(sort++);
            node.setCreateTime(LocalDateTime.now());
            node.setUpdateTime(LocalDateTime.now());
            nodeMapper.insert(node);
        }
        log.info("预设人生阶段创建: userId={}", userId);
    }

    // ==================== 年代记忆 ====================

    @Override
    public EraVO saveEra(String userId, EraRequest request) {
        requireUser(userId);
        M4Era era = new M4Era();
        era.setUserId(userId);
        era.setName(request.getName().trim());
        era.setCreateTime(LocalDateTime.now());
        era.setUpdateTime(LocalDateTime.now());
        eraMapper.insert(era);
        log.info("年代记忆创建: userId={}, id={}, name={}", userId, era.getId(), era.getName());
        return toEraVO(era);
    }

    @Override
    public List<EraVO> listEras(String userId) {
        requireUser(userId);
        return eraMapper.selectList(new LambdaQueryWrapper<M4Era>()
                        .eq(M4Era::getUserId, userId)
                        .orderByDesc(M4Era::getCreateTime))
                .stream().map(era -> {
                    EraVO vo = toEraVO(era);
                    vo.setStoryCount(storyMapper.selectCount(new LambdaQueryWrapper<M4Story>()
                            .eq(M4Story::getUserId, userId)
                            .eq(M4Story::getEraId, era.getId())));
                    return vo;
                }).collect(Collectors.toList());
    }

    @Override
    public void deleteEra(String userId, Long id) {
        requireUser(userId);
        M4Era era = eraMapper.selectOne(new LambdaQueryWrapper<M4Era>()
                .eq(M4Era::getId, id)
                .eq(M4Era::getUserId, userId));
        if (era == null) {
            throw new BusinessException(404, "年代不存在");
        }
        eraMapper.deleteById(id);
    }

    // ==================== 家人帮忙协作 ====================

    @Override
    public ShareHelpVO createShareHelp(String userId, Long nodeId) {
        requireUser(userId);
        M4TimelineNode node = findMyNode(userId, nodeId);
        M4ShareHelp help = new M4ShareHelp();
        help.setUserId(userId);
        help.setNodeId(nodeId);
        help.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        help.setExpiredAt(LocalDateTime.now().plusDays(SHARE_HELP_VALID_DAYS));
        help.setStatus(0);
        help.setCreateTime(LocalDateTime.now());
        help.setUpdateTime(LocalDateTime.now());
        shareHelpMapper.insert(help);

        ShareHelpVO vo = new ShareHelpVO();
        vo.setId(help.getId());
        vo.setNodeId(nodeId);
        vo.setNodeName(displayName(node));
        vo.setShareToken(help.getShareToken());
        vo.setExpiredAt(help.getExpiredAt());
        vo.setStatus(help.getStatus());
        log.info("家人帮忙分享生成: userId={}, nodeId={}, token={}", userId, nodeId, help.getShareToken());
        return vo;
    }

    @Override
    public HelpDetailVO getHelp(String token) {
        M4ShareHelp help = findHelpByToken(token);
        return toHelpDetailVO(help);
    }

    @Override
    public HelpDetailVO submitHelp(String token, HelpSubmitRequest request) {
        M4ShareHelp help = findHelpByToken(token);
        if (overDailyLimit("help:" + token, DAILY_HELP_LIMIT)) {
            throw new BusinessException(429, "提交太频繁啦，稍后再试试");
        }
        if (!StringUtils.hasText(request.getHelperNote()) && !StringUtils.hasText(request.getHelperPhotoUrl())) {
            throw new BusinessException(400, "至少补一张照片或一句提示");
        }
        help.setHelperNote(StringUtils.hasText(request.getHelperNote()) ? CryptoTypeHandler.encrypt(request.getHelperNote().trim()) : null);
        help.setHelperPhotoUrl(StringUtils.hasText(request.getHelperPhotoUrl()) ? request.getHelperPhotoUrl().trim() : null);
        help.setUpdateTime(LocalDateTime.now());
        shareHelpMapper.updateById(help);
        log.info("亲友补内容成功: token={}, nodeId={}", token, help.getNodeId());
        return toHelpDetailVO(help);
    }

    @Override
    public ShareHelpVO confirmShareHelp(String userId, Long shareId) {
        requireUser(userId);
        M4ShareHelp help = shareHelpMapper.selectOne(new LambdaQueryWrapper<M4ShareHelp>()
                .eq(M4ShareHelp::getId, shareId)
                .eq(M4ShareHelp::getUserId, userId));
        if (help == null) {
            throw new BusinessException(404, "分享不存在");
        }
        // 亲友补的照片自动存入照片库，挂到对应节点
        if (StringUtils.hasText(help.getHelperPhotoUrl()) && help.getStatus() != 1) {
            M4Photo photo = new M4Photo();
            photo.setUserId(userId);
            photo.setSource("user");
            photo.setUrl(help.getHelperPhotoUrl());
            photo.setIsIllustration(0);
            photo.setNodeId(help.getNodeId());
            photo.setCreateTime(LocalDateTime.now());
            photo.setUpdateTime(LocalDateTime.now());
            photoMapper.insert(photo);
        }
        help.setStatus(1);
        help.setUpdateTime(LocalDateTime.now());
        shareHelpMapper.updateById(help);
        log.info("老人确认采用亲友补内容: userId={}, shareId={}", userId, shareId);

        M4TimelineNode node = nodeMapper.selectById(help.getNodeId());
        ShareHelpVO vo = new ShareHelpVO();
        vo.setId(help.getId());
        vo.setNodeId(help.getNodeId());
        vo.setNodeName(node != null ? displayName(node) : null);
        vo.setShareToken(help.getShareToken());
        vo.setExpiredAt(help.getExpiredAt());
        vo.setStatus(help.getStatus());
        return vo;
    }

    // ==================== 花园 ====================

    @Override
    public GardenVO garden(String userId) {
        requireUser(userId);
        GardenVO vo = new GardenVO();
        vo.setStoryCount(storyMapper.selectCount(new LambdaQueryWrapper<M4Story>()
                .eq(M4Story::getUserId, userId)));
        vo.setNodeCount(nodeMapper.selectCount(new LambdaQueryWrapper<M4TimelineNode>()
                .eq(M4TimelineNode::getUserId, userId)));
        vo.setEraCount(eraMapper.selectCount(new LambdaQueryWrapper<M4Era>()
                .eq(M4Era::getUserId, userId)));
        vo.setPhotoCount(photoMapper.selectCount(new LambdaQueryWrapper<M4Photo>()
                .eq(M4Photo::getUserId, userId)));
        vo.setMomentCount(momentMapper.selectCount(new LambdaQueryWrapper<M4MomentCopy>()
                .eq(M4MomentCopy::getUserId, userId)
                .eq(M4MomentCopy::getSelected, 1)));
        return vo;
    }

    // ==================== 隐私与数据 ====================

    @Override
    public ConsentVO saveConsent(String userId, ConsentRequest request) {
        requireUser(userId);
        M4PrivacyConsent existing = consentMapper.selectOne(new LambdaQueryWrapper<M4PrivacyConsent>()
                .eq(M4PrivacyConsent::getUserId, userId)
                .eq(M4PrivacyConsent::getConsentType, request.getConsentType()));
        if (existing != null) {
            existing.setGranted(request.getGranted());
            existing.setUpdateTime(LocalDateTime.now());
            consentMapper.updateById(existing);
            return toConsentVO(existing);
        }
        M4PrivacyConsent consent = new M4PrivacyConsent();
        consent.setUserId(userId);
        consent.setConsentType(request.getConsentType());
        consent.setGranted(request.getGranted());
        consent.setCreateTime(LocalDateTime.now());
        consent.setUpdateTime(LocalDateTime.now());
        consentMapper.insert(consent);
        log.info("隐私授权记录: userId={}, type={}, granted={}", userId, request.getConsentType(), request.getGranted());
        return toConsentVO(consent);
    }

    @Override
    public List<ConsentVO> listConsents(String userId) {
        requireUser(userId);
        return consentMapper.selectList(new LambdaQueryWrapper<M4PrivacyConsent>()
                        .eq(M4PrivacyConsent::getUserId, userId))
                .stream().map(this::toConsentVO).collect(Collectors.toList());
    }

    @Override
    public ExportVO exportAll(String userId) {
        requireUser(userId);
        ExportVO vo = new ExportVO();
        vo.setNodes(nodeMapper.selectList(new LambdaQueryWrapper<M4TimelineNode>()
                        .eq(M4TimelineNode::getUserId, userId))
                .stream().map(this::toNodeVO).collect(Collectors.toList()));
        vo.setStories(listStories(userId, null));
        vo.setEras(listEras(userId));
        vo.setPhotos(listPhotos(userId));
        vo.setMoments(listMoments(userId));
        vo.setConsents(listConsents(userId));
        log.info("数据导出: userId={}", userId);
        return vo;
    }

    @Override
    public Map<String, Object> deleteAllData(String userId) {
        requireUser(userId);
        Map<String, Object> result = new HashMap<>();
        String[] tables = {"m4_story", "m4_photo", "m4_moment_copy", "m4_timeline_node",
                "m4_era", "m4_share_help", "m4_privacy_consent"};
        for (String table : tables) {
            int n = jdbcTemplate.update("DELETE FROM " + table + " WHERE user_id = ?", userId);
            result.put(table, n);
        }
        log.warn("用户数据已物理删除（被遗忘权）: userId={}, counts={}", userId, result);
        return result;
    }

    @Override
    public ShareHelpVO revokeShareHelp(String userId, Long shareId) {
        requireUser(userId);
        M4ShareHelp help = shareHelpMapper.selectOne(new LambdaQueryWrapper<M4ShareHelp>()
                .eq(M4ShareHelp::getId, shareId)
                .eq(M4ShareHelp::getUserId, userId));
        if (help == null) {
            throw new BusinessException(404, "分享不存在");
        }
        help.setStatus(2);
        help.setUpdateTime(LocalDateTime.now());
        shareHelpMapper.updateById(help);
        log.info("分享已作废: userId={}, shareId={}", userId, shareId);

        M4TimelineNode node = nodeMapper.selectById(help.getNodeId());
        ShareHelpVO vo = new ShareHelpVO();
        vo.setId(help.getId());
        vo.setNodeId(help.getNodeId());
        vo.setNodeName(node != null ? displayName(node) : null);
        vo.setShareToken(help.getShareToken());
        vo.setExpiredAt(help.getExpiredAt());
        vo.setStatus(help.getStatus());
        return vo;
    }

    /** Redis 按日限流：超过上限抛 429；Redis 不可用时放行（不阻断业务） */
    private boolean overDailyLimit(String key, int max) {
        try {
            String redisKey = "m4:limit:" + key;
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(redisKey, Duration.ofDays(1));
            }
            return count != null && count > max;
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过限流: {}", e.getMessage());
            return false;
        }
    }

    private ConsentVO toConsentVO(M4PrivacyConsent c) {
        ConsentVO vo = new ConsentVO();
        vo.setId(c.getId());
        vo.setConsentType(c.getConsentType());
        vo.setGranted(c.getGranted());
        vo.setUpdateTime(c.getUpdateTime() != null ? c.getUpdateTime().toString() : null);
        return vo;
    }
    // ==================== 私有工具 ====================

    private void requireUser(String userId) {
        if (userService.getById(userId) == null) {
            throw new BusinessException(401, "登录已失效，请重新进入");
        }
    }

    private M4TimelineNode findMyNode(String userId, Long id) {
        M4TimelineNode node = nodeMapper.selectOne(new LambdaQueryWrapper<M4TimelineNode>()
                .eq(M4TimelineNode::getId, id)
                .eq(M4TimelineNode::getUserId, userId));
        if (node == null) {
            throw new BusinessException(404, "节点不存在");
        }
        return node;
    }

    private M4ShareHelp findHelpByToken(String token) {
        M4ShareHelp help = shareHelpMapper.selectOne(new LambdaQueryWrapper<M4ShareHelp>()
                .eq(M4ShareHelp::getShareToken, token));
        if (help == null) {
            throw new BusinessException(404, "分享链接不存在或已失效");
        }
        if (help.getExpiredAt() != null && help.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(410, "分享链接已过期");
        }
        if (help.getStatus() != null && help.getStatus() == 2) {
            throw new BusinessException(410, "分享已作废");
        }
        return help;
    }

    private String displayName(M4TimelineNode node) {
        if (StringUtils.hasText(node.getCustomName())) {
            return node.getCustomName();
        }
        return PRESET_STAGES.getOrDefault(node.getStage(), node.getStage());
    }

    private PhotoVO toPhotoVO(M4Photo p) {
        PhotoVO vo = new PhotoVO();
        vo.setId(p.getId());
        vo.setSource(p.getSource());
        vo.setUrl(p.getUrl());
        vo.setIsIllustration(p.getIsIllustration());
        vo.setEraTag(p.getEraTag());
        vo.setNodeId(p.getNodeId());
        vo.setEraId(p.getEraId());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }

    private StockImageVO toStockVO(M4StockImage s) {
        StockImageVO vo = new StockImageVO();
        vo.setId(s.getId());
        vo.setKeywords(s.getKeywords());
        vo.setEra(s.getEra());
        vo.setScene(s.getScene());
        vo.setUrl(s.getUrl());
        vo.setLicense(s.getLicense());
        return vo;
    }

    private StoryVO toStoryVO(M4Story s) {
        StoryVO vo = new StoryVO();
        vo.setId(s.getId());
        vo.setNodeId(s.getNodeId());
        vo.setEraId(s.getEraId());
        vo.setPhotoId(s.getPhotoId());
        vo.setTitle(s.getTitle());
        vo.setOriginalText(CryptoTypeHandler.decrypt(s.getOriginalText()));
        vo.setPolishedText(CryptoTypeHandler.decrypt(s.getPolishedText()));
        vo.setSummary(s.getSummary());
        vo.setMood(s.getMood());
        vo.setMusic(s.getMusic());
        vo.setStatus(s.getStatus());
        vo.setCreateTime(s.getCreateTime());
        return vo;
    }

    private MomentVO toMomentVO(M4MomentCopy m) {
        MomentVO vo = new MomentVO();
        vo.setId(m.getId());
        vo.setPhotoId(m.getPhotoId());
        vo.setStoryId(m.getStoryId());
        vo.setContent(m.getContent());
        vo.setStyle(m.getStyle());
        vo.setSelected(m.getSelected());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }

    private NodeVO toNodeVO(M4TimelineNode n) {
        NodeVO vo = new NodeVO();
        vo.setId(n.getId());
        vo.setStage(n.getStage());
        vo.setDisplayName(displayName(n));
        vo.setCustomName(n.getCustomName());
        vo.setSortOrder(n.getSortOrder());
        vo.setYearFrom(n.getYearFrom());
        vo.setYearTo(n.getYearTo());
        return vo;
    }

    private EraVO toEraVO(M4Era e) {
        EraVO vo = new EraVO();
        vo.setId(e.getId());
        vo.setName(e.getName());
        vo.setCoverPhotoId(e.getCoverPhotoId());
        return vo;
    }

    private HelpDetailVO toHelpDetailVO(M4ShareHelp h) {
        HelpDetailVO vo = new HelpDetailVO();
        vo.setId(h.getId());
        vo.setNodeId(h.getNodeId());
        M4TimelineNode node = nodeMapper.selectById(h.getNodeId());
        vo.setNodeName(node != null ? displayName(node) : null);
        vo.setHelperNote(CryptoTypeHandler.decrypt(h.getHelperNote()));
        vo.setHelperPhotoUrl(h.getHelperPhotoUrl());
        vo.setExpiredAt(h.getExpiredAt());
        vo.setStatus(h.getStatus());
        return vo;
    }
}






