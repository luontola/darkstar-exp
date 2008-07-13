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

package net.orfjackal.sgs;

/**
 * @author Esko Luontola
 * @since 27.1.2008
 */
public class TestTransparentReference_JdkProxy extends TestTransparentReference {

    private static TransparentReferenceFactory newFactory() {
        return new TransparentReferenceJdkProxyFactory();
    }

    public static class WhenCreatingATransparentReference
            extends TestTransparentReference.WhenCreatingATransparentReference {

        protected void setUp() throws Exception {
            factory = newFactory();
            super.setUp();
        }
    }

    public static class WhenManagedObjectsAreConvertedToTransparentReferencesDuringSerialization
            extends TestTransparentReference.WhenManagedObjectsAreConvertedToTransparentReferencesDuringSerialization {

        protected void setUp() throws Exception {
            factory = newFactory();
            super.setUp();
        }
    }

    public static class MarkingTransparentReferencesForUpdate
            extends TestTransparentReference.MarkingTransparentReferencesForUpdate {

        protected void setUp() throws Exception {
            factory = newFactory();
            super.setUp();
        }
    }
}
