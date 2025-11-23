package edu.zsc.ai.util;

import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Conditional utility class
 * Provides concise conditional checking and assignment operations
 *
 * @author zgq
 */
public class ConditionalUtil {

    /**
     * If string is not blank, execute setter operation
     *
     * @param value  Value to check
     * @param setter Setter method (usually object's setter)
     * @example ConditionalUtil.setIfNotBlank(user.getName (), target::setUsername);
     */
    public static void setIfNotBlank(String value, Consumer<String> setter) {
        if (StringUtils.isNotBlank(value)) {
            setter.accept(value);
        }
    }

    /**
     * If string is not blank, execute setter operation, otherwise use default value
     *
     * @param value        Value to check
     * @param defaultValue Default value
     * @param setter       Setter method
     * @example ConditionalUtil.setIfNotBlankOrElse(user.getName (), "Anonymous", target::setUsername);
     */
    public static void setIfNotBlankOrElse(String value, String defaultValue, Consumer<String> setter) {
        setter.accept(StringUtils.isNotBlank(value) ? value : defaultValue);
    }

    /**
     * If string is not blank, execute setter operation, otherwise use default value supplier
     *
     * @param value           Value to check
     * @param defaultSupplier Default value supplier
     * @param setter          Setter method
     * @example ConditionalUtil.setIfNotBlankOrElse(
     *user.getName (),
     * () -> user.getEmail().split("@")[0],
     * target::setUsername
     * );
     */
    public static void setIfNotBlankOrElse(String value, Supplier<String> defaultSupplier, Consumer<String> setter) {
        setter.accept(StringUtils.isNotBlank(value) ? value : defaultSupplier.get());
    }

    /**
     * If object is not null, execute setter operation
     *
     * @param value  Value to check
     * @param setter Setter method
     * @param <T>    Value type
     * @example ConditionalUtil.setIfNotNull(user.getAge (), target::setAge);
     */
    public static <T> void setIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    /**
     * If object is not null, execute setter operation, otherwise use default value
     *
     * @param value        Value to check
     * @param defaultValue Default value
     * @param setter       Setter method
     * @param <T>          Value type
     * @example ConditionalUtil.setIfNotNullOrElse(user.getAge (), 18, target::setAge);
     */
    public static <T> void setIfNotNullOrElse(T value, T defaultValue, Consumer<T> setter) {
        setter.accept(value != null ? value : defaultValue);
    }

    /**
     * If object is not null, execute setter operation, otherwise use default value supplier
     *
     * @param value           Value to check
     * @param defaultSupplier Default value supplier
     * @param setter          Setter method
     * @param <T>             Value type
     * @example ConditionalUtil.setIfNotNullOrElse(
     *user.getAge (),
     * () -> calculateDefaultAge(),
     * target::setAge
     * );
     */
    public static <T> void setIfNotNullOrElse(T value, Supplier<T> defaultSupplier, Consumer<T> setter) {
        setter.accept(value != null ? value : defaultSupplier.get());
    }

    /**
     * If condition is true, execute setter operation
     *
     * @param condition Condition
     * @param value     Value to set
     * @param setter    Setter method
     * @param <T>       Value type
     * @example ConditionalUtil.setIf(user.isVip (), "VIP", target::setLevel);
     */
    public static <T> void setIf(boolean condition, T value, Consumer<T> setter) {
        if (condition) {
            setter.accept(value);
        }
    }

    /**
     * Set different values based on condition
     *
     * @param condition  Condition
     * @param trueValue  Value when condition is true
     * @param falseValue Value when condition is false
     * @param setter     Setter method
     * @param <T>        Value type
     * @example ConditionalUtil.setByCondition(user.isVip (), "VIP", "Normal", target::setLevel);
     */
    public static <T> void setByCondition(boolean condition, T trueValue, T falseValue, Consumer<T> setter) {
        setter.accept(condition ? trueValue : falseValue);
    }

    /**
     * If object is not null, execute action (not assignment, but execute a piece of logic)
     *
     * @param value  Value to check
     * @param action Action to execute
     * @param <T>    Value type
     * @example ConditionalUtil.ifNotNull(session, s - > {
     *s.setActive ( false);
     * sessionService.updateById(s);
     * });
     */
    public static <T> void ifNotNull(T value, Consumer<T> action) {
        if (value != null) {
            action.accept(value);
        }
    }

    /**
     * If string is not blank, execute action
     *
     * @param value  Value to check
     * @param action Action to execute
     * @example ConditionalUtil.ifNotBlank(email, e - > sendEmail ( e));
     */
    public static void ifNotBlank(String value, Consumer<String> action) {
        if (StringUtils.isNotBlank(value)) {
            action.accept(value);
        }
    }

    /**
     * If condition is true, execute action
     *
     * @param condition Condition
     * @param action    Action to execute
     * @example ConditionalUtil.ifTrue(user.isVip (), () -> sendVipEmail(user));
     */
    public static void ifTrue(boolean condition, Runnable action) {
        if (condition) {
            action.run();
        }
    }

    /**
     * Chained conditional setter builder
     * Supports chained calls with multiple conditions
     *
     * @param <T> Target object type
     * @example ConditionalUtil.builder(user)
     * .setIfNotBlank(request.getName(), User::setUsername)
     * .setIfNotBlank(request.getPicture(), User::setAvatarUrl)
     * .setIfNotNull(request.getAge(), User::setAge)
     * .build();
     */
    public static <T> ConditionalBuilder<T> builder(T target) {
        return new ConditionalBuilder<>(target);
    }

    /**
     * Conditional setter builder
     */
    public static class ConditionalBuilder<T> {
        private final T target;

        private ConditionalBuilder(T target) {
            this.target = target;
        }

        /**
         * If string is not blank, set
         */
        public ConditionalBuilder<T> setIfNotBlank(String value, Consumer<String> setter) {
            ConditionalUtil.setIfNotBlank(value, setter);
            return this;
        }

        /**
         * If string is not blank, set, otherwise use default value
         */
        public ConditionalBuilder<T> setIfNotBlankOrElse(String value, String defaultValue, Consumer<String> setter) {
            ConditionalUtil.setIfNotBlankOrElse(value, defaultValue, setter);
            return this;
        }

        /**
         * If string is not blank, set, otherwise use default value supplier
         */
        public ConditionalBuilder<T> setIfNotBlankOrElse(String value, Supplier<String> defaultSupplier, Consumer<String> setter) {
            ConditionalUtil.setIfNotBlankOrElse(value, defaultSupplier, setter);
            return this;
        }

        /**
         * If object is not null, set
         */
        public <V> ConditionalBuilder<T> setIfNotNull(V value, Consumer<V> setter) {
            ConditionalUtil.setIfNotNull(value, setter);
            return this;
        }

        /**
         * If object is not null, set, otherwise use default value
         */
        public <V> ConditionalBuilder<T> setIfNotNullOrElse(V value, V defaultValue, Consumer<V> setter) {
            ConditionalUtil.setIfNotNullOrElse(value, defaultValue, setter);
            return this;
        }

        /**
         * If condition is true, set
         */
        public <V> ConditionalBuilder<T> setIf(boolean condition, V value, Consumer<V> setter) {
            ConditionalUtil.setIf(condition, value, setter);
            return this;
        }

        /**
         * Complete build, return target object
         */
        public T build() {
            return target;
        }
    }
}

