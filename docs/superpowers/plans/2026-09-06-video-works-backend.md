# 视频作品后端（m4_video + ffmpeg 合成）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 后端新增"视频作品"能力：接收对话端上传的素材包（≤5 张照片 + 每张字幕 + 标题 + 文案 + 可选配乐音频），用 ffmpeg 合成 720p mp4（字幕烧录、配乐混音），入库 `m4_video` 并挂到人生时光节点/年代记忆；节点/年代/花园计数改为视频数。

**Architecture:** 设计文档 `docs/superpowers/specs/2026-09-06-video-works-only-design.md`。新增 Flyway V10 建表 → 实体/Mapper/VO → 独立的 `M4VideoComposer`（ProcessBuilder 调 ffmpeg，逐照片成段再 concat 混音）→ `M4Service`/Impl 新方法（位置挂载规则复用现有 works 逻辑）→ Controller `/api/v1/m4/videos` 四个端点 → timeline/listEras/garden 计数切到 m4_video。旧的 /works、/stories、m4_story 等保留不删（页面停用，后续清理）。

**Tech Stack:** Java 17 / Spring Boot 3.2 / MyBatis-Plus / PostgreSQL 16 (Flyway) / 本机 ffmpeg（`brew install ffmpeg`，macOS arm64）

**关键路径（一律相对仓库根 `xiaoliaoweb-src-2026-09-06/`）**
- SQL：`xiaoliao-api/src/main/resources/db/migration/V10__m4_video.sql`
- 新代码：`xiaoliao-api/src/main/java/com/xiaoliao/api/m4/{entity/M4Video.java, mapper/M4VideoMapper.java, vo/VideoVO.java, video/M4VideoComposer.java}`
- 改：`.../m4/service/M4Service.java`、`.../m4/service/impl/M4ServiceImpl.java`、`.../m4/controller/M4Controller.java`、`.../m4/vo/{NodeVO,EraVO,GardenVO}.java`
- 环境：本机 macOS，后端跑在 8080（spring-boot:run 后台任务），PostgreSQL/Redis 在 docker（xiaoliao-pg / xiaoliao-redis），Maven 本地仓库参数 `-Dmaven.repo.local=/Users/robot/Downloads/DSH/.m2repo`，JDK：`export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
- 验证用 mock 用户头：`curl -H "X-Mock-User-Id: <uid>"`；dev 下 auth.enabled=false（见 `application-dev.yml`）
- ffmpeg 中文字体（本机已存在）：`/System/Library/Fonts/STHeiti Light.ttc`（composer 会探测多个候选）

---

### Task A0: 安装并冒烟验证 ffmpeg

**Files:** 无（环境）

- [ ] **Step 1: 安装 ffmpeg**

```bash
brew install ffmpeg
```

- [ ] **Step 2: 验证命令与字体**

```bash
ffmpeg -version | head -1
ls "/System/Library/Fonts/STHeiti Light.ttc"
```

Expected: `ffmpeg version ...` 正常输出；字体文件存在。

- [ ] **Step 3: 用一张测试图冒烟验证 drawtext 中文可用（可选但强烈建议）**

```bash
cd /tmp && python3 -c "
import base64
open('smoke.jpg','wb').write(base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='))"
printf '字幕冒烟测试' > /tmp/cap.txt
ffmpeg -y -loop 1 -framerate 25 -t 1 -i /tmp/smoke.jpg -vf "scale=320:240,drawtext=fontfile=/System/Library/Fonts/STHeiti\ Light.ttc:textfile=/tmp/cap.txt:fontsize=24:fontcolor=white:x=(w-text_w)/2:y=h-60" -c:v libx264 -preset veryfast -an /tmp/smoke.mp4 2>&1 | tail -3
ls -la /tmp/smoke.mp4
```

Expected: `/tmp/smoke.mp4` 生成、无 drawtext 报错。若字体路径含空格在 `-vf` 参数内出问题，确认传给 ProcessBuilder 时不要 shell 引号（数组传参），filter 内字体路径用单引号包裹：`drawtext=fontfile='/System/Library/Fonts/STHeiti Light.ttc':...`。

- [ ] **Step 4: Commit（无代码改动可跳过；有则提交环境说明）**

---

### Task A1: Flyway V10 迁移建表 m4_video

**Files:**
- Create: `xiaoliao-api/src/main/resources/db/migration/V10__m4_video.sql`

- [ ] **Step 1: 创建迁移文件**（照抄以下全部内容）

```sql
-- =============================================
-- V10：视频作品表（作品 = 后端合成的 mp4）
-- 设计文档：docs/superpowers/specs/2026-09-06-video-works-only-design.md
-- =============================================
CREATE TABLE IF NOT EXISTS m4_video (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(64)  NOT NULL,
    node_id     BIGINT,
    era_id      BIGINT,
    title       VARCHAR(64),
    caption     TEXT,
    video_url   VARCHAR(512) NOT NULL,
    poster_url  VARCHAR(512),
    duration    INT          DEFAULT 0,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     DEFAULT 0
);
COMMENT ON TABLE m4_video IS 'M4视频作品（后端ffmpeg合成）';
COMMENT ON COLUMN m4_video.node_id IS '所属人生时光节点（与 era_id 二选一）';
COMMENT ON COLUMN m4_video.era_id IS '所属年代记忆';
COMMENT ON COLUMN m4_video.title IS '作品标题';
COMMENT ON COLUMN m4_video.caption IS '完整文案（详情页展示）';
COMMENT ON COLUMN m4_video.video_url IS '/uploads/videos/...mp4';
COMMENT ON COLUMN m4_video.poster_url IS '视频首帧封面';
CREATE INDEX idx_m4_video_user_node ON m4_video(user_id, node_id) WHERE deleted = 0;
CREATE INDEX idx_m4_video_user_era  ON m4_video(user_id, era_id) WHERE deleted = 0;
```

- [ ] **Step 2: 重启后端使 Flyway 执行 V10**（若后端在跑，kill 掉后台任务后重启）

```bash
# 在仓库根：
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
mvn -Dmaven.repo.local=/Users/robot/Downloads/DSH/.m2repo -pl xiaoliao-api spring-boot:run
```

- [ ] **Step 3: 验证表已建**

```bash
docker exec xiaoliao-pg bash -c "PGPASSWORD=xiaoliao_dev psql -U xiaoliao -d xiaoliao -c '\\d m4_video'"
```

Expected: 显示 m4_video 表结构；`SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;` 出现 V10 且 success=t。

- [ ] **Step 4: Commit**

```bash
git add xiaoliao-api/src/main/resources/db/migration/V10__m4_video.sql
git commit -m "feat(m4): V10 建 m4_video 视频作品表"
```

---

### Task A2: 实体 / Mapper / VO

**Files:**
- Create: `xiaoliao-api/src/main/java/com/xiaoliao/api/m4/entity/M4Video.java`
- Create: `xiaoliao-api/src/main/java/com/xiaoliao/api/m4/mapper/M4VideoMapper.java`
- Create: `xiaoliao-api/src/main/java/com/xiaoliao/api/m4/vo/VideoVO.java`

- [ ] **Step 1: 创建实体 M4Video**（风格同 `M4Story`/`M4Era`）

```java
package com.xiaoliao.api.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("m4_video")
public class M4Video {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    /** 所属人生时光节点（可选） */
    private Long nodeId;

    /** 所属年代记忆（可选） */
    private Long eraId;

    /** 作品标题 */
    private String title;

    /** 完整文案（详情展示） */
    private String caption;

    /** 视频地址 /uploads/videos/... */
    private String videoUrl;

    /** 封面帧地址 */
    private String posterUrl;

    /** 时长秒 */
    private Integer duration;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 2: 创建 Mapper**

```java
package com.xiaoliao.api.m4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.m4.entity.M4Video;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频作品 Mapper
 */
@Mapper
public interface M4VideoMapper extends BaseMapper<M4Video> {
}
```

- [ ] **Step 3: 创建 VideoVO**

```java
package com.xiaoliao.api.m4.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频作品 VO
 */
@Data
public class VideoVO {

    private Long id;

    private Long nodeId;

    private Long eraId;

    private String title;

    private String caption;

    private String videoUrl;

    private String posterUrl;

    private Integer duration;

    private LocalDateTime createTime;
}
```

- [ ] **Step 4: 编译验证**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
mvn -q -Dmaven.repo.local=/Users/robot/Downloads/DSH/.m2repo -pl xiaoliao-common,xiaoliao-api -am compile
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add xiaoliao-api/src/main/java/com/xiaoliao/api/m4/entity/M4Video.java \
        xiaoliao-api/src/main/java/com/xiaoliao/api/m4/mapper/M4VideoMapper.java \
        xiaoliao-api/src/main/java/com/xiaoliao/api/m4/vo/VideoVO.java
git commit -m "feat(m4): M4Video 实体/Mapper/VideoVO"
```

---

### Task A3: M4VideoComposer —— ffmpeg 视频合成组件

**Files:**
- Create: `xiaoliao-api/src/main/java/com/xiaoliao/api/m4/video/M4VideoComposer.java`

职责：把 `MultipartFile[] photos + List<String> captions + MultipartFile bgm(nullable)` 合成为 720p mp4 + 首帧封面，返回 `{videoUrl, posterUrl, duration}`。逐照片出段（中文字幕 drawtext 用 textfile 避免转义），concat 后可选混入 bgm（不足循环）。所有文件存 `uploads/videos/yyyyMM/<uuid>/`（url = uploadBaseUrl + /uploads/videos/...，与 saveWork 照片模式一致），临时段文件用后即删。

- [ ] **Step 1: 创建 M4VideoComposer.java**（完整代码如下）

```java
package com.xiaoliao.api.m4.video;

import com.xiaoliao.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 视频作品合成器：调用本机 ffmpeg 把照片组 + 字幕 + 配乐合成为 720p mp4。
 * 字幕用 Java AWT 先渲染成透明 PNG（底部半透明黑条+白色中文），再用内置 overlay 滤镜叠加，
 * 避免依赖 brew/Debian 基础版 ffmpeg 未编译的 drawtext（libfreetype）滤镜，跨环境行为一致。
 * 每张照片成段后 concat。产物目录与 /api/upload、works 照片一致（uploadDir 下），url 由 uploadBaseUrl 拼出。
 */
@Slf4j
@Component
public class M4VideoComposer {

    /** 单张照片时长（秒） */
    private static final double SEC_PER_PHOTO = 3.5;
    /** 每段 fade 淡入淡出时长（秒） */
    private static final double FADE = 0.35;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int MAX_PHOTOS = 5;
    private static final int MAX_BGM_MB = 20;
    /** 中文字幕字体候选（按序取第一个存在者） */
    private static final String[] FONT_CANDIDATES = {
            "/System/Library/Fonts/STHeiti Light.ttc",
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/Hiragino Sans GB.ttc",
            "/System/Library/Fonts/Supplemental/Songti.ttc"
    };
    /** AWT 字体缓存（创建一次复用） */
    private static Font CN_FONT;

    @Value("${xiaoliao.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${xiaoliao.upload.base-url:http://localhost:8080}")
    private String uploadBaseUrl;

    private String fontfile() {
        for (String c : FONT_CANDIDATES) {
            if (Files.isRegularFile(Paths.get(c))) return c;
        }
        throw new BusinessException(500, "服务器缺少中文字幕字体，无法合成视频");
    }

    private synchronized Font cnFont() {
        try {
            if (CN_FONT == null) {
                CN_FONT = Font.createFont(Font.TRUETYPE_FONT, new File(fontfile()))
                        .deriveFont(Font.BOLD, 46f);
            }
            return CN_FONT;
        } catch (Exception e) {
            throw new BusinessException(500, "加载中文字幕字体失败：" + e.getMessage());
        }
    }

    /** 字幕文本清洗：去换行/控制符、超长截断（Java 渲染无需 filter 转义） */
    private String captionText(String s) {
        String t = (s == null ? "" : s).replaceAll("[\r\n\t]", " ").trim();
        return t.length() > 24 ? t.substring(0, 23) + "…" : t;
    }

    /** 把一条字幕渲染成 1280x720 透明 PNG（底部半透明黑条 + 白色居中文字；空字幕=全透明） */
    private void renderCaptionPng(String text, Path outPng) throws Exception {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            String cap = captionText(text);
            if (!cap.isEmpty()) {
                Font font = cnFont();
                g.setFont(font);
                FontMetrics fm = g.getFontMetrics();
                int barH = fm.getHeight() + 36;
                int barY = HEIGHT - barH - 24;
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRoundRect(40, barY, WIDTH - 80, barH, 24, 24);
                g.setColor(Color.WHITE);
                int tw = fm.stringWidth(cap);
                g.drawString(cap, (WIDTH - tw) / 2, barY + (barH - fm.getHeight()) / 2 + fm.getAscent());
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(img, "png", outPng.toFile());
    }

    public record Result(String videoUrl, String posterUrl, int duration) {
    }

    /**
     * 合成视频。
     *
     * @param photos   有序照片 1..5
     * @param captions 与 photos 一一对应的字幕文本（长度不足补空）
     * @param bgm      可选配乐音频文件（wav/mp3 等 ffmpeg 可解码），null = 无声
     */
    public Result compose(List<MultipartFile> photos, List<String> captions, MultipartFile bgm) {
        if (photos == null || photos.isEmpty() || photos.size() > MAX_PHOTOS) {
            throw new BusinessException(400, "照片需 1~" + MAX_PHOTOS + " 张");
        }
        for (MultipartFile f : photos) {
            if (f == null || f.isEmpty() || f.getSize() > 10L * 1024 * 1024) {
                throw new BusinessException(400, "照片无效或超过 10MB");
            }
        }
        // headless 服务器渲染字体需要
        System.setProperty("java.awt.headless", "true");
        String sub = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String token = UUID.randomUUID().toString().replace("-", "");
        Path outDir = Paths.get(uploadDir, "videos", sub, token).toAbsolutePath().normalize();
        Path tmpDir = Paths.get(uploadDir, "videos", "tmp", token).toAbsolutePath().normalize();
        try {
            Files.createDirectories(outDir);
            Files.createDirectories(tmpDir);

            List<String> caps = new ArrayList<>();
            for (int i = 0; i < photos.size(); i++) {
                caps.add(captionText(i < captions.size() ? captions.get(i) : ""));
            }

            // 1) 照片与配乐落盘
            List<Path> photoFiles = new ArrayList<>();
            for (int i = 0; i < photos.size(); i++) {
                Path p = tmpDir.resolve("photo_" + i + ".jpg");
                photos.get(i).transferTo(p.toFile());
                photoFiles.add(p);
            }
            Path bgmFile = null;
            if (bgm != null && !bgm.isEmpty()) {
                if (bgm.getSize() > MAX_BGM_MB * 1024L * 1024L) {
                    throw new BusinessException(400, "配乐文件超过 " + MAX_BGM_MB + "MB");
                }
                bgmFile = tmpDir.resolve("bgm" + suffixOf(bgm.getOriginalFilename()));
                bgm.transferTo(bgmFile.toFile());
            }

            // 2) 每张照片 → 字幕PNG → 片段（fade in/out + overlay 字幕）
            double each = SEC_PER_PHOTO;
            List<Path> segs = new ArrayList<>();
            for (int i = 0; i < photoFiles.size(); i++) {
                Path capPng = tmpDir.resolve("cap_" + i + ".png");
                renderCaptionPng(caps.get(i), capPng);
                Path seg = tmpDir.resolve("seg_" + i + ".mp4");
                double fadeOutStart = Math.max(0.2, each - FADE);
                String fc = String.format(
                        "[0:v]scale=%d:%d:force_original_aspect_ratio=decrease," +
                        "pad=%d:%d:(ow-iw)/2:(oh-ih)/2:color=black,setsar=1[b0];" +
                        "[b0][1:v]overlay=0:0:shortest=1," +
                        "fade=t=in:st=0:d=%.2f,fade=t=out:st=%.2f:d=%.2f," +
                        "format=yuv420p,fps=25[v]",
                        WIDTH, HEIGHT, WIDTH, HEIGHT, FADE, fadeOutStart, FADE);
                List<String> cmd = new ArrayList<>(List.of(
                        "ffmpeg", "-y",
                        "-loop", "1", "-framerate", "25", "-t", String.valueOf(each), "-i", photoFiles.get(i).toString(),
                        "-loop", "1", "-framerate", "25", "-t", String.valueOf(each), "-i", capPng.toString(),
                        "-filter_complex", fc,
                        "-map", "[v]",
                        "-c:v", "libx264", "-preset", "veryfast", "-crf", "23", "-an",
                        seg.toString()));
                run(cmd, 90);
                segs.add(seg);
            }

            // 3) concat 片段
            Path listFile = tmpDir.resolve("concat.txt");
            StringBuilder sb = new StringBuilder();
            for (Path s : segs) {
                sb.append("file '").append(s.toAbsolutePath().toString().replace("'", "'\\''")).append("'\n");
            }
            Files.writeString(listFile, sb.toString(), StandardCharsets.UTF_8);
            String outMp4 = outDir.resolve("video.mp4").toString();
            List<String> cat = new ArrayList<>(List.of(
                    "ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", listFile.toString()));
            if (bgmFile != null) {
                cat.addAll(List.of("-stream_loop", "-1", "-i", bgmFile.toString()));
            }
            cat.addAll(List.of("-c:v", "libx264", "-preset", "veryfast", "-crf", "23"));
            if (bgmFile != null) {
                cat.addAll(List.of("-c:a", "aac", "-b:a", "128k", "-shortest"));
            } else {
                cat.add("-an");
            }
            cat.add(outMp4);
            run(cat, 180);

            // 4) 首帧封面
            String poster = outDir.resolve("poster.jpg").toString();
            run(List.of("ffmpeg", "-y", "-i", outMp4, "-frames:v", "1", "-q:v", "3", poster), 60);

            int duration = (int) Math.round(each * photoFiles.size());
            String base = uploadBaseUrl.replaceAll("/+$", "");
            String rel = "/uploads/videos/" + sub + "/" + token;
            return new Result(base + rel + "/video.mp4", base + rel + "/poster.jpg", duration);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("视频合成失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "视频合成失败：" + e.getMessage());
        } finally {
            deleteRecursively(tmpDir);
        }
    }

    private String suffixOf(String name) {
        if (name == null || !name.contains(".")) return ".wav";
        return name.substring(name.lastIndexOf('.')).toLowerCase();
    }

    private void run(List<String> cmd, int timeoutSec) {
        try {
            log.info("ffmpeg: {}", String.join(" ", cmd));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new BusinessException(500, "ffmpeg 超时");
            }
            if (p.exitValue() != 0) {
                String tail = out.length() > 800 ? out.substring(out.length() - 800) : out;
                throw new BusinessException(500, "ffmpeg 失败 exit=" + p.exitValue() + "：" + tail);
            }
        } catch (IOException e) {
            throw new BusinessException(500, "ffmpeg 无法执行，请确认已安装（brew install ffmpeg）：" + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "ffmpeg 中断");
        }
    }

    private void deleteRecursively(Path dir) {
        try {
            if (dir == null || !Files.exists(dir)) return;
            try (var walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) { }
                });
            }
        } catch (IOException ignored) { }
    }
}
```


- [ ] **Step 2: 编译**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
mvn -q -Dmaven.repo.local=/Users/robot/Downloads/DSH/.m2repo -pl xiaoliao-common,xiaoliao-api -am compile
```

