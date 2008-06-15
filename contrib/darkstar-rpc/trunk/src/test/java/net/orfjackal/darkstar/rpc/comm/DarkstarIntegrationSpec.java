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
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.client.ClientChannel;
import com.sun.sgs.client.ClientChannelListener;
import com.sun.sgs.client.ServerSessionListener;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

/**
 * @author Esko Luontola
 * @since 10.6.2008
 */
@RunWith(JDaveRunner.class)
public class DarkstarIntegrationSpec extends Specification<Object> {

    public class WhenServerCreatesARpcChannel {

        private ChannelAdapter adapter;

        public Object create() {
            ChannelManager channelManager = mock(ChannelManager.class);

            Channel channel = channelManager.createChannel("RpcChannel", adapter.getChannelListener(), Delivery.UNORDERED_RELIABLE);
            adapter.setChannel(channel);

            return null;
        }

        // TODO
    }

    public class WhenClientJoinsARpcChannel {

        private ClientChannelAdapter adapter;

        public Object create() {
            adapter = new ClientChannelAdapter();

            ServerSessionListener serverSessionListener = new NullServerSessionListener() {

                public ClientChannelListener joinedChannel(ClientChannel channel) {
                    return adapter.joinedChannel(channel);
                }

                public void receivedMessage(ByteBuffer message) {
                    adapter.receivedMessage(message);
                }
            };
            return null;
        }

        // TODO
    }


    private static class NullServerSessionListener implements ServerSessionListener {

        public ClientChannelListener joinedChannel(ClientChannel channel) {
            return null;
        }

        public void receivedMessage(ByteBuffer message) {
        }

        public void reconnecting() {
        }

        public void reconnected() {
        }

        public void disconnected(boolean graceful, String reason) {
        }
    }
}
