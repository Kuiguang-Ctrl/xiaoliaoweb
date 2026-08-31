package com.xiaoliao.api.m4.controller;

import com.xiaoliao.api.config.AuthContext;
import com.xiaoliao.api.m4.dto.ConsentRequest;
import com.xiaoliao.api.m4.dto.EraRequest;
import com.xiaoliao.api.m4.dto.HelpSubmitRequest;
import com.xiaoliao.api.m4.dto.MatchImageRequest;
import com.xiaoliao.api.m4.dto.MomentGenerateRequest;
import com.xiaoliao.api.m4.dto.NodeRequest;
import com.xiaoliao.api.m4.dto.PhotoUploadRequest;
import com.xiaoliao.api.m4.dto.StorySaveRequest;
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
import com.xiaoliao.api.m4.vo.StoryVO;
import com.xiaoliao.api.metrics.ApiMetric;
import com.xiaoliao.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * M4 回忆与传承
 * 人生时光（节点+故事）+ 年代记忆（年代+对照）+ 朋友圈文案 + 素材匹配 + 家人帮忙协作 + 花园
 */
@Tag(name = "M4 回忆与传承")
@RestController
@RequestMapping("/api/v1/m4")
@RequiredArgsConstructor
public class M4Controller {

    private final M4Service m4Service;

    // ---------- 照片 ----------

    @Operation(summary = "保存照片", description = "老人上传照片或系统示意图")
    @PostMapping("/photos")
    @ApiMetric("m4.photo.save")
    public Result<PhotoVO> savePhoto(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @Valid @RequestBody PhotoUploadRequest request) {
        return Result.ok("保存成功", m4Service.savePhoto(userId, request));
    }

    @Operation(summary = "我的照片列表", description = "按时间倒序")
    @GetMapping("/photos")
    public Result<List<PhotoVO>> listPhotos(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(m4Service.listPhotos(userId));
    }

    @Operation(summary = "匹配年代示意图", description = "根据老人对'以前的样子'的描述检索素材库，返回候选示意图")
    @PostMapping("/match")
    @ApiMetric("m4.match_image")
    public Result<MatchResultVO> matchImage(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @Valid @RequestBody MatchImageRequest request) {
        return Result.ok(m4Service.matchImage(userId, request));
    }

    // ---------- 故事 ----------

    @Operation(summary = "保存故事", description = "人生时光节点下或年代记忆下，原文+润色版")
    @PostMapping("/stories")
    public Result<StoryVO> saveStory(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @Valid @RequestBody StorySaveRequest request) {
        return Result.ok("故事收好了", m4Service.saveStory(userId, request));
    }

    @Operation(summary = "故事列表", description = "可按节点过滤")
    @GetMapping("/stories")
    public Result<List<StoryVO>> listStories(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @RequestParam(required = false) Long nodeId) {
        return Result.ok(m4Service.listStories(userId, nodeId));
    }

    // ---------- 朋友圈文案 ----------

    @Operation(summary = "生成朋友圈文案", description = "返回 3 条备选，老人选一条或说'再改改'")
    @PostMapping("/moments/generate")
    @ApiMetric("m4.moments.generate")
    public Result<List<MomentVO>> generateMoments(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @Valid @RequestBody MomentGenerateRequest request) {
        return Result.ok("文案写好了，看看哪条顺眼", m4Service.generateMoments(userId, request));
    }

    @Operation(summary = "我的文案列表")
    @GetMapping("/moments")
    public Result<List<MomentVO>> listMoments(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(m4Service.listMoments(userId));
    }

    @Operation(summary = "选中一条文案", description = "同用户其他文案自动取消选中")
    @PostMapping("/moments/{id}/select")
    public Result<Void> selectMoment(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @PathVariable Long id) {
        m4Service.selectMoment(userId, id);
        return Result.ok("就这条了", null);
    }

    // ---------- 人生时光（节点） ----------

    @Operation(summary = "时光轴全景", description = "按人生阶段分章，首次访问自动建预设七阶段节点")
    @GetMapping("/timeline")
    @ApiMetric("m4.timeline")
    public Result<List<NodeVO>> timeline(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(m4Service.timeline(userId));
    }

    @Operation(summary = "新增节点", description = "预设阶段或自定义阶段")
    @PostMapping("/timeline/nodes")
    public Result<NodeVO> saveNode(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @Valid @RequestBody NodeRequest request) {
        return Result.ok("节点加好了", m4Service.saveNode(userId, request));
    }

