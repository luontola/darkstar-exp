package com.sun.sgs.impl.service.data;

/**
 * @author Esko Luontola
 * @since 24.12.2008
 */
public class TransparentReferencesHelper {

    public static void flushModifiedObjects() {
        Context context = getActiveContext();
        if (context != null) {
            context.refs.flushModifiedObjects();
        }
    }

    private static Context getActiveContext() {
        try {
            return DataServiceImpl.getContextNoJoin();
        } catch (RuntimeException e) {
            // no active context, possibly because we are in a non-durable task
            return null;
        }
    }
}
