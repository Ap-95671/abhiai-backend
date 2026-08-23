package com.abhiai.abhiai_backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.abhiai.abhiai_backend.config.MediaProperties;
import com.abhiai.abhiai_backend.dto.media.MediaAssetResponse;
import com.abhiai.abhiai_backend.entity.MediaAsset;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidMediaException;
import com.abhiai.abhiai_backend.exception.MediaNotFoundException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.MediaAssetRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;
import com.abhiai.abhiai_backend.repository.StoryRepository;

@Service
public class MediaService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm");
    private static final Set<String> DOCUMENT_TYPES = Set.of("application/pdf");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/gif", "gif", "image/webp", "webp",
            "video/mp4", "mp4", "video/webm", "webm", "application/pdf", "pdf");

    private final MediaAssetRepository repository;
    private final UserRepository users;
    private final MediaStorage storage;
    private final MediaProperties properties;
    private final PostAccessService postAccess;
    private final ApplicationEventPublisher events;
    private final StoryRepository stories;

    public MediaService(MediaAssetRepository repository, UserRepository users, MediaStorage storage,
            MediaProperties properties, PostAccessService postAccess, ApplicationEventPublisher events,
            StoryRepository stories) {
        this.repository = repository;
        this.users = users;
        this.storage = storage;
        this.properties = properties;
        this.postAccess = postAccess;
        this.events = events;
        this.stories = stories;
    }

    @Transactional
    public MediaAssetResponse uploadImage(UUID userId, MultipartFile file) {
        requireType(file, IMAGE_TYPES, "Only JPEG, PNG, GIF, and WebP images are supported");
        return store(userId, file);
    }

    @Transactional
    public MediaAssetResponse uploadAttachment(UUID userId, MultipartFile file) {
        requireType(file, Set.copyOf(EXTENSIONS.keySet()), "Only images, MP4/WebM videos, and PDF documents are supported");
        return store(userId, file);
    }

    private MediaAssetResponse store(UUID userId, MultipartFile file) {
        String type = file.getContentType().toLowerCase(Locale.ROOT);
        long limit = sizeLimit(type);
        if (file.getSize() > limit) {
            throw new InvalidMediaException("File exceeds the " + limit / 1024 / 1024 + " MB limit for this media type");
        }
        verifySignature(file, type);
        User owner = users.findById(userId).orElseThrow(UserNotFoundException::new);
        UUID id = UUID.randomUUID();
        String key = id + "." + EXTENSIONS.get(type);
        String name = Path.of(Optional.ofNullable(file.getOriginalFilename()).orElse("attachment." + EXTENSIONS.get(type)))
                .getFileName().toString();
        if (name.length() > 255) name = name.substring(name.length() - 255);
        try (InputStream input = file.getInputStream()) {
            storage.store(key, input, file.getSize(), type);
        } catch (IOException exception) {
            throw new InvalidMediaException("File could not be read");
        }
        try {
            MediaAsset saved=repository.saveAndFlush(new MediaAsset(id, owner, key, name, type, file.getSize()));
            if(saved.getProcessingStatus()==com.abhiai.abhiai_backend.entity.MediaProcessingStatus.PENDING)events.publishEvent(new MediaUploadedEvent(saved.getId()));
            return MediaAssetResponse.from(saved);
        } catch (RuntimeException exception) {
            storage.delete(key);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Download download(UUID userId, UUID id, boolean thumbnail) {
        MediaAsset asset = repository.findById(id).orElseThrow(MediaNotFoundException::new);
        if (!asset.getOwner().getId().equals(userId) && !users.isUsedAsProfileMedia(id)) {
            if (asset.getPost() != null) {
                postAccess.findViewablePost(userId, asset.getPost().getId());
            } else if (stories.findByMediaIdAndExpiresAtAfter(id, java.time.Instant.now()).isEmpty()) {
                throw new UnauthorizedActionException("You cannot access this media");
            }
        }
        String key=thumbnail&&asset.getThumbnailStorageKey()!=null?asset.getThumbnailStorageKey():asset.deliveryStorageKey();
        String type=key.equals(asset.getStorageKey())?asset.getContentType():"image/jpeg";
        return new Download(storage.load(key), type, asset.getOriginalFilename());
    }

    @Transactional
    public void deleteUnattached(UUID userId, UUID id) {
        MediaAsset asset = repository.findById(id).orElseThrow(MediaNotFoundException::new);
        if (!asset.getOwner().getId().equals(userId)) throw new UnauthorizedActionException("You cannot delete this media");
        if (asset.getPost() != null || users.isUsedAsProfileMedia(id) || stories.existsByMediaId(id)
                || repository.isUsedByConversation(id)) {
            throw new InvalidMediaException("Media currently in use cannot be deleted separately");
        }
        repository.delete(asset);
        repository.flush();
        storage.delete(asset.getStorageKey());
        if(asset.getOptimizedStorageKey()!=null)storage.delete(asset.getOptimizedStorageKey());
        if(asset.getThumbnailStorageKey()!=null)storage.delete(asset.getThumbnailStorageKey());
    }

    private void requireType(MultipartFile file, Set<String> allowed, String message) {
        if (file == null || file.isEmpty()) throw new InvalidMediaException("Choose a file to upload");
        String type = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        if (!allowed.contains(type)) throw new InvalidMediaException(message);
    }

    private long sizeLimit(String type) {
        if (IMAGE_TYPES.contains(type)) return properties.getMaxImageBytes();
        if (VIDEO_TYPES.contains(type)) return properties.getMaxVideoBytes();
        return properties.getMaxDocumentBytes();
    }

    private void verifySignature(MultipartFile file, String type) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(16);
            boolean valid = switch (type) {
                case "image/jpeg" -> header.length >= 3 && (header[0] & 255) == 255 && (header[1] & 255) == 216 && (header[2] & 255) == 255;
                case "image/png" -> header.length >= 8 && (header[0] & 255) == 137 && header[1] == 80 && header[2] == 78 && header[3] == 71;
                case "image/gif" -> startsWith(header, "GIF");
                case "image/webp" -> startsWith(header, "RIFF") && containsAt(header, "WEBP", 8);
                case "video/mp4" -> containsAt(header, "ftyp", 4);
                case "video/webm" -> header.length >= 4 && (header[0] & 255) == 0x1A && (header[1] & 255) == 0x45 && (header[2] & 255) == 0xDF && (header[3] & 255) == 0xA3;
                case "application/pdf" -> startsWith(header, "%PDF-");
                default -> false;
            };
            if (!valid) throw new InvalidMediaException("File content does not match its declared media type");
        } catch (IOException exception) {
            throw new InvalidMediaException("File could not be read");
        }
    }

    private boolean startsWith(byte[] bytes, String value) { return containsAt(bytes, value, 0); }
    private boolean containsAt(byte[] bytes, String value, int offset) {
        byte[] expected = value.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length < offset + expected.length) return false;
        for (int index = 0; index < expected.length; index++) if (bytes[offset + index] != expected[index]) return false;
        return true;
    }

    public record Download(Resource resource, String contentType, String filename) {}
}
