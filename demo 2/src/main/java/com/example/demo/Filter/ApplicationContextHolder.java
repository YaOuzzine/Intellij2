package com.example.demo.Filter;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Helper class to access the ApplicationContext from non-Spring managed classes.
 * This is particularly useful for static methods that need to access Spring beans.
 */
@Component
public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /**
     * Get a bean by type.
     *
     * @param beanClass The class of the bean to retrieve
     * @param <T> The type of the bean
     * @return The bean instance, or null if not found or context not set
     */
    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            return null;
        }
        try {
            return context.getBean(beanClass);
        } catch (BeansException e) {
            return null;
        }
    }

    /**
     * Get a bean by name and type.
     *
     * @param name The name of the bean
     * @param beanClass The class of the bean to retrieve
     * @param <T> The type of the bean
     * @return The bean instance, or null if not found or context not set
     */
    public static <T> T getBean(String name, Class<T> beanClass) {
        if (context == null) {
            return null;
        }
        try {
            return context.getBean(name, beanClass);
        } catch (BeansException e) {
            return null;
        }
    }
}