Expected: BUILD SUCCESS（如缺 import，补齐 `java.io.IOException` 等）

- [ ] **Step 3: Commit**

```bash
git add xiaoliao-api/src/main/java/com/xiaoliao/api/m4/video/M4VideoComposer.java
git commit -m "feat(m4): M4VideoComposer ffmpeg 视频合成组件"
```

---

### Task A4: M4Service 接口 + 实现（save/list/get/delete + 计数切换）

**Files:**
- Modify: `xiaoliao-api/src/main/java/com/xiaoliao/api/m4/service/M4Service.java`
- Modify: `xiaoliao-api/src/main/java/com/xiaoliao/api/m4/service/impl/M4ServiceImpl.java`

- [ ] **Step 1: 接口新增方法**（在 `M4Service.java` 的 `saveWork` 方法之后插入）

```java
    // ---------- 视频作品（后端合成） ----------

    /** 上传素材包并合成视频作品，挂到人生时光节点或年代记忆 */
    Map<String, Object> saveVideo(String userId, String destKind, Long nodeId, String nodeLabel, String eraName,
                                  String title, String caption,
                                  List<MultipartFile> photos, List<String> captions, MultipartFile bgm);

    /** 视频作品列表（可按节点或年代过滤） */
    List<VideoVO> listVideos(String userId, Long nodeId, Long eraId);

    /** 单条视频作品 */
    VideoVO getVideo(String userId, Long id);

    /** 删除视频作品（逻辑删除） */
    void deleteVideo(String userId, Long id);
```

