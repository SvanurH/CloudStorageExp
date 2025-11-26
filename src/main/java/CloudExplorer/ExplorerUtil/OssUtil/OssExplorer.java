package CloudExplorer.ExplorerUtil.OssUtil;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;

/**
 * OSS Explorer Logic
 * 负责构建和发送阿里云 OSS 请求。
 */
public class OssExplorer {

    private final MontoyaApi api;
    private final String endpoint;
    private final String bucketName;
    private final String region;

    // Credentials
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String securityToken;

    public OssExplorer(MontoyaApi api, String endpoint, String bucketName, String region,
                       String accessKeyId, String accessKeySecret, String securityToken) {
        this.api = api;
        this.endpoint = endpoint;
        this.bucketName = bucketName;
        this.region = region;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.securityToken = securityToken;
    }

    /**
     * 构建并发送 OSS 请求
     * @param method HTTP 方法 (GET, PUT, etc.)
     * @param path 请求路径
     * @param param 查询参数 (e.g., list-type=2)
     * @return Montoya HttpRequestResponse 对象
     */
    public HttpRequestResponse buildOssRequest(String method, String path, String param) {
        // 1. 定义服务
        HttpService service = HttpService.httpService(endpoint, 443, true);
        String date = OssUtil.getGMTDate();

        // 2. 拼接 URL Query
        String fullPath = path;
        if (param != null && !param.isEmpty()) {
            fullPath += param.startsWith("?") ? param : "?" + param;
        }

        // 3. 计算签名
        String signature = OssUtil.getAuthorization(accessKeyId, accessKeySecret, securityToken, method, bucketName, path, date);

        // 4. 手动构造请求行 (Montoya 允许这样做)
        String requestLine = String.format("%s %s HTTP/1.1\r\n\r\n", method, fullPath);

        // 5. 构建请求对象并添加 Header
        HttpRequest request = HttpRequest.httpRequest(service, requestLine)
                .withHeader("Host", endpoint)
                .withHeader("Date", date)
                .withHeader("Authorization", signature)
                .withHeader("Connection", "close");

        // 6. 如果存在 STS Token，添加 x-oss-security-token 头
        if (securityToken != null && !securityToken.trim().isEmpty()) {
            request = request.withHeader("x-oss-security-token", securityToken.trim());
        }

        // 7. 发送请求
        return api.http().sendRequest(request);
    }
}
