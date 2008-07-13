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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a value for a key, but caches the value returned by the underlying service. It is not guaranteed,
 * that in a multithreaded environment the underlying service is called at most once per a key, but otherwise
 * this class is thread-safe. The cache is never emptied during the life-time of the cache instance.
 *
 * @author Esko Luontola
 * @since 7.5.2008
 */
public abstract class Cache<K, V> {

    private final Map<K, V> cache = Collections.synchronizedMap(new HashMap<K, V>());

    public V get(K key) {
        V value = cache.get(key);
        if (value == null) {
            value = newInstance(key);
            cache.put(key, value);
        }
        return value;
    }

    protected abstract V newInstance(K key);
}
