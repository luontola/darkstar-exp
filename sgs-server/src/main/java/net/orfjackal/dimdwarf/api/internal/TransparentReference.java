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

package net.orfjackal.dimdwarf.api.internal;

/**
 * @author Esko Luontola
 * @since 31.1.2008
 */
public interface TransparentReference {

    Object getEntity();

    EntityReference<?> getEntityReference();

    Class<?> getType();

    /**
     * Returns {@code true} when (1) the other object is a transparent reference to the same entity
     * as this refers to, or (2) the other object is the same entity itself.
     * <p/>
     * This method and {@link net.orfjackal.dimdwarf.api.internal.EntityObject#equals} must follow the same contract.
     */
    boolean equals(Object obj);

    /**
     * Returns a hashCode which is remains the same through the whole lifecycle of the entity
     * (i.e. from its creation until its removal from the database).
     * <p/>
     * This method and {@link net.orfjackal.dimdwarf.api.internal.EntityObject#hashCode} must follow the same contract.
     */
    int hashCode();

    /**
     * The proxy will delegate to this method, so that the {@link net.orfjackal.dimdwarf.api.internal.TransparentReference} implementation
     * instead of the proxy will be serialized.
     */
    Object writeReplace();
}