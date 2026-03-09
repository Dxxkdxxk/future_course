package com.lzlz.springboot.security.config;

import cn.hutool.core.codec.Base64;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码生成工具类（转Base64）
 */
public class QRCodeUtil {

    /**
     * 生成二维码Base64编码
     * @param content 二维码内容
     * @param width 二维码宽度
     * @param height 二维码高度
     * @return Base64编码字符串（可直接用于前端img标签）
     */
    public static String generateQRCodeBase64(String content, int width, int height) {
        try {
            // 二维码配置
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // 纠错级别
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8"); // 字符集
            hints.put(EncodeHintType.MARGIN, 1); // 边距

            // 生成二维码矩阵
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            // 矩阵转图片流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageConfig config = new MatrixToImageConfig(0xFF000000, 0xFFFFFFFF); // 黑底白边
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream, config);

            // 转Base64
            return "data:image/png;base64," + Base64.encode(outputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("二维码生成失败", e);
        }
    }
}
