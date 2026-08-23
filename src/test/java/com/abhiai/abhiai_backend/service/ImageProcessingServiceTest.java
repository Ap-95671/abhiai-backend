package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.awt.image.BufferedImage; import java.io.*; import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.Mock; import org.mockito.junit.jupiter.MockitoExtension; import org.springframework.core.io.ByteArrayResource;
import com.abhiai.abhiai_backend.entity.*; import com.abhiai.abhiai_backend.repository.MediaAssetRepository;

@ExtendWith(MockitoExtension.class)
class ImageProcessingServiceTest {
    @Mock MediaAssetRepository repository; @Mock MediaStorage storage;
    @Test void createsOptimizedAndThumbnailImages() throws Exception {
        UUID id=UUID.randomUUID(); User owner=new User("owner","Owner","owner@example.com","hash"); MediaAsset asset=new MediaAsset(id,owner,id+".png","photo.png","image/png",200);
        BufferedImage image=new BufferedImage(800,600,BufferedImage.TYPE_INT_RGB); ByteArrayOutputStream bytes=new ByteArrayOutputStream(); ImageIO.write(image,"png",bytes);
        when(repository.findById(id)).thenReturn(Optional.of(asset)); when(storage.load(asset.getStorageKey())).thenReturn(new ByteArrayResource(bytes.toByteArray()));

        new ImageProcessingService(repository,storage).process(new MediaUploadedEvent(id));

        assertEquals(MediaProcessingStatus.COMPLETED,asset.getProcessingStatus());
        verify(storage,times(2)).store(anyString(),any(InputStream.class),anyLong(),eq("image/jpeg")); verify(repository).save(asset);
    }
}
