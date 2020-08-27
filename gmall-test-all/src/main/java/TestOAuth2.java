import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

public class TestOAuth2 {

    public static void main(String[] args) {
        //https://api.weibo.com/oauth2/authorize?client_id=YOUR_CLIENT_ID&response_type=code&redirect_uri=YOUR_REGISTERED_REDIRECT_URI
        //https://api.weibo.com/oauth2/authorize?client_id=639265612&response_type=code&redirect_uri=http://127.0.0.1:8085/vlogin

        //https://api.weibo.com/oauth2/access_token?client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET&grant_type=authorization_code&redirect_uri=YOUR_REGISTERED_REDIRECT_URI&code=CODE
        //https://api.weibo.com/oauth2/access_token?client_id=639265612&client_secret=51d0faaeb509cc5cbc71f9489a066a68&grant_type=authorization_code&redirect_uri=http://127.0.0.1:8085/vlogin&code=CODE
        String s = HttpclientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=639265612&response_type=code&redirect_uri=http://127.0.0.1:8085/vlogin");
        System.out.println(s);
        Map<String,String> map = new HashMap<String, String>();
        map.put("client_id","639265612");
        map.put("client_secret","51d0faaeb509cc5cbc71f9489a066a68");
        map.put("grant_type","authorization_code");
        map.put("redirect_uri","http://127.0.0.1:8085/vlogin");
        map.put("code","2afe3c2cea54b00f9ebe371349639feb");
        String res = HttpclientUtil.doPost("https://api.weibo.com/oauth2/access_token", map);
        Map<String,String> m = JSON.parseObject(res, Map.class);

        System.out.println();

        //https://api.weibo.com/2/users/show.json?access_token=2.002B_sfF0gLSQh0deda948dbDhAINC&uid=5199436767
    }
}