- [ ] **Step 2: 同步接口 import**：在 M4Service.java 顶部 import 区加入 `com.xiaoliao.api.m4.vo.VideoVO`（与其它 vo import 同组）。

- [ ] **Step 3: Impl 增加字段注入与私有位置解析**

在 `M4ServiceImpl` 字段区（`private final M4StoryMapper storyMapper;` 之后）加入：

```java
    private final M4VideoMapper videoMapper;
    private final M4VideoComposer videoComposer;
```

`@RequiredArgsConstructor` 会自动注入。然后在类内新增两个私有方法（放在 `requireUser` 附近）：

```java
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
```

- [ ] **Step 4: Impl 新增四个 @Override 方法**（放在 `deleteStory` 方法之后、`saveWork` 之前；`saveWork` 保持原样不删）

```java
    @Override
    public Map<String, Object> saveVideo(String userId, String destKind, Long nodeId, String nodeLabel, String eraName,
                                         String title, String caption,
                                         List<MultipartFile> photos, List<String> captions, MultipartFile bgm) {
        requireUser(userId);
        List<MultipartFile> files = new ArrayList<>();
        if (photos != null) {
            for (MultipartFile f : photos) {
                if (f != null && !f.isEmpty()) files.add(f);
            }
        }
        if (files.isEmpty()) {
            throw new BusinessException(400, "请先传照片（1~5 张）");
        }
        if (files.size() > 5) {
            throw new BusinessException(400, "最多 5 张照片");
        }
        Map.Entry<Long, Long> place = resolvePlacement(userId, destKind, nodeId, nodeLabel, eraName);
        List<String> capList = new ArrayList<>();
        if (captions != null) {
            for (String c : captions) {
                if (c != null) capList.add(c);
            }
        }
        M4VideoComposer.Result r = videoComposer.compose(files, capList, bgm);

        M4Video v = new M4Video();
        v.setUserId(userId);
        v.setNodeId(place.getKey());
        v.setEraId(place.getValue());
        v.setTitle(StringUtils.hasText(title) ? title.trim().substring(0, Math.min(title.trim().length(), 64)) : null);
        v.setCaption(StringUtils.hasText(caption) ? caption.trim().substring(0, Math.min(caption.trim().length(), 5000)) : null);
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
        log.info("视频作品删除: userId={}, id={}", userId, id);
    }
```

