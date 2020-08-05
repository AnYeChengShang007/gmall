package com.fjx.gmall.manage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FTPConfig {
    /*ftpconfig.host=192.168.25.141
    ftpconfig.port=21
    ftpconfig.username=ftpuser
    ftpconfig.password=ftpuser
    ftpconfig.basePath=/home/ftpuser/www/images
    ftpconfig.image_base_path=http://192.168.25.141/images
    ftpconfig.vbasePath=/home/ftpuser/www/images
    ftpconfig.video_base_path=http://192.168.25.141/images*/
    @Value("${ftpconfig.host}")
    private String host;
    @Value("${ftpconfig.port}")
    private Integer port;
    @Value("${ftpconfig.username}")
    private String username;
    @Value("${ftpconfig.password}")
    private String password;
    @Value("${ftpconfig.basePath}")
    private String basePath;
    @Value("${ftpconfig.image_base_path}")
    private String image_base_path;
    @Value("${ftpconfig.vbasePath}")
    private String vbasePath;
    @Value("${ftpconfig.video_base_path}")
    private String video_base_path;

    @Override
    public String toString() {
        return "FTPConfig{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", basePath='" + basePath + '\'' +
                ", image_base_path='" + image_base_path + '\'' +
                ", vbasePath='" + vbasePath + '\'' +
                ", video_base_path='" + video_base_path + '\'' +
                '}';
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getImage_base_path() {
        return image_base_path;
    }

    public void setImage_base_path(String image_base_path) {
        this.image_base_path = image_base_path;
    }

    public String getVbasePath() {
        return vbasePath;
    }

    public void setVbasePath(String vbasePath) {
        this.vbasePath = vbasePath;
    }

    public String getVideo_base_path() {
        return video_base_path;
    }

    public void setVideo_base_path(String video_base_path) {
        this.video_base_path = video_base_path;
    }
}
