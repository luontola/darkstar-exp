/*
 * Copyright (c) 2008  Esko Luontola, www.orfjackal.net
 *
 * This program is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.orfjackal.darkstar.tref;

import com.sun.sgs.app.ManagedObject;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * @author Esko Luontola
 * @since 27.1.2008
 */
public class TransparentReferenceConvertingObjectOutputStream extends ObjectOutputStream {

    private final TransparentReferenceFactory factory;

    public TransparentReferenceConvertingObjectOutputStream(OutputStream out, TransparentReferenceFactory factory) throws IOException {
        super(out);
        this.factory = factory;
        enableReplaceObject(true);
    }

    protected Object replaceObject(Object obj) throws IOException {
        if (obj instanceof ManagedObject) {
            return TransparentReferenceUtil.createTransparentReferenceForSerialization((ManagedObject) obj, factory);
        }
        return obj;
    }
}