    @Operation(summary = "修改节点")
    @PutMapping("/timeline/nodes/{id}")
    public Result<NodeVO> updateNode(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @PathVariable Long id,
            @Valid @RequestBody NodeRequest request) {
        return Result.ok("改好了", m4Service.updateNode(userId, id, request));
    }

    @Operation(summary = "删除节点")
    @DeleteMapping("/timeline/nodes/{id}")
    public Result<Void> deleteNode(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @PathVariable Long id) {
        m4Service.deleteNode(userId, id);
        return Result.ok("删除成功", null);
    }

    // ---------- 年代记忆 ----------

    @Operation(summary = "新建年代", description = "如：我最怀念的80年代")
    @PostMapping("/eras")
    public Result<EraVO> saveEra(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @Valid @RequestBody EraRequest request) {
        return Result.ok("年代记下了", m4Service.saveEra(userId, request));
    }

    @Operation(summary = "年代列表")
    @GetMapping("/eras")
    public Result<List<EraVO>> listEras(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(m4Service.listEras(userId));
    }

    @Operation(summary = "删除年代")
    @DeleteMapping("/eras/{id}")
    public Result<Void> deleteEra(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @PathVariable Long id) {
        m4Service.deleteEra(userId, id);
        return Result.ok("删除成功", null);
    }

    // ---------- 家人帮忙协作 ----------

    @Operation(summary = "生成家人帮忙分享", description = "老人端调用，返回7天有效的分享token")
    @PostMapping("/timeline/nodes/{id}/share-help")
    @ApiMetric("m4.share_help.create")
    public Result<ShareHelpVO> createShareHelp(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @PathVariable Long id) {
        return Result.ok("分享链接生成好了，发给家人吧", m4Service.createShareHelp(userId, id));
    }

    @Operation(summary = "亲友查看节点", description = "免登录，凭token")
    @GetMapping("/help/{token}")
    public Result<HelpDetailVO> getHelp(@PathVariable String token) {
        return Result.ok(m4Service.getHelp(token));
    }

    @Operation(summary = "亲友提交补图/提示", description = "免登录，凭token")
    @PostMapping("/help/{token}")
    public Result<HelpDetailVO> submitHelp(@PathVariable String token,
                                           @RequestBody HelpSubmitRequest request) {
        return Result.ok("传好了，谢谢帮忙", m4Service.submitHelp(token, request));
    }

    @Operation(summary = "老人确认采用亲友补的内容", description = "确认后亲友照片自动存入照片库")
    @PostMapping("/share-help/{id}/confirm")
    public Result<ShareHelpVO> confirmShareHelp(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @PathVariable Long id) {
        return Result.ok("收到啦，用上了", m4Service.confirmShareHelp(userId, id));
    }

    // ---------- 隐私与数据 ----------

    @Operation(summary = "隐私授权列表", description = "云端存储/匿名统计授权状态")
    @GetMapping("/privacy")
    public Result<List<ConsentVO>> listConsents(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(m4Service.listConsents(userId));
    }

    @Operation(summary = "设置隐私授权", description = "cloud_storage=云端存储 anonymous_stats=匿名统计，需用户明确同意")
    @PostMapping("/privacy/consent")
    public Result<ConsentVO> saveConsent(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @Valid @RequestBody ConsentRequest request) {
        return Result.ok("已记录", m4Service.saveConsent(userId, request));
    }

    @Operation(summary = "导出我的全部数据", description = "用户可带走自己的全部M4数据")
    @GetMapping("/export")
    @ApiMetric("m4.data.export")
    public Result<ExportVO> exportAll(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(m4Service.exportAll(userId));
    }

    @Operation(summary = "删除我的全部数据", description = "被遗忘权：物理删除该用户全部M4数据")
    @DeleteMapping("/data")
    @ApiMetric("m4.data.delete")
    public Result<Map<String, Object>> deleteAllData(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok("已删除", m4Service.deleteAllData(userId));
    }

    @Operation(summary = "作废分享链接", description = "老人可随时作废已发出的家人帮忙链接")
    @PostMapping("/share-help/{id}/revoke")
    public Result<ShareHelpVO> revokeShareHelp(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @PathVariable Long id) {
        return Result.ok("分享已作废", m4Service.revokeShareHelp(userId, id));
    }
    // ---------- 花园 ----------

    @Operation(summary = "我的花园计数", description = "开花数：故事/节点/年代/照片/已选文案")
    @GetMapping("/garden")
    @ApiMetric("m4.garden")
    public Result<GardenVO> garden(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(m4Service.garden(userId));
    }
}
