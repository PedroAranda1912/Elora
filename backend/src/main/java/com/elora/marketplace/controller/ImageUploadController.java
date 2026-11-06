package com.elora.marketplace.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class ImageUploadController {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private final Path uploadDirectory;

    public ImageUploadController(@Value("${elora.upload-dir:./uploads}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @PostMapping(path = "/api/uploads/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha uma imagem de até 5 MB.");
        }

        String contentType = file.getContentType();
        String extension = extensionFor(contentType, file);
        String filename = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(uploadDirectory);
            Files.write(uploadDirectory.resolve(filename), file.getBytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível salvar a imagem.");
        }

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/{filename}").buildAndExpand(filename).toUriString();
        return Map.of("url", url);
    }

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@org.springframework.web.bind.annotation.PathVariable String filename) {
        if (!filename.matches("[0-9a-fA-F-]{36}\\.(png|jpg|gif|webp)")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Path file = uploadDirectory.resolve(filename).normalize();
        if (!file.getParent().equals(uploadDirectory) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        try {
            return ResponseEntity.ok()
                    .contentType(mediaTypeFor(filename))
                    .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(Files.readAllBytes(file));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private String extensionFor(String contentType, MultipartFile file) {
        byte[] header;
        try {
            header = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo.");
        }
        if (header.length < 12) throw invalidImage();

        if (MediaType.IMAGE_PNG_VALUE.equalsIgnoreCase(contentType)
                && (header[0] & 0xff) == 0x89 && header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47) return ".png";
        if (MediaType.IMAGE_JPEG_VALUE.equalsIgnoreCase(contentType)
                && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff) return ".jpg";
        if ("image/gif".equalsIgnoreCase(contentType)
                && header[0] == 'G' && header[1] == 'I' && header[2] == 'F') return ".gif";
        if ("image/webp".equalsIgnoreCase(contentType)
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') return ".webp";
        throw invalidImage();
    }

    private ResponseStatusException invalidImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma imagem PNG, JPEG, GIF ou WebP válida.");
    }

    private MediaType mediaTypeFor(String filename) {
        if (filename.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (filename.endsWith(".jpg")) return MediaType.IMAGE_JPEG;
        if (filename.endsWith(".gif")) return MediaType.IMAGE_GIF;
        return MediaType.parseMediaType("image/webp");
    }
}
