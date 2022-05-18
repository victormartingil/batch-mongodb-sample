package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.example.domain.User;
import com.example.listener.JobCompletionNotificationListener;
import com.example.model.UserDetail;
import com.example.processor.UserItemProcessor;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchConfiguration.class);

    @Autowired
    public JobBuilderFactory jobBuilderFactory;
    @Autowired
    public StepBuilderFactory stepBuilderFactory;


    @Bean
    public FlatFileItemReader<UserDetail> reader() {
        return new FlatFileItemReaderBuilder<UserDetail>().name("userItemReader")
                .resource(new ClassPathResource("user-sample-data.csv")).delimited()
                .names(new String[] {"email", "firstName", "lastName", "mobileNumber"})
                .fieldSetMapper(new BeanWrapperFieldSetMapper<UserDetail>() {
                    {
                        setTargetType(UserDetail.class);
                    }
                }).build();
    }

    @Bean
    public MongoItemWriter<User> writer(MongoTemplate mongoTemplate) {
        return new MongoItemWriterBuilder<User>().template(mongoTemplate).collection("user")
                .build();
    }


    @Bean
    public UserItemProcessor processor() {
        return new UserItemProcessor();
    }


    @Bean
    public Step step1(FlatFileItemReader<UserDetail> itemReader, MongoItemWriter<User> itemWriter)
            throws Exception {

        return this.stepBuilderFactory.get("step1").<UserDetail, User>chunk(5).reader(itemReader)
                .processor(processor()).writer(itemWriter).build();
    }

    @Bean
    public Job updateUserJob(JobCompletionNotificationListener listener, Step step1)
            throws Exception {

        LOGGER.info("************* Job launched *****************");

        return this.jobBuilderFactory.get("updateUserJob").incrementer(new RunIdIncrementer())
                .listener(listener).start(step1).build();
    }

}