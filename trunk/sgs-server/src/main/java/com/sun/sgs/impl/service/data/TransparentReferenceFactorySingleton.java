package com.sun.sgs.impl.service.data;

import com.google.inject.Provider;
import com.sun.sgs.app.*;
import net.orfjackal.dimdwarf.api.internal.EntityReference;
import net.orfjackal.dimdwarf.entities.ReferenceFactory;
import net.orfjackal.dimdwarf.entities.tref.*;

/**
 * @author Esko Luontola
 * @since 24.12.2008
 */
public class TransparentReferenceFactorySingleton {

    private static final TransparentReferenceFactory instance = create();

    public static TransparentReferenceFactory getInstance() {
        return instance;
    }

    private static TransparentReferenceFactory create() {
        final ReferenceFactory referenceFactory = new ReferenceFactory() {
            public <T> EntityReference<T> createReference(T entity) {
                ManagedReference<T> ref = AppContext.getDataManager().createReference(entity);
                return new EntityReferenceAdapter<T>(ref);
            }
        };
        Provider<ReferenceFactory> referenceFactoryProvider = new Provider<ReferenceFactory>() {
            public ReferenceFactory get() {
                return referenceFactory;
            }
        };
        return new TransparentReferenceFactoryImpl(referenceFactoryProvider);
    }
}