- [ ] **Step 5: 计数切换 —— timeline()**（把 `timeline` 里 `storyMapper.selectCount(...)` 块替换为 video 计数）

原代码（约 744-748 行）：
```java
            vo.setStoryCount(storyMapper.selectCount(new LambdaQueryWrapper<M4Story>()
                    .eq(M4Story::getUserId, userId)
                    .eq(M4Story::getNodeId, node.getId())));
```
替换为：
```java
            vo.setVideoCount(videoMapper.selectCount(new LambdaQueryWrapper<M4Video>()
                    .eq(M4Video::getUserId, userId)
                    .eq(M4Video::getNodeId, node.getId())));
```

- [ ] **Step 6: 计数切换 —— listEras()**（把 listEras 中 `vo.setStoryCount(...)` 块替换）

原代码：
```java
                    vo.setStoryCount(storyMapper.selectCount(new LambdaQueryWrapper<M4Story>()
                            .eq(M4Story::getUserId, userId)
                            .eq(M4Story::getEraId, era.getId())));
```
替换为：
```java
                    vo.setVideoCount(videoMapper.selectCount(new LambdaQueryWrapper<M4Video>()
                            .eq(M4Video::getUserId, userId)
                            .eq(M4Video::getEraId, era.getId())));
```

- [ ] **Step 7: 计数切换 —— garden()**（把 `vo.setStoryCount(storyMapper...)` 替换为 video 计数）

