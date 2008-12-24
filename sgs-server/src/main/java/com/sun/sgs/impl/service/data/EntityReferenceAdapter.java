package com.sun.sgs.impl.service.data;

import com.sun.sgs.app.ManagedReference;
import net.orfjackal.dimdwarf.api.internal.EntityReference;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 24.12.2008
 */
public class EntityReferenceAdapter<T> implements EntityReference<T>, Serializable {
    private static final long serialVersionUID = 1;

    private final ManagedReference<T> ref;

    public EntityReferenceAdapter(ManagedReference<T> ref) {
        this.ref = ref;
    }

    public T get() {
        return ref.get();
    }

    public BigInteger getEntityId() {
        return ref.getId();
    }
}
