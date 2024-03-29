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

package com.sun.sgs.impl.service.data;

import com.sun.sgs.app.*;
import com.sun.sgs.impl.sharedutil.*;
import net.orfjackal.dimdwarf.entities.tref.ReplaceEntitiesWithTransparentReferences;
import net.orfjackal.dimdwarf.serial.SerializationReplacer;

import java.io.*;
import java.lang.reflect.*;
import java.math.BigInteger;
import java.security.*;
import java.util.*;
import java.util.logging.*;

/** Defines serialization utilities.  This class cannot be instantiated. */
final class SerialUtil {

    /**
     * The first 4 bytes of output streams created by serialization protocol
     * version 2.
     */
    private static final byte[] SERIAL_PROTOCOL_2_HEADER = {
	(byte) 0xac, (byte) 0xed, 0x00, 0x05
    };

    /**
     * The initial byte to use in place of the initial 4 bytes of serial output
     * using serialization protocol version 2.
     */
    private static final byte SERIAL_PROTOCOL_2 = 1;

    /**
     * The initial byte to use for serial output other than serialization
     * protocol version 2.
     */
    private static final byte SERIAL_PROTOCOL_OTHER = 2;

    /** The logger for this class. */
    private static final LoggerWrapper logger =
	new LoggerWrapper(Logger.getLogger(SerialUtil.class.getName()));

    /** This class cannot be instantiated. */
    private SerialUtil() {
	throw new AssertionError();
    }

    /**
     * Converts serialized data into an object.
     *
     * @param	data the serialized data
     * @param	classSerial controls reading of class descriptors
     * @return	the object
     * @throws	ObjectIOException if a problem occurs deserializing the object
     */
    static Object deserialize(byte[] data, ClassSerialization classSerial) {
        return deserialize(data, classSerial, null);
    }

