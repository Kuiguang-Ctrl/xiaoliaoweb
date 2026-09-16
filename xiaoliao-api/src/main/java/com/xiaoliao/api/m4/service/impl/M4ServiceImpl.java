package com.xiaoliao.api.m4.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xiaoliao.api.chat.DeepSeekChatService;
import com.xiaoliao.api.m4.dto.ConsentRequest;
import com.xiaoliao.api.m4.dto.EraRequest;
import com.xiaoliao.api.m4.dto.GenImageRequest;
import com.xiaoliao.api.m4.dto.HelpSubmitRequest;
import com.xiaoliao.api.m4.dto.MatchImageRequest;
import com.xiaoliao.api.m4.dto.MomentGenerateRequest;
import com.xiaoliao.api.m4.dto.MomentPhotoRequest;
import com.xiaoliao.api.m4.dto.NodeRequest;
import com.xiaoliao.api.m4.dto.PhotoUploadRequest;
import com.xiaoliao.api.m4.dto.StorySaveRequest;
import com.xiaoliao.api.m4.entity.M4Era;
import com.xiaoliao.api.m4.entity.M4PrivacyConsent;
import com.xiaoliao.api.m4.entity.M4MomentCopy;
import com.xiaoliao.api.m4.entity.M4Photo;
import com.xiaoliao.api.m4.entity.M4PlazaLike;
import com.xiaoliao.api.m4.entity.M4PlazaWork;
import com.xiaoliao.api.m4.entity.M4ShareHelp;
import com.xiaoliao.api.m4.entity.M4StockImage;
import com.xiaoliao.api.m4.entity.M4Story;
import com.xiaoliao.api.m4.entity.M4TimelineNode;
import com.xiaoliao.api.m4.entity.M4Video;
import com.xiaoliao.api.m4.mapper.M4EraMapper;
import com.xiaoliao.api.m4.mapper.M4PrivacyConsentMapper;
import com.xiaoliao.api.m4.mapper.M4MomentCopyMapper;
import com.xiaoliao.api.m4.mapper.M4PhotoMapper;
import com.xiaoliao.api.m4.mapper.M4PlazaLikeMapper;
import com.xiaoliao.api.m4.mapper.M4PlazaWorkMapper;
import com.xiaoliao.api.m4.mapper.M4ShareHelpMapper;
import com.xiaoliao.api.m4.mapper.M4StockImageMapper;
import com.xiaoliao.api.m4.mapper.M4StoryMapper;
import com.xiaoliao.api.m4.mapper.M4TimelineNodeMapper;
import com.xiaoliao.api.m4.mapper.M4VideoMapper;
import com.xiaoliao.api.m4.service.M4Service;
import com.xiaoliao.api.m4.image.ImageGenerator;
import com.xiaoliao.api.m4.video.M4VideoComposer;
import com.xiaoliao.api.m4.vo.ConsentVO;
import com.xiaoliao.api.m4.vo.EraVO;
import com.xiaoliao.api.m4.vo.ExportVO;
import com.xiaoliao.api.m4.vo.GardenVO;
import com.xiaoliao.api.m4.vo.HelpDetailVO;
import com.xiaoliao.api.m4.vo.MatchResultVO;
import com.xiaoliao.api.m4.vo.MomentVO;
import com.xiaoliao.api.m4.vo.MomentPhotoVO;
import com.xiaoliao.api.m4.vo.NodeVO;
import com.xiaoliao.api.m4.vo.PhotoVO;
import com.xiaoliao.api.m4.vo.PlazaFeedVO;
import com.xiaoliao.api.m4.vo.PlazaWorkVO;
import com.xiaoliao.api.m4.vo.ShareHelpVO;
import com.xiaoliao.api.m4.vo.StockImageVO;
import com.xiaoliao.api.m4.vo.StoryVO;
import com.xiaoliao.api.m4.vo.VideoVO;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.xiaoliao.api.user.UserService;
import com.xiaoliao.api.util.CryptoTypeHandler;
import com.xiaoliao.common.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * - 人生时光：预设七阶段节点链，首次访问自动建空节点（不带任何默认故事），可增删改；
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
    private final M4VideoMapper videoMapper;
    private final M4PlazaWorkMapper plazaWorkMapper;
    private final M4PlazaLikeMapper plazaLikeMapper;
    private final M4VideoComposer videoComposer;
    private final ImageGenerator imageGenerator;
    private final M4MomentCopyMapper momentMapper;
    private final M4ShareHelpMapper shareHelpMapper;
    private final M4StockImageMapper stockImageMapper;
    private final M4PrivacyConsentMapper consentMapper;
    private final UserService userService;
    private final DeepSeekChatService deepSeekChatService;
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    /** 默认展示名（含已下线预设，老数据仍可正常显示） */
    private static final Map<String, String> STAGE_LABELS = new LinkedHashMap<>();
    static {
        STAGE_LABELS.put("childhood", "出生与童年");
        STAGE_LABELS.put("school", "上学");
        STAGE_LABELS.put("work", "工作/当兵");
        STAGE_LABELS.put("marriage", "结婚成家");
        STAGE_LABELS.put("parenting", "生儿育女");
        STAGE_LABELS.put("grandchildren", "孙辈");
        STAGE_LABELS.put("retirement", "退休与现在");
    }

    /** 新建用户默认创建的阶段顺序（已去掉孙辈、退休与现在） */
    private static final List<String> PRESET_ORDER = Arrays.asList(
            "childhood", "school", "work", "marriage", "parenting");

    /** 人生时光节点名（前端叫法）→ 阶段 key */
    private static final Map<String, String> STAGE_NAME_TO_KEY = new HashMap<>();
    static {
        STAGE_NAME_TO_KEY.put("出生与童年", "childhood");
        STAGE_NAME_TO_KEY.put("我的童年", "childhood");
        STAGE_NAME_TO_KEY.put("童年", "childhood");
        STAGE_NAME_TO_KEY.put("上学", "school");
        STAGE_NAME_TO_KEY.put("上学那会儿", "school");
        STAGE_NAME_TO_KEY.put("工作/当兵", "work");
        STAGE_NAME_TO_KEY.put("参加工作", "work");
        STAGE_NAME_TO_KEY.put("结婚成家", "marriage");
        STAGE_NAME_TO_KEY.put("生儿育女", "parenting");
    }

    private static final int SHARE_HELP_VALID_DAYS = 7;

    /** Pexels 图库 API Key（生产用环境变量 XIAOLIAO_PEXELS_KEY；为空时不联网搜图） */
    @Value("${xiaoliao.pexels.key:}")
    private String pexelsKey;

    /** 作品照片落盘目录与访问前缀（与 /api/upload 一致） */
    @Value("${xiaoliao.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${xiaoliao.upload.base-url:http://localhost:8080}")
    private String uploadBaseUrl;

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
    private static final int PLAZA_FEED_LIMIT = 30;
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
    public Map<String, Object> genImage(String userId, GenImageRequest request) {
        requireUser(userId);
        String style = StringUtils.hasText(request.getStyle()) ? request.getStyle() : "old_photo";
        String url = imageGenerator.generate(request.getPrompt(), style);
        log.info("文生图成功: userId={}, style={}, url={}", userId, style, url);
        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("url", url);
        ret.put("style", style);
        return ret;
    }

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
    public List<StoryVO> listStories(String userId, Long nodeId, Long eraId) {
        requireUser(userId);
        LambdaQueryWrapper<M4Story> wrapper = new LambdaQueryWrapper<M4Story>()
                .eq(M4Story::getUserId, userId)
                .orderByDesc(M4Story::getCreateTime);
        if (nodeId != null) {
            wrapper.eq(M4Story::getNodeId, nodeId);
        }
        if (eraId != null) {
            wrapper.eq(M4Story::getEraId, eraId);
        }
        return storyMapper.selectList(wrapper).stream().map(this::toStoryVO).collect(Collectors.toList());
    }

    @Override
    public void deleteStory(String userId, Long id) {
        requireUser(userId);
        M4Story story = storyMapper.selectOne(new LambdaQueryWrapper<M4Story>()
                .eq(M4Story::getId, id)
                .eq(M4Story::getUserId, userId));
        if (story == null) {
            throw new BusinessException(404, "故事不存在");
        }
        storyMapper.deleteById(id);
        log.info("故事删除成功: userId={}, id={}", userId, id);
    }

    @Override
    public Map<String, Object> saveVideo(String userId, String destKind, Long nodeId, String nodeLabel, String eraName,
                                         String title, String caption,
                                         List<MultipartFile> photos, List<String> captions, MultipartFile bgm) {
        requireUser(userId);
        // 按原下标过滤非空照片，字幕与照片按同一原始下标对齐，避免空槽位导致字幕错位
        List<MultipartFile> files = new ArrayList<>();
        List<Integer> keptOrig = new ArrayList<>();
        if (photos != null) {
            for (int i = 0; i < photos.size(); i++) {
                MultipartFile f = photos.get(i);
                if (f != null && !f.isEmpty()) {
                    files.add(f);
                    keptOrig.add(i);
                }
            }
        }
        if (files.isEmpty()) {
            throw new BusinessException(400, "请先传照片（1~5 张）");
        }
        if (files.size() > 5) {
            throw new BusinessException(400, "最多 5 张照片");
        }
        List<String> capList = new ArrayList<>();
        for (int orig : keptOrig) {
            capList.add(captions != null && orig < captions.size() && captions.get(orig) != null ? captions.get(orig) : "");
        }
        // 先合成、成功后再建 holder：ffmpeg 失败不残留刚建的空自定义节点/年代
        M4VideoComposer.Result r = videoComposer.compose(files, capList, bgm);
        Map.Entry<Long, Long> place = resolvePlacement(userId, destKind, nodeId, nodeLabel, eraName);

        M4Video v = new M4Video();
        v.setUserId(userId);
        v.setNodeId(place.getKey());
        v.setEraId(place.getValue());
        v.setTitle(clipText(title, 64));
        v.setCaption(clipText(caption, 5000));
        v.setVideoUrl(r.videoUrl());
        v.setPosterUrl(r.posterUrl());
        v.setDuration(r.duration());
        v.setCreateTime(LocalDateTime.now());
        v.setUpdateTime(LocalDateTime.now());
        videoMapper.insert(v);
        log.info("视频作品保存成功: userId={}, videoId={}, nodeId={}, eraId={}", userId, v.getId(), place.getKey(), place.getValue());

        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("videoId", v.getId());
        ret.put("title", v.getTitle());
        ret.put("videoUrl", v.getVideoUrl());
        ret.put("posterUrl", v.getPosterUrl());
        ret.put("duration", v.getDuration());
        if (place.getKey() != null) ret.put("nodeId", place.getKey());
        if (place.getValue() != null) ret.put("eraId", place.getValue());
        return ret;
    }

    @Override
    public List<VideoVO> listVideos(String userId, Long nodeId, Long eraId) {
        requireUser(userId);
        LambdaQueryWrapper<M4Video> qw = new LambdaQueryWrapper<M4Video>()
                .eq(M4Video::getUserId, userId)
                .orderByDesc(M4Video::getCreateTime);
        if (nodeId != null) qw.eq(M4Video::getNodeId, nodeId);
        if (eraId != null) qw.eq(M4Video::getEraId, eraId);
        return videoMapper.selectList(qw).stream().map(this::toVideoVO).collect(Collectors.toList());
    }

    @Override
    public VideoVO getVideo(String userId, Long id) {
        requireUser(userId);
        M4Video v = videoMapper.selectOne(new LambdaQueryWrapper<M4Video>()
                .eq(M4Video::getId, id)
                .eq(M4Video::getUserId, userId));
        if (v == null) {
            throw new BusinessException(404, "视频不存在");
        }
        return toVideoVO(v);
    }

    @Override
    public void deleteVideo(String userId, Long id) {
        requireUser(userId);
        M4Video v = videoMapper.selectOne(new LambdaQueryWrapper<M4Video>()
                .eq(M4Video::getId, id)
                .eq(M4Video::getUserId, userId));
        if (v == null) {
            throw new BusinessException(404, "视频不存在");
        }
        videoMapper.deleteById(id);
        // 作品从「我的作品」里删掉后，广场上那条也一起撤下，免得点开是空视频
        plazaWorkMapper.delete(new LambdaQueryWrapper<M4PlazaWork>().eq(M4PlazaWork::getVideoId, id));
        log.info("视频作品删除: userId={}, id={}", userId, id);
    }

    // ==================== 广场 ====================

    @Override
    public PlazaFeedVO plazaFeed(String userId, Integer limit) {
        requireUser(userId);
        int cap = (limit == null || limit <= 0) ? PLAZA_FEED_LIMIT : Math.min(limit, 60);
        List<M4PlazaWork> works = plazaWorkMapper.selectList(new LambdaQueryWrapper<M4PlazaWork>()
                .orderByDesc(M4PlazaWork::getCreateTime)
                .orderByDesc(M4PlazaWork::getId)
                .last("LIMIT " + cap));

        Set<String> ownerIds = new HashSet<>();
        List<Long> workIds = new ArrayList<>();
        for (M4PlazaWork w : works) {
            ownerIds.add(w.getUserId());
            workIds.add(w.getId());
        }
        Set<Long> likedIds = new HashSet<>();
        if (!workIds.isEmpty()) {
            plazaLikeMapper.selectList(new LambdaQueryWrapper<M4PlazaLike>()
                            .eq(M4PlazaLike::getUserId, userId)
                            .in(M4PlazaLike::getWorkId, workIds))
                    .forEach(l -> likedIds.add(l.getWorkId()));
        }
        Map<String, String> names = userService.nicknameMap(ownerIds);

        PlazaFeedVO feed = new PlazaFeedVO();
        feed.setWorks(works.stream()
                .map(w -> toPlazaWorkVO(w, names.get(w.getUserId()), userId, likedIds.contains(w.getId())))
                .collect(Collectors.toList()));
        feed.setTotal(plazaWorkMapper.selectCount(null));

        List<M4PlazaWork> mine = plazaWorkMapper.selectList(new LambdaQueryWrapper<M4PlazaWork>()
                .eq(M4PlazaWork::getUserId, userId));
        feed.setMyWorkCount((long) mine.size());
        feed.setReceivedLikes(mine.stream()
                .mapToInt(w -> w.getLikeCount() == null ? 0 : w.getLikeCount()).sum());
        return feed;
    }

    @Override
    public PlazaWorkVO publishPlazaWork(String userId, Long videoId, String title) {
        requireUser(userId);
        M4Video v = videoMapper.selectOne(new LambdaQueryWrapper<M4Video>()
                .eq(M4Video::getId, videoId)
                .eq(M4Video::getUserId, userId));
        if (v == null) {
            throw new BusinessException(404, "这件作品找不到了，先做好一支视频再来广场");
        }
        String ownerName = userService.nicknameMap(List.of(userId)).get(userId);
        M4PlazaWork exist = plazaWorkMapper.selectOne(new LambdaQueryWrapper<M4PlazaWork>()
                .eq(M4PlazaWork::getVideoId, videoId));
        if (exist != null) {
            return toPlazaWorkVO(exist, ownerName, userId, hasLikedPlaza(userId, exist.getId()));
        }
        String t = StringUtils.hasText(title) ? title : v.getTitle();
        M4PlazaWork w = new M4PlazaWork();
        w.setUserId(userId);
        w.setVideoId(videoId);
        w.setTitle(clipText(StringUtils.hasText(t) ? t : "时光里的故事", 64));
        w.setVideoUrl(v.getVideoUrl());
        w.setCoverUrl(v.getPosterUrl());
        w.setDuration(v.getDuration() == null ? 0 : v.getDuration());
        w.setLikeCount(0);
        w.setCreateTime(LocalDateTime.now());
        w.setUpdateTime(LocalDateTime.now());
        try {
            plazaWorkMapper.insert(w);
        } catch (DuplicateKeyException e) {
            // 并发重复发布：复用已经挂上去的那条
            w = plazaWorkMapper.selectOne(new LambdaQueryWrapper<M4PlazaWork>()
                    .eq(M4PlazaWork::getVideoId, videoId));
        }
        log.info("作品发布到广场: userId={}, plazaWorkId={}, videoId={}", userId, w.getId(), videoId);
        return toPlazaWorkVO(w, ownerName, userId, false);
    }

    @Override
    public void unpublishPlazaWork(String userId, Long workId) {
        requireUser(userId);
        M4PlazaWork w = plazaWorkMapper.selectOne(new LambdaQueryWrapper<M4PlazaWork>()
                .eq(M4PlazaWork::getId, workId)
                .eq(M4PlazaWork::getUserId, userId));
        if (w == null) {
            throw new BusinessException(404, "广场上没找到这件作品");
        }
        plazaWorkMapper.deleteById(workId);
        log.info("广场作品撤下: userId={}, plazaWorkId={}", userId, workId);
    }

    @Override
    public PlazaWorkVO togglePlazaLike(String userId, Long workId) {
        requireUser(userId);
        M4PlazaWork w = plazaWorkMapper.selectById(workId);
        if (w == null) {
            throw new BusinessException(404, "这件作品已经从广场撤下了");
        }
        M4PlazaLike like = plazaLikeMapper.selectOne(new LambdaQueryWrapper<M4PlazaLike>()
                .eq(M4PlazaLike::getWorkId, workId)
                .eq(M4PlazaLike::getUserId, userId));
        boolean liked;
        if (like != null) {
            plazaLikeMapper.deleteById(like.getId());
            bumpPlazaLikeCount(workId, -1);
            liked = false;
        } else {
            M4PlazaLike row = new M4PlazaLike();
            row.setWorkId(workId);
            row.setUserId(userId);
            row.setCreateTime(LocalDateTime.now());
            try {
                plazaLikeMapper.insert(row);
                bumpPlazaLikeCount(workId, 1);
            } catch (DuplicateKeyException e) {
                // 并发重复点赞：当作已经点过
            }
            liked = true;
        }
        M4PlazaWork fresh = plazaWorkMapper.selectById(workId);
        String ownerName = userService.nicknameMap(List.of(fresh.getUserId())).get(fresh.getUserId());
        return toPlazaWorkVO(fresh, ownerName, userId, liked);
    }

    /** 点赞数增减（SQL 原子自增，避免并发覆盖；减到 0 为止） */
    private void bumpPlazaLikeCount(Long workId, int delta) {
        plazaWorkMapper.update(null, new LambdaUpdateWrapper<M4PlazaWork>()
                .eq(M4PlazaWork::getId, workId)
                .setSql(delta > 0 ? "like_count = like_count + 1" : "like_count = GREATEST(like_count - 1, 0)"));
    }

    private boolean hasLikedPlaza(String userId, Long workId) {
        return plazaLikeMapper.selectCount(new LambdaQueryWrapper<M4PlazaLike>()
                .eq(M4PlazaLike::getWorkId, workId)
                .eq(M4PlazaLike::getUserId, userId)) > 0;
    }

    private PlazaWorkVO toPlazaWorkVO(M4PlazaWork w, String ownerName, String viewerId, boolean liked) {
        PlazaWorkVO vo = new PlazaWorkVO();
        vo.setId(w.getId());
        vo.setVideoId(w.getVideoId());
        vo.setTitle(w.getTitle());
        vo.setVideoUrl(w.getVideoUrl());
        vo.setCoverUrl(w.getCoverUrl());
        vo.setDuration(w.getDuration());
        vo.setOwnerId(w.getUserId());
        vo.setOwnerName(ownerName);
        vo.setMine(viewerId != null && viewerId.equals(w.getUserId()));
        vo.setLikeCount(w.getLikeCount() == null ? 0 : w.getLikeCount());
        vo.setLiked(liked);
        vo.setCreateTime(w.getCreateTime());
        return vo;
    }

    @Override
    public Map<String, Object> saveWork(String userId, String destKind, Long nodeId, String nodeLabel, String eraName,
                                        String title, String text, String music,
                                        List<MultipartFile> photos) {
        requireUser(userId);
        String body = text == null ? "" : text.trim();
        if (!StringUtils.hasText(body)) {
            throw new BusinessException(400, "故事内容不能为空");
        }
        List<MultipartFile> files = new ArrayList<>();
        if (photos != null) {
            for (MultipartFile f : photos) {
                if (f != null && !f.isEmpty()) files.add(f);
            }
        }
        if (files.isEmpty()) {
            throw new BusinessException(400, "请先传照片");
        }

        Long holderNodeId = null;
        Long holderEraId = null;
        if ("life".equals(destKind)) {
            ensurePresetNodes(userId);
            M4TimelineNode node = null;
            if (nodeId != null) {
                // 前端“人生时光”选择的是真实存在的节点：按 id 精确挂载，绝不复活已删除的节点
                node = nodeMapper.selectOne(new LambdaQueryWrapper<M4TimelineNode>()
                        .eq(M4TimelineNode::getId, nodeId)
                        .eq(M4TimelineNode::getUserId, userId));
                if (node == null) {
                    throw new BusinessException(400, "这一段人生时光已经不存在了，请重新选一段");
                }
            } else {
                String label = StringUtils.hasText(nodeLabel) ? nodeLabel.trim() : null;
                String stage = StringUtils.hasText(label) ? STAGE_NAME_TO_KEY.get(label) : null;
                if (stage != null) {
                    // 预设阶段：只挂到现存节点，找不到说明该段已被删除，不自动重建
                    node = nodeMapper.selectOne(new LambdaQueryWrapper<M4TimelineNode>()
                            .eq(M4TimelineNode::getUserId, userId)
                            .eq(M4TimelineNode::getStage, stage)
                            .orderByAsc(M4TimelineNode::getSortOrder)
                            .last("limit 1"));
                    if (node == null) {
                        throw new BusinessException(400, "这一段人生时光已经删掉了，请重新选一段");
                    }
                } else {
                    // 自定义段：按名称精确找现存节点，找不到才新建（避免挂错到别的自定义段）
                    String customName = StringUtils.hasText(label) ? label : "自定义";
                    node = nodeMapper.selectOne(new LambdaQueryWrapper<M4TimelineNode>()
                            .eq(M4TimelineNode::getUserId, userId)
                            .eq(M4TimelineNode::getStage, "custom")
                            .eq(M4TimelineNode::getCustomName, customName)
                            .last("limit 1"));
                    if (node == null) {
                        node = new M4TimelineNode();
                        node.setUserId(userId);
                        node.setStage("custom");
                        node.setCustomName(customName);
                        Long cnt = nodeMapper.selectCount(new LambdaQueryWrapper<M4TimelineNode>()
                                .eq(M4TimelineNode::getUserId, userId));
                        node.setSortOrder(cnt == null ? 0 : cnt.intValue());
                        node.setCreateTime(LocalDateTime.now());
                        node.setUpdateTime(LocalDateTime.now());
                        nodeMapper.insert(node);
                        log.info("作品自动建节点: userId={}, stage=custom, customName={}", userId, customName);
                    }
                }
            }
            holderNodeId = node.getId();
        } else if ("era".equals(destKind)) {
            String name = StringUtils.hasText(eraName) ? eraName.trim() : "那段时光";
            M4Era era = eraMapper.selectOne(new LambdaQueryWrapper<M4Era>()
                    .eq(M4Era::getUserId, userId)
                    .eq(M4Era::getName, name));
            if (era == null) {
                era = new M4Era();
                era.setUserId(userId);
                era.setName(name);
                era.setCreateTime(LocalDateTime.now());
                era.setUpdateTime(LocalDateTime.now());
                eraMapper.insert(era);
                log.info("作品自动建年代: userId={}, name={}", userId, name);
            }
            holderEraId = era.getId();
        } else {
            throw new BusinessException(400, "请选择收进人生时光还是年代记忆");
        }

        List<Long> photoIds = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
                String ext = original.contains(".")
                        ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT)
                        : "jpg";
                if (!("jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext)
                        || "webp".equals(ext) || "gif".equals(ext))) {
                    continue;
                }
                if (file.getSize() > 10L * 1024 * 1024) {
                    continue;
                }
                String sub = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
                java.nio.file.Path dir = java.nio.file.Paths.get(uploadDir, sub).toAbsolutePath().normalize();
                java.nio.file.Files.createDirectories(dir);
                String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
                file.transferTo(dir.resolve(name).toFile());
                String url = uploadBaseUrl.replaceAll("/+$", "") + "/uploads/" + sub + "/" + name;
                M4Photo ph = new M4Photo();
                ph.setUserId(userId);
                ph.setSource("user");
                ph.setUrl(url);
                ph.setIsIllustration(0);
                ph.setNodeId(holderNodeId);
                ph.setEraId(holderEraId);
                ph.setCreateTime(LocalDateTime.now());
                ph.setUpdateTime(LocalDateTime.now());
                photoMapper.insert(ph);
                photoIds.add(ph.getId());
                log.info("作品照片已存: userId={}, photoId={}, nodeId={}, eraId={}", userId, ph.getId(), holderNodeId, holderEraId);
            } catch (Exception e) {
                log.warn("作品照片保存失败: userId={}, err={}", userId, e.getMessage());
            }
        }
        if (photoIds.isEmpty()) {
            throw new BusinessException(500, "照片保存失败，请重试");
        }

        M4Story story = new M4Story();
        story.setUserId(userId);
        story.setNodeId(holderNodeId);
        story.setEraId(holderEraId);
        story.setPhotoId(photoIds.get(0));
        story.setTitle(StringUtils.hasText(title) ? title.trim().substring(0, Math.min(title.trim().length(), 64)) : null);
        story.setOriginalText(CryptoTypeHandler.encrypt(body));
        story.setSummary("回忆视频");
        story.setMusic(StringUtils.hasText(music) ? music.trim() : null);
        story.setStatus(1);
        story.setCreateTime(LocalDateTime.now());
        story.setUpdateTime(LocalDateTime.now());
        storyMapper.insert(story);
        log.info("回忆作品保存成功: userId={}, storyId={}, nodeId={}, eraId={}", userId, story.getId(), holderNodeId, holderEraId);

        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("storyId", story.getId());
        ret.put("title", story.getTitle());
        if (holderNodeId != null) ret.put("nodeId", holderNodeId);
        if (holderEraId != null) ret.put("eraId", holderEraId);
        return ret;
    }

    // ==================== 朋友圈文案 ====================

    @Override
    public List<MomentVO> generateMoments(String userId, MomentGenerateRequest request) {
        requireUser(userId);
        if (overDailyLimit("moments:" + userId, DAILY_MOMENT_LIMIT)) {
            throw new BusinessException(429, "今天已经写过很多次啦，明天再来吧～");
        }
        String style = StringUtils.hasText(request.getStyle()) ? request.getStyle() : "simple";
        // 故事润色/原文 → 优先请 AI 按老人自己的话改写成朋友圈文案；AI 不可用再退回规则框架
        String storyText = null;
        String storyTitle = null;
        if (request.getStoryId() != null) {
            M4Story story = storyMapper.selectById(request.getStoryId());
            if (story != null && userId.equals(story.getUserId())) {
                storyText = storyPlainText(story);
                storyTitle = story.getTitle();
            }
        }
        // 没有 storyId 也可以直接给一段描述（发照片写朋友圈等场景）
        if (!StringUtils.hasText(storyText) && StringUtils.hasText(request.getDescription())) {
            storyText = request.getDescription().trim();
        }
        List<String> templates = new ArrayList<>();
        if (StringUtils.hasText(storyText)) {
            List<String> fromAi = aiMomentCopies(style, storyText, storyTitle, request.getPhotoId() != null);
            if (fromAi != null) {
                templates.addAll(fromAi);
            }
        }
        // 不足 3 条（AI 未配置/超时/少写）时，用老人自己的话按规则框架补齐
        if (templates.size() < 3) {
            List<String> fallback = StringUtils.hasText(storyText)
                    ? momentCopyForStory(style, storyText)
                    : Arrays.asList(momentTemplates(style));
            if (fallback != null) {
                for (String f : fallback) {
                    if (templates.size() >= 3) {
                        break;
                    }
                    if (!templates.contains(f)) {
                        templates.add(f);
                    }
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

    @Override
    public MomentPhotoVO generateMomentsFromPhoto(String userId, MomentPhotoRequest request) {
        requireUser(userId);
        // 1) 照片：给了 photoId 直接用（校验归属）；只给 url 则先入照片库
        Long photoId = request.getPhotoId();
        String photoUrl = StringUtils.hasText(request.getPhotoUrl()) ? request.getPhotoUrl().trim() : null;
        if (photoId != null) {
            M4Photo photo = photoMapper.selectOne(new LambdaQueryWrapper<M4Photo>()
                    .eq(M4Photo::getId, photoId)
                    .eq(M4Photo::getUserId, userId));
            if (photo == null) {
                throw new BusinessException(404, "这张照片找不到啦");
            }
            if (photoUrl == null) {
                photoUrl = photo.getUrl();
            }
        } else if (photoUrl != null) {
            PhotoUploadRequest photoReq = new PhotoUploadRequest();
            photoReq.setUrl(photoUrl);
            photoReq.setSource("user");
            photoReq.setNodeId(request.getNodeId());
            photoReq.setEraId(request.getEraId());
            photoId = savePhoto(userId, photoReq).getId();
        }

        // 2) 把老人的描述收成一段故事（只想要文案的页面可传 saveStory=false）
        Long storyId = null;
        if (request.getSaveStory() == null || request.getSaveStory()) {
            StorySaveRequest storyReq = new StorySaveRequest();
            storyReq.setNodeId(request.getNodeId());
            storyReq.setEraId(request.getEraId());
            storyReq.setPhotoId(photoId);
            storyReq.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : "照片里的故事");
            storyReq.setOriginalText(request.getDescription().trim());
            storyReq.setPolishedText(request.getDescription().trim());
            storyReq.setMood("happy");
            storyId = saveStory(userId, storyReq).getId();
        }

        // 3) 文案：AI 按老人自己的话改写（不可用自动规则兜底），3 条候选落 m4_moment_copy
        MomentGenerateRequest genReq = new MomentGenerateRequest();
        genReq.setStoryId(storyId);
        genReq.setPhotoId(photoId);
        genReq.setStyle(request.getStyle());
        genReq.setDescription(request.getDescription().trim());
        List<MomentVO> moments = generateMoments(userId, genReq);

        MomentPhotoVO vo = new MomentPhotoVO();
        vo.setPhotoId(photoId);
        vo.setPhotoUrl(photoUrl);
        vo.setStoryId(storyId);
        vo.setMoments(moments);
        log.info("照片朋友圈文案: userId={}, photoId={}, storyId={}, count={}", userId, photoId, storyId, moments.size());
        return vo;
    }

    /** 解密故事文本（优先润色版，其次原文），并去掉外层引号 */
    private String storyPlainText(M4Story story) {
        String content = stripOuterQuotes(CryptoTypeHandler.decrypt(story.getPolishedText()));
        if (!StringUtils.hasText(content)) {
            content = stripOuterQuotes(CryptoTypeHandler.decrypt(story.getOriginalText()));
        }
        return StringUtils.hasText(content) ? content.trim() : null;
    }

    /**
     * 请 AI 把老人的原话改写成 3 条可以复制分享给家人的文案。
     * 只依据老人说过的内容（不编造）；不可用/失败时返回 null，由调用方走规则兜底。
     */
    private List<String> aiMomentCopies(String style, String storyText, String title, boolean withPhoto) {
        String styleLabel;
        switch (style == null ? "" : style) {
            case "warm":
                styleLabel = "温暖";
                break;
            case "humorous":
                styleLabel = "幽默";
                break;
            case "proud":
                styleLabel = "自豪";
                break;
            default:
                styleLabel = "朴实";
                break;
        }
        String system = "你是\"小辽\"，帮老人把一段口述回忆改写成可以复制下来发给家人朋友的一段话。"
                + "要求：第一人称、口语化短句、温暖朴实；只依据老人说过的内容，绝不编造细节；"
                + "每条不超过 90 字；最多用一个 🌷 或 🌼 之类的小表情；"
                + "严格输出 3 条，每条单独一行，行首用 1. 2. 3.，不要任何解释、标题或前后缀。";
        StringBuilder user = new StringBuilder();
        user.append("老人喜欢的风格：").append(styleLabel).append('\n');
        if (StringUtils.hasText(title)) {
            user.append("这段回忆的标题：").append(title).append('\n');
        }
        if (withPhoto) {
            user.append("这段话会配一张老照片，可以自然地提一句照片。\n");
        }
        user.append("老人说：\n").append(storyText);
        try {
            String out = deepSeekChatService.chat(user.toString(), null, system, 700, 0.8);
            List<String> list = new ArrayList<>();
            for (String line : out.split("\\r?\\n")) {
                String t = line.trim()
                        .replaceFirst("^[0-9１-９]{1,2}\\s*[.、)）:：]\\s*", "")
                        .replaceFirst("^[①②③④⑤]\\s*", "")
                        .trim();
                if (StringUtils.hasText(t) && !list.contains(t)) {
                    list.add(t);
                }
                if (list.size() >= 3) {
                    break;
                }
            }
            if (!list.isEmpty()) {
                log.info("朋友圈文案 AI 生成: style={}, count={}", style, list.size());
                return list;
            }
        } catch (Exception e) {
            log.warn("朋友圈文案 AI 生成失败，改走规则兜底: {}", e.getMessage());
        }
        return null;
    }

    /** 依据老人自己的话拼出 3 条朋友圈文案；无文本可引用时返回 null（走规则模板兜底） */
    private List<String> momentCopyForStory(String style, String rawContent) {
        String content = StringUtils.hasText(rawContent) ? rawContent.trim() : null;
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
            vo.setVideoCount(videoMapper.selectCount(new LambdaQueryWrapper<M4Video>()
                    .eq(M4Video::getUserId, userId)
                    .eq(M4Video::getNodeId, node.getId())));
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
        // 该节点下的故事、照片与视频一并清除（逻辑删除），避免留下查不到的孤儿数据
        storyMapper.delete(new LambdaQueryWrapper<M4Story>()
                .eq(M4Story::getUserId, userId)
                .eq(M4Story::getNodeId, id));
        photoMapper.delete(new LambdaQueryWrapper<M4Photo>()
                .eq(M4Photo::getUserId, userId)
                .eq(M4Photo::getNodeId, id));
        videoMapper.delete(new LambdaQueryWrapper<M4Video>()
                .eq(M4Video::getUserId, userId)
                .eq(M4Video::getNodeId, id));
        nodeMapper.deleteById(id);
    }

    /** 首次访问自动创建预设七阶段空节点（不插入任何默认故事/照片，故事数默认 0） */
    private void ensurePresetNodes(String userId) {
        Long count = nodeMapper.selectCount(new LambdaQueryWrapper<M4TimelineNode>()
                .eq(M4TimelineNode::getUserId, userId));
        if (count > 0) {
            return;
        }
        int sort = 0;
        for (String stage : PRESET_ORDER) {
            M4TimelineNode node = new M4TimelineNode();
            node.setUserId(userId);
            node.setStage(stage);
            node.setSortOrder(sort++);
            node.setCreateTime(LocalDateTime.now());
            node.setUpdateTime(LocalDateTime.now());
            nodeMapper.insert(node);
        }
        log.info("预设人生阶段创建(默认0故事): userId={}", userId);
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
                    vo.setVideoCount(videoMapper.selectCount(new LambdaQueryWrapper<M4Video>()
                            .eq(M4Video::getUserId, userId)
                            .eq(M4Video::getEraId, era.getId())));
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
        vo.setVideoCount(videoMapper.selectCount(new LambdaQueryWrapper<M4Video>()
                .eq(M4Video::getUserId, userId)));
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
        vo.setStories(listStories(userId, null, null));
        vo.setEras(listEras(userId));
        vo.setPhotos(listPhotos(userId));
        vo.setMoments(listMoments(userId));
        vo.setConsents(listConsents(userId));
        vo.setVideos(listVideos(userId, null, null));
        String myName = userService.nicknameMap(List.of(userId)).get(userId);
        vo.setPlazaWorks(plazaWorkMapper.selectList(new LambdaQueryWrapper<M4PlazaWork>()
                        .eq(M4PlazaWork::getUserId, userId)
                        .orderByDesc(M4PlazaWork::getCreateTime))
                .stream().map(w -> toPlazaWorkVO(w, myName, userId, false)).collect(Collectors.toList()));
        log.info("数据导出: userId={}", userId);
        return vo;
    }

    @Override
    public Map<String, Object> deleteAllData(String userId) {
        requireUser(userId);
        Map<String, Object> result = new HashMap<>();
        String[] tables = {"m4_story", "m4_photo", "m4_video", "m4_moment_copy", "m4_timeline_node",
                "m4_era", "m4_share_help", "m4_privacy_consent", "m4_plaza_work", "m4_plaza_like"};
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

    /** 解析收件位置：返回 holderNodeId / holderEraId（复制 saveWork 同规则，便于后续删除旧 works 后无回归） */
    private Map.Entry<Long, Long> resolvePlacement(String userId, String destKind, Long nodeId, String nodeLabel, String eraName) {
        Long holderNodeId = null;
        Long holderEraId = null;
        if ("life".equals(destKind)) {
            ensurePresetNodes(userId);
            M4TimelineNode node;
            if (nodeId != null) {
                node = nodeMapper.selectOne(new LambdaQueryWrapper<M4TimelineNode>()
                        .eq(M4TimelineNode::getId, nodeId)
                        .eq(M4TimelineNode::getUserId, userId));
                if (node == null) {
                    throw new BusinessException(400, "这一段人生时光已经不存在了，请重新选一段");
                }
            } else {
                String label = StringUtils.hasText(nodeLabel) ? nodeLabel.trim() : null;
                String stage = StringUtils.hasText(label) ? STAGE_NAME_TO_KEY.get(label) : null;
                if (stage != null) {
                    node = nodeMapper.selectOne(new LambdaQueryWrapper<M4TimelineNode>()
                            .eq(M4TimelineNode::getUserId, userId)
                            .eq(M4TimelineNode::getStage, stage)
                            .orderByAsc(M4TimelineNode::getSortOrder)
                            .last("limit 1"));
                    if (node == null) {
                        throw new BusinessException(400, "这一段人生时光已经删掉了，请重新选一段");
                    }
                } else {
                    String customName = StringUtils.hasText(label) ? label : "自定义";
                    node = nodeMapper.selectOne(new LambdaQueryWrapper<M4TimelineNode>()
                            .eq(M4TimelineNode::getUserId, userId)
                            .eq(M4TimelineNode::getStage, "custom")
                            .eq(M4TimelineNode::getCustomName, customName)
                            .last("limit 1"));
                    if (node == null) {
                        node = new M4TimelineNode();
                        node.setUserId(userId);
                        node.setStage("custom");
                        node.setCustomName(customName);
                        Long cnt = nodeMapper.selectCount(new LambdaQueryWrapper<M4TimelineNode>()
                                .eq(M4TimelineNode::getUserId, userId));
                        node.setSortOrder(cnt == null ? 0 : cnt.intValue());
                        node.setCreateTime(LocalDateTime.now());
                        node.setUpdateTime(LocalDateTime.now());
                        nodeMapper.insert(node);
                    }
                }
            }
            holderNodeId = node.getId();
        } else if ("era".equals(destKind)) {
            String name = StringUtils.hasText(eraName) ? eraName.trim() : "那段时光";
            M4Era era = eraMapper.selectOne(new LambdaQueryWrapper<M4Era>()
                    .eq(M4Era::getUserId, userId)
                    .eq(M4Era::getName, name));
            if (era == null) {
                era = new M4Era();
                era.setUserId(userId);
                era.setName(name);
                era.setCreateTime(LocalDateTime.now());
                era.setUpdateTime(LocalDateTime.now());
                eraMapper.insert(era);
            }
            holderEraId = era.getId();
        } else {
            throw new BusinessException(400, "请选择收进人生时光还是年代记忆");
        }
        return new java.util.AbstractMap.SimpleEntry<>(holderNodeId, holderEraId);
    }

    private VideoVO toVideoVO(M4Video v) {
        VideoVO vo = new VideoVO();
        vo.setId(v.getId());
        vo.setNodeId(v.getNodeId());
        vo.setEraId(v.getEraId());
        vo.setTitle(v.getTitle());
        vo.setCaption(v.getCaption());
        vo.setVideoUrl(v.getVideoUrl());
        vo.setPosterUrl(v.getPosterUrl());
        vo.setDuration(v.getDuration());
        vo.setCreateTime(v.getCreateTime());
        return vo;
    }

    /** 长文本清洗：trim 一次后按码点截断（避免拆散代理对/多字节字符）；空文本返回 null */
    private String clipText(String raw, int maxCodePoints) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.codePointCount(0, t.length()) <= maxCodePoints
                ? t
                : t.substring(0, t.offsetByCodePoints(0, maxCodePoints));
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
        return STAGE_LABELS.getOrDefault(node.getStage(), node.getStage());
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






