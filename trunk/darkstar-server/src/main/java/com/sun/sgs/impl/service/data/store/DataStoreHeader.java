/*
 * Copyright 2007-2008 Sun Microsystems, Inc.
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

package com.sun.sgs.impl.service.data.store;

import com.sun.sgs.impl.service.data.store.db.DataEncoding;
import com.sun.sgs.impl.service.data.store.db.DbDatabase;
import com.sun.sgs.impl.service.data.store.db.DbTransaction;
import java.math.BigInteger;

/**
 * Encapsulates the layout of meta data stored at the start of the info
 * database, and in the classes database.  This class cannot be instantiated.
 *
 * In the info database, the value for key 0 stores a magic number common to
 * all DataStoreImpl databases.
 *
 * Key 1 stores the major version number, which must match the value in the
 * current version of the implementation.
 *
 * Key 2 stores the minor version number, which can vary between the database
 * and the implementation.
 *
 * Key 3 stores the ID of the next free ID number to use for allocating new
 * objects.
 *
 * Key 4 stores the ID of the next free transaction ID number for the network
 * version to use in allocating transactions.
 *
 * In the classes database, keys whose initial byte is 1 map the SHA-1 hash of
 * the serialized form of a class descriptor (a ObjectStreamClass) to the class
 * ID, which is 4 byte integer.
 *
 * Keys whose initial byte is 2 map a class ID to the bytes making up the
 * serialized form of the associated class descriptor.  Since these entries
 * come at the end, we can find the next class ID by using a cursor to find the
 * last entry.
 *
 * In the names database, keys are the UTF8 encoding of binding names, and
 * values are the object IDs of the associated objects.
 *
 * In the oids database, keys are object IDs, and values are the bytes
 * representing the associated objects.
 *
 * As of version 3, the serialized forms used for the object values are
 * compressed as follows:
 *
 * - If the first byte is 1, then the value was created by serialization
 *   protocol version 2, and the 4 bytes at the start of that format have been
 *   elided.  Otherwise, the first byte is 2, and is followed by the
 *   uncompressed data.
 *
 * - Class descriptors in the serialized form have been replaced by an integer
 *   which refers to a class ID stored in the classes database.  The class IDs
 *   themselves have been compressed using the Int30 class
 *
 * Version history:
 *
 * Version 1.0: Initial version, 11/3/2006
 * Version 2.0: Add NEXT_TXN_ID, 2/15/2007
 * Version 3.0: Add classes DB, compress object values, 5/18/2007
 */
final class DataStoreHeader {

    /** The key for the magic number. */
    static final long MAGIC_KEY = 0;

    /** The key for the major version number. */
    static final long MAJOR_KEY = 1;

    /** The key for the minor version number. */
    static final long MINOR_KEY = 2;

    /** The key for the value of the next free object ID. */
    static final long NEXT_OBJ_ID_KEY = 3;

    /**
     * The key for the value of the next free transaction ID, used in the
     * network version.
     */
    static final long NEXT_TXN_ID_KEY = 4;

    /** The magic number: DaRkStAr. */
    static final long MAGIC = 0x4461526b53744172L;

    /** The major version number. */
    static final short MAJOR_VERSION = 3;

    /** The minor version number. */
    static final short MINOR_VERSION = 0;

    /** The first free object ID. */
    static final long INITIAL_NEXT_OBJ_ID = 1;

    /** The first free transaction ID. */
    static final long INITIAL_NEXT_TXN_ID = 1;

    /** The first byte stored in keys for the classes database hash keys. */
    static final byte CLASS_HASH_PREFIX = 1;

    /**
     * The first byte value stored in class ID keys.  This value should be
     * greater than CLASS_HASH, to insure that class ID keys come after class
     * hash ones.
     */
    static final byte CLASS_ID_PREFIX = 2;

    /** This class cannot be instantiated. */
    private DataStoreHeader() {
	throw new AssertionError();
    }

