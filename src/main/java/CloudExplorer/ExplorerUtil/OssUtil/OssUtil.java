package CloudExplorer.ExplorerUtil.OssUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;

/**
 * OSS Utility Class
 * 处理日期格式化和 HMAC-SHA1 签名计算。
 */
public class OssUtil {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /**
     * 获取符合 RFC 822 格式的 GMT 时间字符串
     */
    public static String getGMTDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return sdf.format(new Date());
    }

    /**
     * 计算阿里云 OSS Authorization 头
     *
     * @param accessKeyId     AccessKey ID
     * @param accessKeySecret AccessKey Secret
     * @param securityToken   STS Token (可选)
     * @param method          HTTP Method (GET/PUT)
     * @param bucketName      Bucket 名称
     * @param objectName      Object 路径
     * @param dateStr         GMT 格式的时间字符串
     * @return 完整的 Authorization Header 值
     */
    public static String getAuthorization(String accessKeyId, String accessKeySecret, String securityToken,
                                          String method, String bucketName,
                                          String objectName, String dateStr) {
        try {
            // 1. 构造 CanonicalizedOSSHeaders
            String canonicalizedOSSHeaders = "";
            if (securityToken != null && !securityToken.trim().isEmpty()) {
                canonicalizedOSSHeaders = "x-oss-security-token:" + securityToken.trim() + "\n";
            }

            // 2. 构造 CanonicalizedResource
            StringBuilder resourceBuilder = new StringBuilder("/");
            if (bucketName != null && !bucketName.isEmpty()) {
                resourceBuilder.append(bucketName).append("/");
                if (objectName != null && !objectName.isEmpty()) {
                    String cleanObjName = objectName.startsWith("/") ? objectName.substring(1) : objectName;
                    resourceBuilder.append(cleanObjName);
                }
            }
            String canonicalizedResource = resourceBuilder.toString();

            // 3. 构造待签名字符串
            // Format: VERB + "\n" + Content-MD5 + "\n" + Content-Type + "\n" + Date + "\n" + CanonicalizedOSSHeaders + CanonicalizedResource
            String stringToSign = method + "\n" +
                    "" + "\n" +  // Content-MD5 (留空)
                    "" + "\n" +  // Content-Type (留空)
                    dateStr + "\n" +
                    canonicalizedOSSHeaders +
                    canonicalizedResource;

            // 4. 计算 HMAC-SHA1
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(new SecretKeySpec(accessKeySecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA1_ALGORITHM));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));

            return "OSS " + accessKeyId + ":" + Base64.getEncoder().encodeToString(signData);

        } catch (Exception e) {
            // 生产环境建议记录详细日志
            System.err.println("[OssUtil] Sign Error: " + e.getMessage());
            return "";
        }
    }
}
