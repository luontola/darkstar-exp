/*
 * Copyright (c) 2008-2009, Esko Luontola. All Rights Reserved.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
