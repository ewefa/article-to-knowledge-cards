package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.security.LoginRequired;
import com.campus.trade.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 图片上传：商品图片、头像
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileService fileService;

    @PostMapping("/image")
    @LoginRequired
    public Result<Map<String, Object>> image(@RequestParam("file") MultipartFile file) {
        String url = fileService.save(file);
        return Result.ok("上传成功", Map.of("url", url));
    }
}
