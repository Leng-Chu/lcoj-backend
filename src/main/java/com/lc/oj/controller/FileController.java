package com.lc.oj.controller;

import com.lc.oj.common.BaseResponse;
import com.lc.oj.common.ErrorCode;
import com.lc.oj.common.ResultUtils;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.model.vo.FileListVO;
import com.lc.oj.model.vo.FileListVO.Case;
import com.lc.oj.utils.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${lcoj.data-path}")
    private String dataPath;

    @PostMapping("/upload")
    public BaseResponse<Boolean> uploadFile(@RequestParam Long num, @RequestParam MultipartFile[] file) {
        String path = dataPath + num;
        try {
            //request.getName()是文件夹名称，文件夹在./src/data/下，如果没有该文件夹，先创建
            File dir = new File(path);
            if (!dir.exists()) {
                boolean flag = dir.mkdirs();
                if (!flag) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR);
                }
            }
            for (MultipartFile multipartFile : file) {
                multipartFile.transferTo(new File(path + File.separator + multipartFile.getOriginalFilename()));
            }
            return ResultUtils.success(true);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    @GetMapping("/list")
    public BaseResponse<FileListVO> listFiles(@RequestParam Long num) {
        String path = dataPath + num;
        File dir = new File(path);
        List<Case> inputFiles = new ArrayList<>();
        List<Case> outputFiles = new ArrayList<>();

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    Case item = new Case();
                    item.setName(file.getName());
                    item.setSize(file.length());
                    if (file.getName().endsWith(".in")) {
                        inputFiles.add(item);
                    } else if (file.getName().endsWith(".out")||file.getName().endsWith(".ans")) {
                        outputFiles.add(item);
                    }
                }
            }
        }
        //根据名称排序
        inputFiles.sort(Comparator.comparing(Case::getName));
        outputFiles.sort(Comparator.comparing(Case::getName));
        FileListVO fileListVO = new FileListVO();
        fileListVO.setInputFiles(inputFiles);
        fileListVO.setOutputFiles(outputFiles);
        return ResultUtils.success(fileListVO);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename, @RequestParam Long num) {
        String path = dataPath + num + File.separator + filename;
        File file = new File(path);
        return download(file);
    }

    @GetMapping("/downloadAll")
    public ResponseEntity<Resource> downloadAllFiles(@RequestParam Long num) {
        String folderPath = dataPath + num;
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        String zipPath = folderPath + ".zip";
        FileUtils.zipDir(zipPath, folder);
        File zipFile = new File(zipPath);
        return download(zipFile);
    }

    private ResponseEntity<Resource> download(File file) {
        if (file.exists()) {
            try {
                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(file.length())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } catch (FileNotFoundException e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到文件");
            }
        } else {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到文件");
        }
    }

    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deleteFile(@RequestParam String filename, @RequestParam Long num) {
        String path = dataPath + num + File.separator + filename;
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                return ResultUtils.success(true);
            } else {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除文件失败");
            }
        } else {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到要删除的文件");
        }
    }

    @DeleteMapping("/deleteAll")
    public BaseResponse<Boolean> deleteAllFiles(@RequestParam Long num) {
        String folderPath = dataPath + num;
        File dir = new File(folderPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到文件夹");
        }
        FileUtils.deleteDirectory(dir);
        return ResultUtils.success(true);
    }
}