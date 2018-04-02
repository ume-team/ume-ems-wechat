/*
 * Copyright 2014-2017 UME Framework Group, Apache License Version 2.0
 */
package org.umeframework.ems.wechat.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

/**
 * DefaultApplication
 * 
 * @author mayue
 *
 */
// 排除SpringBoot自带的数据源初始化设置，采用application.properties中使用ume.jdbc设置的数据源连接。
@SpringBootApplication
@EnableAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class })
// 导入项目的自动化配置。
@Import({
        // 启动UME框架的默认配置
        // org.umeframework.dora.appconfig.AutoConfiguration.class,
        org.umeframework.dora.appconfig.DefaultBeanFactoryConfiguration.class,
        org.umeframework.dora.appconfig.DefaultDaoConfiguration.class,
        org.umeframework.dora.appconfig.DefaultDataSourceConfiguration.class,
        org.umeframework.dora.appconfig.DefaultExceptionHandlerConfiguration.class,
        org.umeframework.dora.appconfig.DefaultHttpProxyConfiguration.class,
        org.umeframework.dora.appconfig.DefaultJdbcDataSourceManagerConfiguration.class,
        org.umeframework.dora.appconfig.DefaultLogConfiguration.class,
        org.umeframework.dora.appconfig.DefaultUserLoginConfiguration.class,
        org.umeframework.dora.appconfig.DefaultMessageConfiguration.class,
        org.umeframework.dora.appconfig.DefaultServiceAjaxConfiguration.class,
        org.umeframework.dora.appconfig.DefaultServiceRunnerConfiguration.class,
        org.umeframework.dora.appconfig.DefaultServiceMappingConfiguration.class,
        org.umeframework.dora.appconfig.DefaultSystemPropertyConfiguration.class,
        org.umeframework.dora.appconfig.DefaultTransactionManagerConfiguration.class,
        org.umeframework.dora.appconfig.DefaultWebControllerConfiguration.class,
        org.umeframework.ems.app.EmsConfiguration.class })
@ImportResource("classpath:config/ume-ems-wechat/initDataConfiguration.xml")
@ComponentScan(basePackages = "org.umeframework.ems")
public class Application {

	/**
	 * Start by local XML configuration style
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
