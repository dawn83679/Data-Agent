package edu.zsc.ai.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Internationalization utility class
 * Used to get i18n messages
 *
 * @author hhz
 */
@Component
public class I18nUtils {

    @Autowired
    private MessageSource messageSource;

    /**
     * Get i18n message
     *
     * @param code message code
     * @return i18n message
     */
    public String getMessage(String code) {
        return getMessage(code, null);
    }

    /**
     * Get i18n message with parameters
     *
     * @param code message code
     * @param args parameter array
     * @return i18n message
     */
    public String getMessage(String code, Object[] args) {
        return getMessage(code, args, "");
    }

    /**
     * Get i18n message with parameters and default message
     *
     * @param code message code
     * @param args parameter array
     * @param defaultMessage default message
     * @return i18n message
     */
    public String getMessage(String code, Object[] args, String defaultMessage) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }

    /**
     * Get current locale
     *
     * @return current Locale
     */
    public Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    /**
     * Set locale
     *
     * @param locale locale
     */
    public void setLocale(Locale locale) {
        LocaleContextHolder.setLocale(locale);
    }

    /**
     * Set locale by language code
     *
     * @param lang language code (e.g.: zh_CN, en_US)
     */
    public void setLocale(String lang) {
        if (lang == null || lang.isEmpty()) {
            return;
        }
        
        String[] parts = lang.split("_");
        Locale locale;
        if (parts.length == 2) {
            locale = new Locale(parts[0], parts[1]);
        } else {
            locale = new Locale(lang);
        }
        
        LocaleContextHolder.setLocale(locale);
    }
}
