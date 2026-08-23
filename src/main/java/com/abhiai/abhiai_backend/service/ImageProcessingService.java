package com.abhiai.abhiai_backend.service;

import java.awt.*; import java.awt.image.BufferedImage; import java.io.*; import java.util.*;
import javax.imageio.*; import javax.imageio.stream.ImageOutputStream;
import org.springframework.scheduling.annotation.Async; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.transaction.event.TransactionPhase; import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import com.abhiai.abhiai_backend.entity.*; import com.abhiai.abhiai_backend.repository.MediaAssetRepository;

@Service
public class ImageProcessingService {
    private final MediaAssetRepository repository; private final MediaStorage storage;
    public ImageProcessingService(MediaAssetRepository repository,MediaStorage storage){this.repository=repository;this.storage=storage;}
    @Async @Transactional(propagation=Propagation.REQUIRES_NEW) @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void process(MediaUploadedEvent event){
        MediaAsset asset=repository.findById(event.mediaId()).orElse(null); if(asset==null||asset.getProcessingStatus()!=MediaProcessingStatus.PENDING)return;
        String optimized=asset.getId()+"-optimized.jpg",thumbnail=asset.getId()+"-thumbnail.jpg";
        try(InputStream input=storage.load(asset.getStorageKey()).getInputStream()){
            BufferedImage source=ImageIO.read(input); if(source==null)throw new IOException("Unsupported image data");
            byte[] optimizedBytes=encode(resize(source,1920),.85f); byte[] thumbnailBytes=encode(resize(source,480),.80f);
            storage.store(optimized,new ByteArrayInputStream(optimizedBytes),optimizedBytes.length,"image/jpeg");
            storage.store(thumbnail,new ByteArrayInputStream(thumbnailBytes),thumbnailBytes.length,"image/jpeg");
            asset.processingCompleted(optimized,thumbnail); repository.save(asset);
        }catch(Exception exception){storage.delete(optimized);storage.delete(thumbnail);asset.processingFailed();repository.save(asset);}
    }
    private BufferedImage resize(BufferedImage source,int max){double scale=Math.min(1d,(double)max/Math.max(source.getWidth(),source.getHeight()));int width=Math.max(1,(int)Math.round(source.getWidth()*scale)),height=Math.max(1,(int)Math.round(source.getHeight()*scale));BufferedImage target=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=target.createGraphics();graphics.setColor(Color.WHITE);graphics.fillRect(0,0,width,height);graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);graphics.drawImage(source,0,0,width,height,null);graphics.dispose();return target;}
    private byte[] encode(BufferedImage image,float quality)throws IOException{ByteArrayOutputStream output=new ByteArrayOutputStream();ImageWriter writer=ImageIO.getImageWritersByFormatName("jpeg").next();try(ImageOutputStream stream=ImageIO.createImageOutputStream(output)){writer.setOutput(stream);ImageWriteParam params=writer.getDefaultWriteParam();params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);params.setCompressionQuality(quality);writer.write(null,new IIOImage(image,null,null),params);}finally{writer.dispose();}return output.toByteArray();}
}