原代码：
```java
        vo.setStoryCount(storyMapper.selectCount(new LambdaQueryWrapper<M4Story>()
                .eq(M4Story::getUserId, userId)));
```
替换为：
```java
        vo.setVideoCount(videoMapper.selectCount(new LambdaQueryWrapper<M4Video>()
                .eq(M4Video::getUserId, userId)));
```

- [ ] **Step 8: 编译**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
mvn -q -Dmaven.repo.local=/Users/robot/Downloads/DSH/.m2repo -pl xiaoliao-common,xiaoliao-api -am compile
```

Expected: BUILD SUCCESS（若引用 `vo.setStoryCount` 报错，是因为 Step 5-7 前 NodeVO 还没改，见 Task A5；可先只做 1-4 编译）

- [ ] **Step 9: Commit**

```bash
git add xiaoliao-api/src/main/java/com/xiaoliao/api/m4/service/M4Service.java \
        xiaoliao-api/src/main/java/com/xiaoliao/api/m4/service/impl/M4ServiceImpl.java
git commit -m "feat(m4): 视频作品 save/list/get/delete + 计数切换到 m4_video"
```

---

### Task A5: VO 字段 storyCount → videoCount

**Files:**
- Modify: `xiaoliao-api/src/main/java/com/xiaoliao/api/m4/vo/NodeVO.java`
- Modify: `xiaoliao-api/src/main/java/com/xiaoliao/api/m4/vo/EraVO.java`
- Modify: `xiaoliao-api/src/main/java/com/xiaoliao/api/m4/vo/GardenVO.java`

- [ ] **Step 1: NodeVO**

把字段：
```java
    /** 节点下故事数 */
    private Long storyCount;
