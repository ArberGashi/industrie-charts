package com.arbergashi.charts.testutils;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

public final class EdtTestUtils {
    private EdtTestUtils() {
    }

    public static <T> T callOnEdt(Callable<T> callable) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        final Object[] holder = new Object[1];
        final RuntimeException[] err = new RuntimeException[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    holder[0] = callable.call();
                } catch (Exception e) {
                    err[0] = new RuntimeException(e);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
        if (err[0] != null) {
            throw err[0];
        }
        @SuppressWarnings("unchecked")
        T result = (T) holder[0];
        return result;
    }
}
