package com.pacvue.segment.event.client;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.HashUtil;
import com.pacvue.segment.event.core.SegmentEvent;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SegmentEventClientFile implements SegmentEventClient {
    private static final long FILE_SIZE_UNIT = 1024 * 1024L; // MB
    private File file;
    private final String path;
    private final String fileName;
    private final long maxFileSizeMb;

    public SegmentEventClientFile(String path, String fileName, long maxFileSizeMb) {
        this.path = path;
        this.fileName = fileName;
        this.maxFileSizeMb = maxFileSizeMb * FILE_SIZE_UNIT;
        startNewFile(path, fileName);
    }

    @Override
    public Mono<Boolean> send(List<SegmentEvent> events) {
        return Mono.create(sink -> {
            // 检查文件大小，如果超过 100MB，则进行滚动压缩
            if (file.length() >= maxFileSizeMb) {
                File renameFile = null;
                synchronized (this) {
                    if (file.length() >= maxFileSizeMb) {
                        renameFile = renameFile(file);
                        startNewFile(path, fileName);
                    }
                }
                compressCurrentFile(renameFile);
            }

            // 写入数据到当前文件
            FileUtil.writeLines(events, file, StandardCharsets.UTF_8, true);
            sink.success(true);
        });
    }

    // 重命名文件
    private File renameFile(File file) {
        String zipFileName = file.getName().replace(".log", "_"  + System.currentTimeMillis()+  ".log");

        // 目标文件路径（重命名）
        File renamedFile = new File(file.getParent(), zipFileName);

        // 的 move 方法进行重命名
        FileUtil.move(file, renamedFile, true); // true 表示覆盖同名文件
        return renamedFile;
    }

    // 将当前文件压缩成 .zip 格式
    // 测试下来压缩率大概80%
    private void compressCurrentFile(File file)  {
        if (null == file) return;
        String zipFileName = file.getName().replace(".log", ".zip");
        File zipFile = new File(file.getParent(), zipFileName);

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            // 将当前文件内容写入压缩文件
            try (FileInputStream fis = new FileInputStream(file)) {
                IoUtil.copy(fis, zos);
            }
            zos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("zip file error", e);
        }

        // 删除原始文件，压缩后的文件保留
        FileUtil.del(file);
    }

    // 创建一个新的文件进行写入
    private void startNewFile(String path, String fileName) {
        File targetFile = FileUtil.file(FileUtil.getTmpDir(), path, fileName);
        // 目标文件不存在，创建文件
        if (!targetFile.exists()) {
            // 检查目录是否存在，如果不存在则创建
            if (!targetFile.getParentFile().exists() && !targetFile.getParentFile().mkdirs()) {
                throw new RuntimeException("create parent dir failed");
            }
            // 创建文件（如果文件不存在）
            targetFile = FileUtil.touch(targetFile);  // 如果文件不存在则创建
        }

        if (!targetFile.setReadable(true)) {
            throw new RuntimeException("target file is not readable");
        }

        if (!targetFile.setWritable(true)) {
            throw new RuntimeException("target file is not writable");
        }
        this.file = targetFile;
    }
}
