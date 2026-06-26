package com.slm.barbershop.controller;

import com.slm.barbershop.model.ApiResponse;
import com.slm.barbershop.model.FileResponse;
import com.slm.barbershop.service.FileService;
import com.slm.barbershop.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
        return ApiResponse.ok(fileService.upload(file, userId));
    }

}
