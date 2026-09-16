package com.xiaoliao.api.m4.image;

/**
 * 文生图生成器（可插拔）：把一段画面描述生成一张图片并持久化到本地上传目录，
 * 返回本项目可访问的绝对 URL。将来换厂商（即梦/可灵等）只需新增实现。
 */
public interface ImageGenerator {

    /**
     * @param prompt 画面描述
     * @param style  old_photo/color/sketch，空或未知按 old_photo
     * @return 生成图片的本项目绝对访问 URL
     */
    String generate(String prompt, String style);
}