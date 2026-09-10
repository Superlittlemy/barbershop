package com.slm.storage.controller;

import com.slm.common.context.UserContext;
import com.slm.common.model.ApiResponse;
import com.slm.storage.model.AccessUrlRequest;
import com.slm.storage.model.AccessUrlResponse;
import com.slm.storage.model.FileResponse;
import com.slm.storage.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "文件", description = "文件上传与元数据管理")
@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @Operation(summary = "上传文件(按内容去重)")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileResponse> upload(@RequestPart("file") MultipartFile file) {
        Long userId = UserContext.getUser() != null ? UserContext.getUser().getId() : null;
        return ApiResponse.success(fileService.upload(file, userId));
    }

    @Operation(summary = "批量获取文件访问地址(带过期时间)")
    @PostMapping("/access-url")
    public ApiResponse<List<AccessUrlResponse>> accessUrl(@Valid @RequestBody AccessUrlRequest request) {
        return ApiResponse.success(fileService.getAccessUrls(request.getObjectKeys(), request.getExpiresSeconds()));
    }

}
