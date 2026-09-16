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
 * ffmpeg 可执行文件路径由 xiaoliao.ffmpeg.path 配置（默认用 PATH 里的 ffmpeg，Windows 可指向本地安装路径）。
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
    /** 中文字幕字体候选（按序取第一个存在者；Windows 演示机优先，其次 macOS、Debian/Ubuntu 常见中文字体） */
    private static final String[] FONT_CANDIDATES = {
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/Deng.ttf",
            "/System/Library/Fonts/STHeiti Light.ttc",
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/Hiragino Sans GB.ttc",
            "/System/Library/Fonts/Supplemental/Songti.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"
    };
    /** AWT 字体缓存（创建一次复用） */
    private static Font cnFontCache;

    @Value("${xiaoliao.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${xiaoliao.upload.base-url:http://localhost:8080}")
    private String uploadBaseUrl;

    @Value("${xiaoliao.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    private String fontPath() {
        for (String c : FONT_CANDIDATES) {
            if (Files.isRegularFile(Paths.get(c))) return c;
        }
        throw new BusinessException(500, "服务器缺少中文字幕字体，无法合成视频（Windows 请确认 C:/Windows/Fonts/msyh.ttc 或 simhei.ttf 存在）");
    }

    private synchronized Font cnFont() {
        try {
            if (cnFontCache == null) {
                cnFontCache = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath()))
                        .deriveFont(Font.BOLD, 46f);
            }
            return cnFontCache;
        } catch (Exception e) {
            throw new BusinessException(500, "加载中文字幕字体失败：" + e.getMessage());
        }
    }

    /** 字幕文本清洗：去换行/控制符、超长截断（按码点截断避免拆散代理对；Java 渲染无需 filter 转义） */
    private String captionText(String s) {
        String t = (s == null ? "" : s).replaceAll("[\r\n\t]", " ").trim();
        if (t.codePointCount(0, t.length()) <= 24) return t;
        return t.substring(0, t.offsetByCodePoints(0, 23)) + "…";
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
                String fc = String.format(java.util.Locale.ROOT,
                        "[0:v]scale=%d:%d:force_original_aspect_ratio=decrease," +
                        "pad=%d:%d:(ow-iw)/2:(oh-ih)/2:color=black,setsar=1[b0];" +
                        "[b0][1:v]overlay=0:0:shortest=1," +
                        "fade=t=in:st=0:d=%.2f,fade=t=out:st=%.2f:d=%.2f," +
                        "format=yuv420p,fps=25[v]",
                        WIDTH, HEIGHT, WIDTH, HEIGHT, FADE, fadeOutStart, FADE);
                List<String> cmd = new ArrayList<>(List.of(
                        ffmpegPath, "-y",
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
                    ffmpegPath, "-y", "-f", "concat", "-safe", "0", "-i", listFile.toString()));
            if (bgmFile != null) {
                cat.addAll(List.of("-stream_loop", "-1", "-i", bgmFile.toString()));
            }
            cat.addAll(List.of("-c:v", "libx264", "-preset", "veryfast", "-crf", "23"));
            if (bgmFile != null) {
                cat.addAll(List.of("-map", "0:v:0", "-map", "1:a:0",
                        "-c:a", "aac", "-b:a", "128k", "-shortest"));
            } else {
                cat.addAll(List.of("-map", "0:v:0", "-an"));
            }
            cat.add(outMp4);
            run(cat, 180);

            // 4) 封面（-ss 在 -i 前做输入侧 seek，跳过首段 fade-in 的纯黑帧）
            String poster = outDir.resolve("poster.jpg").toString();
            run(List.of(ffmpegPath, "-y", "-ss", "0.6", "-i", outMp4, "-frames:v", "1", "-q:v", "3", poster), 60);

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
        Path errLog = null;
        try {
            log.info("ffmpeg: {}", String.join(" ", cmd));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            errLog = Files.createTempFile("ffmpeg-err-", ".log");
            pb.redirectOutput(errLog.toFile());
            Process p = pb.start();
            // 先等进程结束再读日志：避免 readAllBytes 阻塞使 waitFor 超时保护失效
            if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                p.waitFor(5, TimeUnit.SECONDS);
                throw new BusinessException(500, "ffmpeg 超时(超过" + timeoutSec + "s)");
            }
            if (p.exitValue() != 0) {
                String out = Files.readString(errLog, StandardCharsets.UTF_8);
                String tail = out.length() > 800 ? out.substring(out.length() - 800) : out;
                throw new BusinessException(500, "ffmpeg 失败 exit=" + p.exitValue() + "：" + tail);
            }
        } catch (IOException e) {
            throw new BusinessException(500, "ffmpeg 无法执行，请先安装 ffmpeg 或配置 xiaoliao.ffmpeg.path（当前：" + ffmpegPath + "）：" + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "ffmpeg 中断");
        } finally {
            try {
                if (errLog != null) Files.deleteIfExists(errLog);
            } catch (IOException ignored) { }
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
