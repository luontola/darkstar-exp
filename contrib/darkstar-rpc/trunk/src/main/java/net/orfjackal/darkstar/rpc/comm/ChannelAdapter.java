/*
 * Copyright (c) 2008, Esko Luontola. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.orfjackal.darkstar.rpc.comm;

import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelListener;
import com.sun.sgs.app.ClientSession;
import net.orfjackal.darkstar.rpc.MessageReciever;
import net.orfjackal.darkstar.rpc.MessageSender;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Esko Luontola
 * @since 15.6.2008
 */
public class ChannelAdapter implements ChannelListener {

    private final long timeout;

    private final MessageSender requestSender;
    private MessageReciever responseReciever;

    private final MessageSender responseSender;
    private MessageReciever requestReciever;

    private Channel channel;
    private final RpcGateway gateway;

    public ChannelAdapter() {
        this(1000);
    }

    public ChannelAdapter(long timeout) {
        this.timeout = timeout;
        requestSender = new MyRequestSender();
        responseSender = new MyResponseSender();
        gateway = new RpcGateway(requestSender, responseSender, this.timeout);
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public RpcGateway getGateway() {
        return gateway;
    }

    public void receivedMessage(Channel channel, ClientSession sender, ByteBuffer message) {
        System.out.println("ChannelAdapter.receivedMessage");
        System.out.println("channel = " + channel);
        System.out.println("sender = " + sender);
        requestReciever.receivedMessage(ByteBufferUtils.asByteArray(message));
    }

    private class MyRequestSender implements MessageSender {

        public void send(byte[] message) throws IOException {
            System.out.println("ChannelAdapter$MyRequestSender.send");
            channel.send(null, ByteBuffer.wrap(message));
        }

        public void setCallback(MessageReciever callback) {
            responseReciever = callback;
        }
    }

    private class MyResponseSender implements MessageSender {

        public void send(byte[] message) throws IOException {
            System.out.println("ChannelAdapter$MyResponseSender.send");
            channel.send(null, ByteBuffer.wrap(message));
        }

        public void setCallback(MessageReciever callback) {
            requestReciever = callback;
        }
    }
}
