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

package com.sun.sgs.nio.channels;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.sun.sgs.nio.channels.spi.AsynchronousChannelProvider;

public final class Channels {

    private Channels() { }

    private static void launderExecutionException(ExecutionException e)
        throws IOException
    {
        Throwable t = e.getCause();
        if (t instanceof IOException) {
            throw (IOException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else {
            throw new RuntimeException(t.getMessage(), t);
        }
    }

    private static void checkBounds(byte[] b, int off, int len) {
        if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static InputStream
    newInputStream(final AsynchronousByteChannel ch)
    {
        return new InputStream() {

            @Override
            public synchronized int read() throws IOException {
               byte[] oneByte = new byte[1];
               int rc = this.read(oneByte);
               return (rc == -1) ? -1 : oneByte[0];
            }

            @Override
            public synchronized int read(byte[] b, int off, int len)
                throws IOException
            {
                checkBounds(b, off, len);

                if (len == 0)
                    return 0;

                ByteBuffer buf = ByteBuffer.wrap(b);
                buf.position(off).limit(off + len);

                try {
                    return ch.read(buf, null).get();
                } catch (InterruptedException e) {
                    ch.close();
                    Thread.currentThread().interrupt();
                    throw new ClosedByInterruptException();
                } catch (ExecutionException e) {
                    launderExecutionException(e); // always throws
                    throw new AssertionError("unreachable");
                }
            }

            @Override
            public void close() throws IOException {
                ch.close();
            }
        };
    }

    public static OutputStream 
    newOutputStream(final AsynchronousByteChannel ch)
    {
        return new OutputStream() {

            @Override
            public synchronized void write(int b) throws IOException {
                byte[] oneByte = new byte[1];
                oneByte[0] = (byte)b;
                write(oneByte);
            }

            @Override
            public synchronized void write(byte[] b, int off, int len)
                throws IOException
            {
                checkBounds(b, off, len);

                if (len == 0)
                    return;

                ByteBuffer buf = ByteBuffer.wrap(b);
                buf.position(off).limit(off + len);

                try {
                    while (buf.hasRemaining()) {
                        ch.write(buf, null).get();
                    }
                } catch (InterruptedException e) {
                    ch.close();
                    Thread.currentThread().interrupt();
                    throw new ClosedByInterruptException();
                } catch (ExecutionException e) {
                    launderExecutionException(e); // always throws
                    throw new AssertionError("unreachable");
                }
            }

            @Override
            public void close() throws IOException {
                ch.close();
            }
        };
    }

    public static List<ChannelPoolMXBean> getChannelPoolMXBeans() {
        List<ChannelPoolMXBean> result = new LinkedList<ChannelPoolMXBean>();

        Object[] providers = new Object[] {
            SelectorProvider.provider(),
            AsynchronousChannelProvider.provider()
        };

        for (Object provider : providers) {
            if (provider instanceof ManagedChannelFactory) {
                result.addAll(
                    ((ManagedChannelFactory)provider).getChannelPoolMXBeans());
            }
        }

        return result;
    }

    public static ReadableByteChannel newChannel(InputStream in) {
        return java.nio.channels.Channels.newChannel(in);
    }

    public static WritableByteChannel newChannel(OutputStream out) {
        return java.nio.channels.Channels.newChannel(out);
    }

    public static InputStream newInputStream(ReadableByteChannel ch) {
        return java.nio.channels.Channels.newInputStream(ch);
    }

    public static OutputStream newOutputStream(WritableByteChannel ch) {
        return java.nio.channels.Channels.newOutputStream(ch);
    }

    public static Reader newReader(ReadableByteChannel ch,
                                   CharsetDecoder dec,
                                   int minBufferCap)
    {
        return java.nio.channels.Channels.newReader(ch, dec, minBufferCap);
    }

    public static Reader newReader(ReadableByteChannel ch, String csName) {
        return java.nio.channels.Channels.newReader(ch, csName);
    }

    public static Writer newWriter(WritableByteChannel ch,
                                   CharsetEncoder enc,
                                   int minBufferCap) {
        return java.nio.channels.Channels.newWriter(ch, enc, minBufferCap);
    }

    public static Writer newWriter(WritableByteChannel ch, String csName) {
        return java.nio.channels.Channels.newWriter(ch, csName);
    }
}
