package com.lc.oj.utils;

import com.lc.oj.common.ErrorCode;
import com.lc.oj.exception.BusinessException;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件工具
 */
public class FileUtils {

    /**
     * 压缩文件夹
     * @param zipFileName
     * @param folder
     */
    public static void zipDir(String zipFileName, File folder) {
        try (FileOutputStream fos = new FileOutputStream(zipFileName);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ZipEntry zipEntry = new ZipEntry(file.getName());
                        zos.putNextEntry(zipEntry);
                        IOUtils.copy(fis, zos);
                        zos.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    /**
     * 删除文件夹
     * @param dir
     */
    public static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!dir.delete()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除文件失败");
        }
    }
}
