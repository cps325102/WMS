package com.example.wms.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${WMS_DB:${wms.db:h2}}")
    private String dbType;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        if ("mysql".equalsIgnoreCase(dbType)) {
            ds.setJdbcUrl(System.getenv().getOrDefault("WMS_MYSQL_URL",
                    "jdbc:mysql://localhost:3306/wms?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"));
            ds.setUsername(System.getenv().getOrDefault("WMS_MYSQL_USER", "root"));
            ds.setPassword(System.getenv().getOrDefault("WMS_MYSQL_PASSWORD", "sll333666sll"));
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            ds.setJdbcUrl("jdbc:h2:file:./data/wms;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE");
            ds.setUsername("sa");
            ds.setPassword("");
            ds.setDriverClassName("org.h2.Driver");
        }
        ds.setMaximumPoolSize(20);
        ds.setMinimumIdle(2);
        return ds;
    }
}
