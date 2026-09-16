package com.xiaoliao.api.m4.service;

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
import com.xiaoliao.api.m4.vo.StoryVO;
import com.xiaoliao.api.m4.vo.VideoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * M4 回忆与传承服务
 * <p>
 * 云存储版：人生时光（节点+故事）+ 年代记忆（年代+对照）+ 朋友圈文案 + 素材库匹配 + 家人帮忙协作
 */
public interface M4Service {

    // ---------- 照片 ----------

    /** 保存照片（老人上传 or 系统示意图） */
    PhotoVO savePhoto(String userId, PhotoUploadRequest request);

    /** 我的照片列表 */
    List<PhotoVO> listPhotos(String userId);

    /** 对话内文生图：小辽根据描述生成一张照片（图片落盘 uploads/images；不落库，保存走 /photos source=gen） */
    Map<String, Object> genImage(String userId, GenImageRequest request);

    /** 根据"以前的样子"描述匹配年代示意图（素材库近似检索） */
    MatchResultVO matchImage(String userId, MatchImageRequest request);

    // ---------- 故事 ----------

    /** 保存故事（原文 + 润色版） */
    StoryVO saveStory(String userId, StorySaveRequest request);

    /** 故事列表（可按节点过滤） */
    List<StoryVO> listStories(String userId, Long nodeId, Long eraId);

    /** 删除故事（逻辑删除） */
    void deleteStory(String userId, Long id);

    /** 保存一段“回忆作品/视频”（照片组+文案+标题+配乐），原子写入照片与故事并挂到节点/年代 */
    Map<String, Object> saveWork(String userId, String destKind, Long nodeId, String nodeLabel, String eraName,
                                 String title, String text, String music,
                                 List<MultipartFile> photos);

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

    // ---------- 广场（作品分享 + 点赞） ----------

    /** 广场作品流（含我收到的赞、我的作品数；自己的作品也在流里，标 mine=true） */
    PlazaFeedVO plazaFeed(String userId, Integer limit);

    /** 把我的视频作品发到广场（同一作品重复发布直接返回已有记录） */
    PlazaWorkVO publishPlazaWork(String userId, Long videoId, String title);

    /** 从广场撤下我的作品（逻辑删除，点赞明细保留） */
    void unpublishPlazaWork(String userId, Long workId);

    /** 广场点赞 / 取消点赞（toggle），返回最新计数 */
    PlazaWorkVO togglePlazaLike(String userId, Long workId);

    // ---------- 朋友圈文案 ----------

    /** 生成朋友圈文案（当前为规则模板，AI 引擎接入后替换） */
    List<MomentVO> generateMoments(String userId, MomentGenerateRequest request);

    /** 照片 + 老人描述 → 朋友圈文案（照片/故事/文案全部落库，各页面共用这一个入口） */
    MomentPhotoVO generateMomentsFromPhoto(String userId, MomentPhotoRequest request);

    /** 我的文案列表 */
    List<MomentVO> listMoments(String userId);

    /** 老人选中一条文案（其余取消选中） */
    void selectMoment(String userId, Long momentId);

    // ---------- 人生时光（节点） ----------

    /** 时光轴全景（按人生阶段分章；首次访问自动建预设节点） */
    List<NodeVO> timeline(String userId);

    /** 新增节点（预设阶段或自定义） */
    NodeVO saveNode(String userId, NodeRequest request);

    /** 修改节点 */
    NodeVO updateNode(String userId, Long id, NodeRequest request);

    /** 删除节点（逻辑删除） */
    void deleteNode(String userId, Long id);

    // ---------- 年代记忆 ----------

    /** 新建年代 */
    EraVO saveEra(String userId, EraRequest request);

    /** 年代列表 */
    List<EraVO> listEras(String userId);

    /** 删除年代（逻辑删除） */
    void deleteEra(String userId, Long id);

    // ---------- 家人帮忙协作 ----------

    /** 老人生成分享（7天有效） */
    ShareHelpVO createShareHelp(String userId, Long nodeId);

    /** 亲友免登录查看节点（凭 token） */
    HelpDetailVO getHelp(String token);

    /** 亲友提交补图/提示（凭 token） */
    HelpDetailVO submitHelp(String token, HelpSubmitRequest request);

    /** 老人确认采用亲友补的内容 */
    ShareHelpVO confirmShareHelp(String userId, Long shareId);

    // ---------- 隐私与数据 ----------

    /** 设置隐私授权（云端存储/匿名统计） */
    ConsentVO saveConsent(String userId, ConsentRequest request);

    /** 隐私授权列表 */
    List<ConsentVO> listConsents(String userId);

    /** 导出我的全部数据（可带走） */
    ExportVO exportAll(String userId);

    /** 删除我的全部数据（被遗忘权，物理删除） */
    Map<String, Object> deleteAllData(String userId);

    /** 作废分享链接 */
    ShareHelpVO revokeShareHelp(String userId, Long shareId);

    // ---------- 花园 ----------

    /** 我的花园计数（开花数） */
    GardenVO garden(String userId);
}
