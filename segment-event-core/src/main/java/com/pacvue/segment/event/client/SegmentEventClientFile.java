package com.pacvue.segment.event.client;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.segment.analytics.messages.Message;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class SegmentEventClientFile<T extends Message> extends AbstractBufferSegmentEventClient<T, SegmentEventClientFile<T>> {
    private static final long FILE_SIZE_UNIT = 1024 * 1024L; // MB
    private File file;
    private final String path;
    private final String fileName;
    private long maxFileSizeMb = 100 * FILE_SIZE_UNIT;

    SegmentEventClientFile(@NonNull String path, @NonNull String fileName, long maxFileSizeMb) {
        this.path = path;
        this.fileName = fileName;
        this.maxFileSizeMb = maxFileSizeMb * FILE_SIZE_UNIT;
        startNewFile(path, fileName);
    }

    @Override
    public Mono<Boolean> send(List<T> events) {
        return Mono.create(sink -> {
            bundle(false);

            // 写入数据到当前文件
            FileUtil.writeLines(events, file, StandardCharsets.UTF_8, true);
            sink.success(true);
        });
    }

    @Override
    public void flush() {

    }

    private void bundle(boolean focus) {
        // 检查文件大小，如果超过 100MB，则进行滚动压缩
        if (file.length() >= maxFileSizeMb || focus) {
            File renameFile = null;
            synchronized (this) {
                if (file.length() >= maxFileSizeMb || focus) {
                    log.debug("begin to bundle file: {}", file.getAbsolutePath());
                    renameFile = renameFile(file);
                    startNewFile(path, fileName);
                }
            }
            File zipFile = compressFile(renameFile);
            afterCompressFile(zipFile);
        }
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
    private File compressFile(File file)  {
        if (null == file) return null;
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
            throw new RuntimeException("zip file error, file: " + zipFile.getAbsolutePath(), e);
        }

        // 删除原始文件，压缩后的文件保留
        FileUtil.del(file);
        return zipFile;
    }

    // 创建一个新的文件进行写入
    private void startNewFile(String path, String fileName) {
        File targetFile = FileUtil.file(FileUtil.getTmpDir(), path, fileName);
        // 目标文件不存在，创建文件
        if (!targetFile.exists()) {
            // 检查目录是否存在，如果不存在则创建
            if (!targetFile.getParentFile().exists() && !targetFile.getParentFile().mkdirs()) {
                throw new RuntimeException("create parent dir failed, parent dir: " + targetFile.getParentFile());
            }
            // 创建文件（如果文件不存在）
            targetFile = FileUtil.touch(targetFile);  // 如果文件不存在则创建
        }

        if (!targetFile.setReadable(true)) {
            throw new RuntimeException("target file is not readable, file: " + targetFile.getAbsolutePath());
        }

        if (!targetFile.setWritable(true)) {
            throw new RuntimeException("target file is not writable， file: " + targetFile.getAbsolutePath());
        }
        this.file = targetFile;
    }

    // 如果应用容器未将数据卷和宿主机器绑定，在容器重启时候最好调用该方法并且实现afterCompressFile，将数据上传到远端，避免数据丢失
    protected void destroy() {
        bundle(true);
    }

    // 预留扩展，如果后续需要对zip文件进行处理，可以通过重写该方法实现，比如将文件上传到S3
    protected void afterCompressFile(File zipFile) {}


    public static <T extends Message> SegmentEventClientFile.Builder<T> builder() {
        return new SegmentEventClientFile.Builder<>();
    }

    public static class Builder<T extends Message> {
        private String path;
        private String fileName;
        private long maxFileSizeMb = 100;

        public SegmentEventClientFile.Builder<T> path(String path) {
            this.path = path;
            return this;
        }

        public SegmentEventClientFile.Builder<T> fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public SegmentEventClientFile.Builder<T> maxFileSizeMb(long maxFileSizeMb) {
            this.maxFileSizeMb = maxFileSizeMb;
            return this;
        }

        public SegmentEventClientFile<T> build() {
            return new SegmentEventClientFile<>(path, fileName, maxFileSizeMb);
        }
    }
}
