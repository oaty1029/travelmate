package com.travelmate.service;

import com.travelmate.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * 图片文件存储 - 校验 / 落盘 / 删除，供管理员与用户头像复用
 */
@Slf4j
@Service
public class ImageStorageService {

    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png");
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /** 校验并保存图片，返回可访问的相对 URL（/images/xxx.ext） */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择图片文件");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw BusinessException.badRequest("图片大小不能超过 5MB");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.lastIndexOf('.') >= 0) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!ALLOWED_IMAGE_EXT.contains(ext)) {
            throw BusinessException.badRequest("仅支持 jpg/png 格式图片");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Path.of(uploadDir, "images").toAbsolutePath();
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename));
        } catch (IOException e) {
            log.error("图片保存失败", e);
            throw new BusinessException(500, "图片上传失败，请稍后重试");
        }
        return "/images/" + filename;
    }

    /** 删除本地图片文件（仅处理 /images/ 前缀，外部 URL 忽略） */
    public void deleteImage(String url) {
        if (url == null || !url.startsWith("/images/")) return;
        String filename = url.substring("/images/".length());
        try {
            Files.deleteIfExists(Path.of(uploadDir, "images").toAbsolutePath().resolve(filename));
        } catch (IOException e) {
            log.warn("删除图片文件失败: {}", filename);
        }
    }
}
