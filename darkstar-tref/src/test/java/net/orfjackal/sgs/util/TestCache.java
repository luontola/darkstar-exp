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

package net.orfjackal.sgs.util;

import junit.framework.TestCase;

/**
 * @author Esko Luontola
 * @since 7.5.2008
 */
public class TestCache {

    public static class WhenAKeyIsUsedTheFirstTime extends TestCase {

        private int count = 0;
        private int returnValue;

        protected void setUp() throws Exception {
            Cache<String, Integer> cache = new Cache<String, Integer>() {
                protected Integer newInstance(String key) {
                    count++;
                    return key.length();
                }
            };
            assertEquals(0, count);
            returnValue = cache.get("foo");
        }

        public void testNewValueIsCreated() {
            assertEquals(1, count);
        }

        public void testTheValueIsReturned() {
            assertEquals(3, returnValue);
        }
    }

    public static class WhenAKeyIsUsedTheSecondTime extends TestCase {

        private int count = 0;
        private int returnValue1;
        private int returnValue2;

        protected void setUp() throws Exception {
            Cache<String, Integer> cache = new Cache<String, Integer>() {
                protected Integer newInstance(String key) {
                    count++;
                    return key.length();
                }
            };
            assertEquals(0, count);
            returnValue1 = cache.get("foo");
            returnValue2 = cache.get("foo");
        }

        public void testNewValueIsCreatedOnlyOnce() {
            assertEquals(1, count);
        }

        public void testBothTimesTheSameValueIsReturned() {
            assertEquals(3, returnValue1);
            assertEquals(3, returnValue2);
        }
    }
}
