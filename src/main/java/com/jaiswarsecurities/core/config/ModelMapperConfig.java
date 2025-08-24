package com.jaiswarsecurities.core.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        
        // Configure mapping strategy for high performance
        mapper.getConfiguration()
            .setMatchingStrategy(MatchingStrategies.STRICT)
            .setFieldMatchingEnabled(true)
            .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
            .setMethodAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PUBLIC);

        // Custom mappings can be added here
        // mapper.addMappings(new PropertyMap<Source, Destination>() { ... });

        return mapper;
    }
}
