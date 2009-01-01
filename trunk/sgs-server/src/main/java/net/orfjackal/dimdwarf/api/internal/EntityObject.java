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

import com.sun.sgs.app.ManagedObject;

/**
 * Marker interface equivalent to Darkstar's ManagedObject.
 * <p/>
 * For performance and Darkstar compatibility reasons the system uses internally only this interface,
 * and not the {@link @Entity} annotation. Classes annotated with {@code @Entity} will be automatically
 * transformed to implement this interface.
 *
 * @author Esko Luontola
 * @since 15.8.2008
 */
public interface EntityObject extends ManagedObject {
}