```
替换为：
```java
    /** 节点下视频作品数 */
    private Long videoCount;
```

- [ ] **Step 2: EraVO**

把字段：
```java
    /** 年代下故事/对照卡数 */
    private Long storyCount;
```
替换为：
```java
    /** 年代下视频作品数 */
    private Long videoCount;
```

- [ ] **Step 3: GardenVO**

把字段：
```java
    /** 故事总数（一朵花） */
    private Long storyCount;
```
替换为：
```java
    /** 视频作品总数（一朵花） */
    private Long videoCount;
```

- [ ] **Step 4: 全仓编译（含测试编译，检查残留引用）**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
mvn -q -Dmaven.repo.local=/Users/robot/Downloads/DSH/.m2repo -DskipTests compile
grep -rn "setStoryCount\|getStoryCount\|\.storyCount" xiaoliao-api/src/main/java | grep -v m4_story | head
```

Expected: BUILD SUCCESS；grep 只剩与 m4_story 表/实体相关或不存在的输出。若 ServiceImpl 里仍有 `setStoryCount`（比如别处 saveStory 相关 VO 组装之外），检查是否属于 Task A4 Step 5-7 漏改位置并修正。

- [ ] **Step 5: Commit**

```bash
git add xiaoliao-api/src/main/java/com/xiaoliao/api/m4/vo/NodeVO.java \
        xiaoliao-api/src/main/java/com/xiaoliao/api/m4/vo/EraVO.java \
        xiaoliao-api/src/main/java/com/xiaoliao/api/m4/vo/GardenVO.java
git commit -m "feat(m4): NodeVO/EraVO/GardenVO storyCount 改 videoCount"
```

---