    /**
     * Verifies the header information in the database, and returns its minor
     * version number.
     *
     * @param	db the database
     * @param	dbTxn the database transaction
     * @return	the minor version number
     * @throws	DbDatabaseException if a problem occurs accessing the database
     * @throws	DataStoreException if the format of the header information is
     *		incorrect
     */
    static int verify(DbDatabase db, DbTransaction dbTxn) {
	byte[] value =
	    db.get(dbTxn, DataEncoding.encodeLong(MAGIC_KEY), false);
	if (value == null) {
	    throw new DataStoreException("Magic number not found");
	}
	long magic = DataEncoding.decodeLong(value);
	if (magic != MAGIC) {
	    throw new DataStoreException(
		"Bad magic number in header: expected " +
		toHexString(MAGIC) + ", found " + toHexString(magic));
	}
	value = db.get(dbTxn, DataEncoding.encodeLong(MAJOR_KEY), false);
	if (value == null) {
	    throw new DataStoreException("Major version number not found");
	}
	short majorVersion = DataEncoding.decodeShort(value);
	if (majorVersion != MAJOR_VERSION) {
	    throw new DataStoreException(
		"Wrong major version number: expected " + MAJOR_VERSION +
		", found " + majorVersion);
	}
	value = db.get(dbTxn, DataEncoding.encodeLong(MINOR_KEY), false);
	if (value == null) {
	    throw new DataStoreException("Minor version number not found");
	}
	return DataEncoding.decodeShort(value);
    }

    /**
     * Stores header information in the database.
     *
     * @param	db the database
     * @param	dbTxn the database transaction
     * @throws	DbDatabaseException if a problem occurs accessing the database
     */
    static void create(DbDatabase db, DbTransaction dbTxn) {
	boolean success = db.putNoOverwrite(
	    dbTxn, DataEncoding.encodeLong(MAGIC_KEY),
	    DataEncoding.encodeLong(MAGIC));
	assert success;
	success = db.putNoOverwrite(
	    dbTxn, DataEncoding.encodeLong(MAJOR_KEY),
	    DataEncoding.encodeShort(MAJOR_VERSION));
	assert success;
	success = db.putNoOverwrite(
	    dbTxn, DataEncoding.encodeLong(MINOR_KEY),
	    DataEncoding.encodeShort(MINOR_VERSION));
	assert success;
	success = db.putNoOverwrite(
	    dbTxn, DataEncoding.encodeLong(NEXT_OBJ_ID_KEY),
	    DataEncoding.encodeLong(INITIAL_NEXT_OBJ_ID));
	assert success;
	success = db.putNoOverwrite(
	    dbTxn, DataEncoding.encodeLong(NEXT_TXN_ID_KEY),
	    DataEncoding.encodeLong(INITIAL_NEXT_TXN_ID));
	assert success;
    }

    /**
     * Returns the next available ID stored under the specified key, and
     * increments the stored value by the specified amount.  The return value
     * will be a positive number.
     *
     * @param	key the key under which the ID is stored
     * @param	db the database
     * @param	dbTxn the database transaction
     * @param	increment the amount to increment the stored amount
     * @return	the next available ID
     * @throws	DbDatabaseException if a problem occurs accessing the database
     */
    static long getNextId(long key,
			  DbDatabase db,
			  DbTransaction dbTxn,
			  long increment)
    {
	byte[] keyBytes = DataEncoding.encodeLong(key);
	byte[] valueBytes = db.get(dbTxn, keyBytes, true);
	if (valueBytes == null) {
	    throw new DataStoreException("Key not found: " + key);
	}
	long result = DataEncoding.decodeLong(valueBytes);
	db.put(dbTxn, keyBytes, DataEncoding.encodeLong(result + increment));
	return result;
    }

    /** Returns a string that describes the standard header. */
    static String headerString() {
	return headerString(MINOR_VERSION);
    }

    /**
     * Returns a string that describes the header with the specified minor
     * version number.
     */
    static String headerString(int minorVersion) {
	return "DataStoreHeader[magic:" + toHexString(MAGIC) +
	    ", version:" + MAJOR_VERSION + "." + minorVersion + "]";
    }

    /** Converts a long to a string in hexadecimal. */
    private static String toHexString(long l) {
	/* Avoid sign extension if bit 63 is set */
	BigInteger bi = BigInteger.valueOf(l & (-1L >>> 1));
	if ((l & (1L << 63)) != 0) {
	    bi = bi.setBit(63);
	}
	return "0x" + bi.toString(16);
    }
}
