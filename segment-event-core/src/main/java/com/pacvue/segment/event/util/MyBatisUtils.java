package com.pacvue.segment.event.util;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MyBatisUtils {
    private static final Map<DataSource, SqlSessionFactory> sqlSessionFactories = new ConcurrentHashMap<>();

    public static SqlSessionFactory getSqlSessionFactory(DataSource dataSource) {
        if (sqlSessionFactories.get(dataSource) == null) {
            synchronized (MyBatisUtils.class) {
                if (sqlSessionFactories.get(dataSource) == null) {
                    try (InputStream inputStream = MyBatisUtils.class.getClassLoader().getResourceAsStream("mybatis-config.xml")) {
                        // 创建 Environment 并绑定 DataSource
                        Environment environment = new Environment("development",
                                new JdbcTransactionFactory(), dataSource);

                        // 读取 MyBatis 配置并手动设置 Environment
                        Configuration configuration = new Configuration(environment);

                        // 重新构建 SqlSessionFactory
                        sqlSessionFactories.put(dataSource, new SqlSessionFactoryBuilder().build(configuration));
                    } catch (Exception e) {
                        throw new RuntimeException("Error initializing MyBatis", e);
                    }
                }
            }
        }
        return sqlSessionFactories.get(dataSource);
    }
}