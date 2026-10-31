package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.config.CampusProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 图片上传：保存到本地 uploads 目录，按年月分目录，返回可访问的相对路径
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private static final List<String> ALLOWED = List.of("jpg", "jpeg", "png", "gif", "webp");

    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private final CampusProperties properties;

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException("图片大小不能超过 5MB");
        }
        String original = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(original);
        if (ext == null || !ALLOWED.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new BizException("仅支持 jpg / png / gif / webp 格式图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException("上传文件必须是图片");
        }
        String folder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        File dir = new File(properties.getUpload().getDir(), folder);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException("图片保存目录创建失败");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext.toLowerCase(Locale.ROOT);
        File target = new File(dir, filename);
        try {
            file.transferTo(target.getAbsoluteFile());
        } catch (IOException e) {
            throw new BizException("图片保存失败，请重试");
        }
        return properties.getUpload().getUrlPrefix() + "/" + folder + "/" + filename;
    }
}
