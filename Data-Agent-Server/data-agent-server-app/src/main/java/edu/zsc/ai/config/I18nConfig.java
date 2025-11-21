package edu.zsc.ai.config;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * Internationalization configuration class
 *
 * @author hhz
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * Configure message source
     * Read messages files from i18n directory
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        // Set base name for i18n files (without language and extension)
        messageSource.setBasenames("i18n/messages");
        // Set default encoding
        messageSource.setDefaultEncoding("UTF-8");
        // Set cache time (seconds), -1 means permanent cache
        messageSource.setCacheSeconds(3600);
        // Return key itself when key not found
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    /**
     * Configure locale resolver
     * Get locale setting from Session
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        // Set default locale to Simplified Chinese
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return localeResolver;
    }

    /**
     * Configure locale change interceptor
     * Switch language via URL parameter lang, e.g.: ?lang=en_US
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        // Set request parameter name
        interceptor.setParamName("lang");
        return interceptor;
    }

    /**
     * Register interceptors
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
