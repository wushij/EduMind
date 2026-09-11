package com.edumind;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.edumind.**.mapper")
public class EduMindApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(EduMindApplication.class, args);
        printStartupSuccessBanner(context);
    }

    private static void printStartupSuccessBanner(ConfigurableApplicationContext context) {
        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String path = env.getProperty("server.servlet.context-path", "");
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            path = "";
        }

        String ip = "127.0.0.1";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
        }

        String[] activeProfiles = env.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "dev";

        String banner = """
                
                \u001B[32;1m==============================================\u001B[0m
                \u001B[36;1m   ______     _       __  __ _           _ \u001B[0m
                \u001B[36;1m  | ____| __| |_   _|  \\/  (_)_ __   __| |\u001B[0m
                \u001B[36;1m  |  _|  / _` | | | | |\\/| | | '_ \\ / _` |\u001B[0m
                \u001B[36;1m  | |___| (_| | |_| | |  | | | | | | (_| |\u001B[0m
                \u001B[36;1m  |_____|\\__,_|\\__,_|_|  |_|_|_| |_|\\__,_|\u001B[0m
                
                \u001B[32;1m  ★ 智教云 · EduMind —— 后端启动成功！★\u001B[0m
                
                \u001B[33m  > 本地服务入口:\u001B[0m \u001B[1mhttp://localhost:%s%s/\u001B[0m
                \u001B[33m  > 局域网络入口:\u001B[0m \u001B[1mhttp://%s:%s%s/\u001B[0m
                \u001B[33m  > 接口文档地址:\u001B[0m \u001B[34;1mhttp://localhost:%s%s/doc.html\u001B[0m
                \u001B[33m  > 当前激活环境:\u001B[0m \u001B[35;1m%s\u001B[0m
                \u001B[32;1m==============================================\u001B[0m
                """.formatted(
                port, path,
                ip, port, path,
                port, path,
                profile
        );

        System.out.println(banner);
    }
}