    static Object deserialize(byte[] data, ClassSerialization classSerial, Set<BigInteger> referencedObjectIds) {
	ObjectInputStream in = null;
	try {
	    in = new CustomClassDescriptorObjectInputStream(
		new CompressByteArrayInputStream(data), classSerial, getSerializationReplacers(), referencedObjectIds);
	    return in.readObject();
	} catch (ClassNotFoundException e) {
	    throw new ObjectIOException(
		"Class not found while deserializing object: " +
		e.getMessage(),
		e, false);
	} catch (IOException e) {
	    throw new ObjectIOException(
		"Problem deserializing object: " + e.getMessage(), e, false);
	} finally {
	    if (in != null) {
		try {
		    in.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    /**
     * Defines an ObjectInputStream whose reading of class descriptors is
     * customized by an instance of ClassSerialization.
     */
    private static final class CustomClassDescriptorObjectInputStream
	extends ObjectInputStream
    {
	private final ClassSerialization classSerial;
        private final SerializationReplacer[] replacers;
        private final Set<BigInteger> referencedObjectIds;

        CustomClassDescriptorObjectInputStream(InputStream in,
                                               ClassSerialization classSerial,
                                               SerializationReplacer[] replacers,
                                               Set<BigInteger> referencedObjectIds)
	    throws IOException
	{
	    super(in);
	    this.classSerial = classSerial;
            this.replacers = replacers;
            this.referencedObjectIds = referencedObjectIds;
            enableResolveObject(true);
        }
	protected ObjectStreamClass readClassDescriptor()
	    throws ClassNotFoundException, IOException
	{
	    return classSerial.readClassDescriptor(this);
	}

        protected Object resolveObject(Object obj) throws IOException {
            if (referencedObjectIds != null && obj instanceof ManagedReference) {
                ManagedReference<?> ref = (ManagedReference<?>) obj;
                referencedObjectIds.add(ref.getId());
            }
            for (SerializationReplacer replacer : replacers) {
                obj = replacer.resolveDeserialized(obj, null);
            }
            return obj;
        }
    }

    /**
     * Defines an input stream that obtains its data from a byte array, like
     * ByteArrayInputStream, and decompresses the start of the stream based on
     * its first byte.  Assumes that the first byte will either be
     * SERIAL_PROTOCOL_2 or SERIAL_PROTOCOL_OTHER.  If the value is
     * SERIAL_PROTOCOL_2, then that byte is replaced with
     * SERIAL_PROTOCOL_2_HEADER.  If it is SERIAL_PROTOCOL_OTHER, then the real
     * header is expected to follow.
     */
    private static final class CompressByteArrayInputStream
	extends ByteArrayInputStream
    {
	CompressByteArrayInputStream(byte[] bytes) throws IOException {
	    super(getBytes(bytes));
	}
	private static byte[] getBytes(byte[] bytes) throws IOException {
	    int b = (bytes.length > 0) ? bytes[0] : -1;
	    if (b == SERIAL_PROTOCOL_2) {
		byte[] result = new byte[bytes.length + 3];
		System.arraycopy(SERIAL_PROTOCOL_2_HEADER, 0, result, 0, 4);
		System.arraycopy(bytes, 1, result, 4, bytes.length - 1);
		return result;
	    } else if (b == SERIAL_PROTOCOL_OTHER) {
		byte[] result = new byte[bytes.length - 1];
		System.arraycopy(bytes, 1, result, 0, bytes.length - 1);
		return result;
	    } else {
		throw new IOException("Unexpected initial byte: " + b);
	    }
	}
    }

    /**
     * Converts an managed object into serialized data.
     *
     * @param	object the object
     * @param	classSerial controls writing of class descriptors
     * @return	the serialized data
     * @throws	ObjectIOException if a problem occurs serializing the object
     *		and, in particular, if a <code>ManagedObject</code> is
     *		referenced without an intervening <code>ManagedReference</code>
     */
    static byte[] serialize(ManagedObject object, ClassSerialization classSerial) {
        return serialize(object, classSerial, null);
    }

    static byte[] serialize(ManagedObject object, ClassSerialization classSerial, Set<BigInteger> referencedObjectIds) {
        ObjectOutputStream out = null;
	try {
	    ByteArrayOutputStream baos = new CompressByteArrayOutputStream();
            out = new CheckReferencesObjectOutputStream(
		baos, object, classSerial, getSerializationReplacers(), referencedObjectIds);
	    out.writeObject(object);
	    out.flush();
	    return baos.toByteArray();
	} catch (ObjectIOException e) {
	    check(object, e);
	    throw e;
	} catch (IOException e) {
	    throw new ObjectIOException(
		"Problem serializing object: " + e.getMessage(), e, false);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    private static SerializationReplacer[] getSerializationReplacers() {
        return new SerializationReplacer[]{
                new ReplaceEntitiesWithTransparentReferences(TransparentReferenceFactorySingleton.getInstance())
        };
    }

    /**
     * Defines a ByteArrayOutputStream that compresses the first 4 bytes if
     * they match the standard values for a serialization stream.  If those
     * bytes match SERIAL_PROTOCOL_2_HEADER, replaces them with
     * SERIAL_PROTOCOL_2.  Otherwise, prepends SERIAL_PROTOCOL_OTHER.
     */
    private static final class CompressByteArrayOutputStream
	extends ByteArrayOutputStream
    {
	CompressByteArrayOutputStream() { }
	public byte[] toByteArray() {
	    byte[] newbuf;
	    if (startsWith(SERIAL_PROTOCOL_2_HEADER)) {
		newbuf = new byte[count - 3];
		newbuf[0] = SERIAL_PROTOCOL_2;
		System.arraycopy(buf, 4, newbuf, 1, count - 4);
	    } else {
		newbuf = new byte[count + 1];
		newbuf[0] = SERIAL_PROTOCOL_OTHER;
		System.arraycopy(buf, 0, newbuf, 1, count);
	    }
	    return newbuf;
	}
	private boolean startsWith(byte[] prefix) {
	    if (count < prefix.length) {
		return false;
	    }
	    for (int i = 0; i < prefix.length; i++) {
		if (buf[i] != prefix[i]) {
		    return false;
		}
	    }
	    return true;
	}
    }

    /**
     * Defines an ObjectOutputStream whose writing of class descriptors can be
     * customized.
     */
    private static class CustomClassDescriptorObjectOutputStream
	extends ObjectOutputStream
    {
	private final ClassSerialization classSerial;

	CustomClassDescriptorObjectOutputStream(OutputStream out,
						ClassSerialization classSerial)
	    throws IOException
	{
	    super(out);
	    this.classSerial = classSerial;
	}
	protected void writeClassDescriptor(ObjectStreamClass desc)
	    throws IOException
	{
	    classSerial.writeClassDescriptor(desc, this);
	}
    }

    /**
     * Define an ObjectOutputStream that checks for references to
     * ManagedObjects not made through ManagedReferences.  Note that this
     * stream will not be able to detect direct references to the top level
     * object, except when the reference is the synthetic reference made by an
     * inner class instance.
     */
    private static final class CheckReferencesObjectOutputStream
	extends CustomClassDescriptorObjectOutputStream
    {
	/** The top level managed object being serialized. */
	private final ManagedObject topLevelObject;
        private final SerializationReplacer[] replacers;
        private final Set<BigInteger> referencedObjectIds;

        /**
	 * Creates an instance that writes to a stream for a managed object
	 * being serialized.
	 */
	CheckReferencesObjectOutputStream(OutputStream out,
                                          ManagedObject topLevelObject,
                                          ClassSerialization classSerial,
                                          SerializationReplacer[] replacers,
                                          Set<BigInteger> referencedObjectIds)
	    throws IOException
	{
	    super(out, classSerial);
	    this.topLevelObject = topLevelObject;
            this.replacers = replacers;
            this.referencedObjectIds = referencedObjectIds;
            AccessController.doPrivileged(
		new PrivilegedAction<Void>() {
		    public Void run() {
			enableReplaceObject(true);
			return null;
		    }
		});
	}

	/** Check for references to managed objects. */
	protected Object replaceObject(Object object) throws IOException {
            for (SerializationReplacer replacer : replacers) {
                object = replacer.replaceSerialized(topLevelObject, object, null);
            }
	    if (object != topLevelObject && object instanceof ManagedObject) {
		throw new ObjectIOException(
		    "ManagedObject was not referenced through a " +
		    "ManagedReference: " + Objects.safeToString(object),
		    false);
	    } else if (object != null) {
		Class<?> cl = object.getClass();
		if (cl.isAnonymousClass()) {
		    if (logger.isLoggable(Level.FINE)) {
			logger.log(
			    Level.FINE,
			    "Storing an instance of an anonymous class: " +
			    "{0}, {1}",
			    Objects.safeToString(object), cl);
		    }
		} else if (cl.isLocalClass()) {
		    if (logger.isLoggable(Level.FINE)) {
			logger.log(
			    Level.FINE,
			    "Storing an instance of a local class: {0}, {1}",
			    Objects.safeToString(object), cl);
		    }
		}
	    }
            if (referencedObjectIds != null && object instanceof ManagedReference) {
                ManagedReference<?> ref = (ManagedReference<?>) object;
                referencedObjectIds.add(ref.getId());
            }
	    return object;
	}
    }

    /**
     * Checks an object for direct references to managed objects, throwing an
     * exception that contains debugging information about where the bad
     * reference was found.
     *
     * @param	object the object to check
     * @param	cause the exception that prompted the check, or null
     * @throws	ObjectIOException if a direct reference to a managed object is
     *		found
     */
    static void check(Object object, ObjectIOException cause) {
	try {
	    new Check(object, cause).checkObject(object);
	} catch (ObjectIOException e) {
	    /* Don't include the nested stack trace from the check itself */
	    e.fillInStackTrace();
	    throw e;
	}
    }

    /**
     * Class to check for bad references.  Uses reflection to traverse the
     * object graph, keeping a stack describing the traversal for use in error
     * messages.  Although the check performed does not account for
     * serialization features such as writeReplace, the assumption is that
     * direct references to managed objects are wrong wherever they are found.
     */
    private static class Check {

	/** The top level object being checked. */
	private final Object topObject;

	/** The thrown exception that prompted the check, or null. */
	private final ObjectIOException cause;

	/** The stack that stores information about object references. */
	private final DebugStack stack = new DebugStack();

	/** Stores information about objects already seen. */
	private final IdentityHashMap<Object, Boolean> seen =
	    new IdentityHashMap<Object, Boolean>();

	/** A version of Stack that formats output appropriately. */
	private static class DebugStack extends Stack<String> {
	    /* Silence lint warning */
	    private static final long serialVersionUID = 1;
	    DebugStack() { }
	    public String push(String s) {
		return super.push((isEmpty() ? "\t-root: " : "\t-") + s);
	    }
	    public String toString() {
		StringBuilder buffer = new StringBuilder();
		if (!isEmpty()) {
		    while (true) {
			buffer.append(pop());
			if (isEmpty()) {
			    break;
			}
			buffer.append('\n');
		    }
		}
		return buffer.toString();
	    }
	}

	/**
	 * Creates an instance for the specified top level object and cause.
	 */
	Check(Object topObject, ObjectIOException cause) {
	    this.topObject = topObject;
	    this.cause = cause;
	}

	/** Checks an object for bad references. */
	void checkObject(Object object) {
	    if (object != null &&
		!(object instanceof ManagedReference) &&
		!seen.containsKey(object))
	    {
		Class<?> cl = object.getClass();
		if (object != topObject && object instanceof ManagedObject) {
		    stack.push("object (class \"" + cl.getName() + "\", " +
			       Objects.safeToString(object) + ")");
		    throw new ObjectIOException(
			"ManagedObject was not referenced through a " +
			"ManagedReference:\n" + stack,
			cause, false);
		}
		seen.put(object, Boolean.TRUE);
		if (cl.isArray()) {
		    checkArray(object);
		} else {
		    checkNonArray(object);
		}
	    }
	}

	/** Checks an array for bad reference. */
	private void checkArray(Object array) {
	    Class<?> cl = array.getClass();
	    Class<?> compCl = cl.getComponentType();
	    if (!compCl.isPrimitive()) {
		int len = Array.getLength(array);
		stack.push("array (class \"" + className(cl) +
			   "\", size: " + len + ")");
		for (int i = 0; i < len; i++) {
		    stack.push("element of array (index: " + i + ")");
		    checkObject(Array.get(array, i));
		    stack.pop();
		}
		stack.pop();
	    }
	}

	/** Checks an non-array for bad reference. */
	private void checkNonArray(Object object) {
	    Class<?> cl = object.getClass();
	    stack.push("object (class \"" + cl.getName() + "\", " +
		       Objects.safeToString(object) + ")");
	    for ( ; cl != null; cl = cl.getSuperclass()) {
		for (Field f : cl.getDeclaredFields()) {
		    if (!Modifier.isStatic(f.getModifiers()) &&
			!f.getType().isPrimitive())
		    {
			stack.push((f.isSynthetic() ? "synthetic " : "") +
				   "field (class \"" + cl.getName() +
				   "\", name: \"" + f.getName() +
				   "\", type: \"" + className(f.getType()) +
				   "\")");
			f.setAccessible(true);
			try {
			    checkObject(f.get(object));
			} catch (IllegalAccessException e) {
			    throw new AssertionError(e);
			}
			stack.pop();
		    }
		}
	    }
	    stack.pop();
	}

	/** Returns the name of a class, converting array class names. */
	private static String className(Class<?> cl) {
	    StringBuilder sb = new StringBuilder();
	    while (cl.isArray()) {
		sb.append("[]");
		cl = cl.getComponentType();
	    }
	    String className = cl.getName();
	    sb.insert(0, className);
	    return sb.toString();
	}
    }
}
