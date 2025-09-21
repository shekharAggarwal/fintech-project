package com.fintech.schedulerservice.config;

import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

/**
 * Quartz Scheduler configuration
 */
@Configuration
public class QuartzConfig {

    private final DataSource dataSource;
    private final QuartzProperties quartzProperties;
    private final ApplicationContext applicationContext;

    public QuartzConfig(DataSource dataSource, 
                       QuartzProperties quartzProperties,
                       ApplicationContext applicationContext) {
        this.dataSource = dataSource;
        this.quartzProperties = quartzProperties;
        this.applicationContext = applicationContext;
    }

    @Bean
    public JobFactory jobFactory() {
        return new SpringJobFactory(applicationContext);
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() throws IOException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        
        factory.setDataSource(dataSource);
        factory.setJobFactory(jobFactory());
        factory.setQuartzProperties(quartzProperties());
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setAutoStartup(true);
        factory.setStartupDelay(10); // 10 seconds delay
        
        return factory;
    }

    @Bean
    public Properties quartzProperties() throws IOException {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("quartz.properties"));
        propertiesFactoryBean.afterPropertiesSet();
        
        Properties properties = propertiesFactoryBean.getObject();
        if (properties == null) {
            properties = new Properties();
        }
        
        // Override with application properties
        properties.putAll(quartzProperties.getProperties());
        
        return properties;
    }

    /**
     * Custom JobFactory to enable Spring dependency injection in Quartz jobs
     */
    private static class SpringJobFactory implements JobFactory {
        
        private final ApplicationContext applicationContext;

        public SpringJobFactory(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        public org.quartz.Job newJob(org.quartz.spi.TriggerFiredBundle bundle, 
                                    org.quartz.Scheduler scheduler) throws org.quartz.SchedulerException {
            try {
                Object job = applicationContext.getBean(bundle.getJobDetail().getJobClass());
                return (org.quartz.Job) job;
            } catch (Exception e) {
                // Fallback to default instantiation
                try {
                    return bundle.getJobDetail().getJobClass().getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    throw new org.quartz.SchedulerException("Failed to instantiate job", ex);
                }
            }
        }
    }
}