### Task A6: Controller 新增 /videos 端点

**Files:**
- Modify: `xiaoliao-api/src/main/java/com/xiaoliao/api/m4/controller/M4Controller.java`

- [ ] **Step 1: import 补充**（在 vo import 组加）

```java
import com.xiaoliao.api.m4.vo.VideoVO;
```

- [ ] **Step 2: 在 saveWork 端点（约 127 行）之后插入四个端点**

```java
    // ---------- 视频作品（后端合成） ----------

    @Operation(summary = "上传素材并合成视频作品", description = "照片(1~5)+字幕数组+标题+文案+可选配乐音频，ffmpeg 合成 mp4 后挂到人生时光节点或年代记忆")
    @PostMapping(value = "/videos", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> saveVideo(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @RequestParam("destKind") String destKind,
            @RequestParam(required = false) Long nodeId,
            @RequestParam(required = false) String nodeLabel,
            @RequestParam(required = false) String eraName,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String caption,
            @RequestParam(value = "captions", required = false) String[] captions,
            @RequestParam(value = "photos", required = false) MultipartFile[] photos,
            @RequestParam(value = "bgm", required = false) MultipartFile bgm) {
        return Result.ok("视频作品收好了",
                m4Service.saveVideo(userId, destKind, nodeId, nodeLabel, eraName, title, caption,
                        photos == null ? java.util.Collections.emptyList() : java.util.Arrays.asList(photos),
                        captions == null ? java.util.Collections.emptyList() : java.util.Arrays.asList(captions),
                        bgm));
    }

    @Operation(summary = "视频作品列表", description = "可按人生时光节点或年代记忆过滤")
    @GetMapping("/videos")
    public Result<List<VideoVO>> listVideos(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @RequestParam(required = false) Long nodeId,
            @RequestParam(required = false) Long eraId) {
        return Result.ok(m4Service.listVideos(userId, nodeId, eraId));
    }

    @Operation(summary = "视频作品详情")
    @GetMapping("/videos/{id}")
    public Result<VideoVO> getVideo(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @PathVariable Long id) {
        return Result.ok(m4Service.getVideo(userId, id));
    }

    @Operation(summary = "删除视频作品")
    @DeleteMapping("/videos/{id}")
    public Result<Void> deleteVideo(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @PathVariable Long id) {
        m4Service.deleteVideo(userId, id);
        return Result.ok("删除成功", null);
    }
```

- [ ] **Step 3: 编译**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
mvn -q -Dmaven.repo.local=/Users/robot/Downloads/DSH/.m2repo -pl xiaoliao-common,xiaoliao-api -am compile
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add xiaoliao-api/src/main/java/com/xiaoliao/api/m4/controller/M4Controller.java
git commit -m "feat(m4): /api/v1/m4/videos 四个端点"
```

---

### Task A7: 集成验证（重启后端 + curl E2E）

**Files:** 无（测试）

前置：确认 ffmpeg 可用、后端 spring-boot:run 在跑（若改过代码需重启）。重启方式：kill 掉现有 mvn 后台进程后重新 `mvn ... spring-boot:run`，等 `/actuator/health` 返回 200。

- [ ] **Step 1: 准备测试图片与配乐**

```bash
python3 -c "
import base64,struct,math,wave
open('/tmp/v1.png','wb').write(base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='))
open('/tmp/v2.png','wb').write(base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='))
# 1 秒 440Hz 正弦 wav
fr=44100
with wave.open('/tmp/bgm.wav','wb') as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(fr)
    w.writeframes(b''.join(struct.pack('<h', int(12000*math.sin(2*math.pi*440*i/fr))) for i in range(fr)))
print('素材就绪')
"
cp /tmp/v1.png /tmp/v2.png /tmp/bgm.wav /tmp/ 2>/dev/null; ls -la /tmp/v1.png /tmp/v2.png /tmp/bgm.wav
```

- [ ] **Step 2: 新用户保存视频到人生时光节点**

```bash
TU="e2e-video-0001"
BASE="http://127.0.0.1:8080/api/v1/m4"
# 取该用户第一个节点 id
NID=$(curl -s -H "X-Mock-User-Id: $TU" "$BASE/timeline" | python3 -c "import json,sys;print(json.load(sys.stdin)['data'][0]['id'])")
echo "nodeId=$NID"
curl -s -X POST -H "X-Mock-User-Id: $TU" "$BASE/videos" \
  -F "destKind=life" -F "nodeId=$NID" -F "title=麦田里的夏天" \
  -F "caption=这是完整文案，讲那年在麦田里的事。" \
  -F "captions=第一张，夏天的麦田" -F "captions=第二张，收割的日子" \
  -F "photos=@/tmp/v1.png" -F "photos=@/tmp/v2.png" -F "bgm=@/tmp/bgm.wav"
```

Expected: 返回 code 200 data.videoId / videoUrl(/uploads/videos/...) / posterUrl / duration(7)。耗时约 10-30s（ffmpeg 合成）。

- [ ] **Step 3: 验证产物文件与 mp4 可解码**

```bash
# 用上一步返回的 videoId/url：
VID=$(curl -s -H "X-Mock-User-Id: e2e-video-0001" "http://127.0.0.1:8080/api/v1/m4/videos" | python3 -c "import json,sys;d=json.load(sys.stdin)['data'];print(d[0]['id'] if d else '')")
echo "videoId=$VID"
curl -s -H "X-Mock-User-Id: e2e-video-0001" "http://127.0.0.1:8080/api/v1/m4/videos/$VID" | python3 -m json.tool
# 落盘检查（uploads 相对仓库根 xiaoliao-api 运行目录）
find . -path ./target -prune -o -name "video.mp4" -print 2>/dev/null | tail -3
ffmpeg -i $(find . -path ./target -prune -o -name "video.mp4" -print 2>/dev/null | tail -1) -f null - 2>&1 | grep -E "Duration|Stream" | head -3
```

Expected: 详情含 videoUrl/posterUrl/caption；ffmpeg 能读取 mp4 且 Duration≈7s、有 Video/Audio(如用 bgm) 流。

- [ ] **Step 4: 验证 HTTP 可访问视频与封面、计数**

```bash
# 从详情取 url 后：
curl -s -o /dev/null -w "video http=%{http_code} size=%{size_download}\n" "http://127.0.0.1:8080<videoUrl>"
curl -s -H "X-Mock-User-Id: e2e-video-0001" "http://127.0.0.1:8080/api/v1/m4/timeline" | python3 -c "import json,sys;n=json.load(sys.stdin)['data'][0];print('node videoCount=',n['videoCount'])"
curl -s -H "X-Mock-User-Id: e2e-video-0001" "http://127.0.0.1:8080/api/v1/m4/garden"
```

Expected: video http=200；node videoCount=1；garden.videoCount=1。

- [ ] **Step 5: 年代场景 + 无 bgm + 隔离 + 删除**

```bash
TU="e2e-video-0001"
BASE="http://127.0.0.1:8080/api/v1/m4"
# 年代 + 无配乐
curl -s -X POST -H "X-Mock-User-Id: $TU" "$BASE/videos" \
  -F "destKind=era" -F "eraName=八十年代" -F "title=老厂房" \
  -F "caption=厂里热闹。" -F "captions=老厂房门口" \
  -F "photos=@/tmp/v1.png"
# eras 计数
curl -s -H "X-Mock-User-Id: $TU" "$BASE/eras" | python3 -c "import json,sys;print([(e['name'],e['videoCount']) for e in json.load(sys.stdin)['data']])"
# 其他用户隔离（应为空）
curl -s -H "X-Mock-User-Id: e2e-video-0002" "$BASE/videos" | python3 -c "import json,sys;print('other videos:',len(json.load(sys.stdin)['data']))"
# 删除第一条
VID=$(curl -s -H "X-Mock-User-Id: $TU" "$BASE/videos?nodeId=$(curl -s -H 'X-Mock-User-Id: '$TU $BASE/timeline | python3 -c "import json,sys;print(json.load(sys.stdin)['data'][0]['id'])")" | python3 -c "import json,sys;d=json.load(sys.stdin)['data'];print(d[0]['id'] if d else '')")
curl -s -X DELETE -H "X-Mock-User-Id: $TU" "$BASE/videos/$VID"
curl -s -H "X-Mock-User-Id: $TU" "$BASE/videos?nodeId=$(curl -s -H 'X-Mock-User-Id: '$TU $BASE/timeline | python3 -c "import json,sys;print(json.load(sys.stdin)['data'][0]['id'])")" | python3 -c "import json,sys;print('after delete:',len(json.load(sys.stdin)['data']))"
```

Expected: eras 出现 八十年代 videoCount=1；other videos=0；delete 后该节点列表为 0。

- [ ] **Step 6: 清理测试数据**

```bash
docker exec -i xiaoliao-pg bash -c "PGPASSWORD=xiaoliao_dev psql -U xiaoliao -d xiaoliao" <<'SQL'
DELETE FROM m4_video WHERE user_id LIKE 'e2e-%';
DELETE FROM m4_era WHERE user_id LIKE 'e2e-%';
DELETE FROM m4_timeline_node WHERE user_id LIKE 'e2e-%';
DELETE FROM users WHERE id LIKE 'e2e-%';
SQL
find . -path ./target -prune -o -type d -name "e2e*" -print 2>/dev/null | head
```

- [ ] **Step 7: Commit（如无代码改动仅验证，可跳过）**
