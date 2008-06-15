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

import jdave.Specification;
import jdave.junit4.JDaveRunner;
import net.orfjackal.darkstar.rpc.MockNetwork;
import org.jmock.Expectations;
import org.junit.runner.RunWith;

import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author Esko Luontola
 * @since 15.6.2008
 */
@RunWith(JDaveRunner.class)
public class RpcGatewaySpec extends Specification<Object> {

    private MockNetwork toMaster = new MockNetwork();
    private MockNetwork toSlave = new MockNetwork();

    private void shutdownNetwork() {
        toMaster.shutdownAndWait();
        toSlave.shutdownAndWait();
    }


    public class ARpcGateway {

        private RpcGateway gatewayToMaster;
        private RpcGateway gatewayToSlave;
        private Foo fooOnClient;
        private Foo fooOnServer;

        public Object create() {
            gatewayToMaster = new RpcGateway(toMaster.getClientToServer(), toSlave.getServerToClient(), 100);
            gatewayToSlave = new RpcGateway(toSlave.getClientToServer(), toMaster.getServerToClient(), 100);
            fooOnClient = mock(Foo.class, "fooOnClient");
            fooOnServer = mock(Foo.class, "fooOnServer");
            gatewayToMaster.registerService(Foo.class, fooOnClient);
            gatewayToSlave.registerService(Foo.class, fooOnServer);
            return null;
        }

        public void destroy() {
            shutdownNetwork();
        }

        public void allowsCallingRemoteServicesOnServer() throws Exception {
            checking(new Expectations() {{
                one(fooOnServer).serviceMethod();
//                one(fooOnServer).hello("ping?"); will(returnValue(ServiceHelper.wrap("pong!")));
            }});
            Set<Foo> servicesOnServer = gatewayToMaster.findRemoteByType(Foo.class);
            specify(servicesOnServer.size(), should.equal(1));
            Foo foo = servicesOnServer.iterator().next();
            foo.serviceMethod();
//            Future<String> f = foo.hello("ping?");
//            String s = f.get(100, TimeUnit.MILLISECONDS);
//            specify(s, should.equal("pong!"));
            shutdownNetwork();
        }
    }

    public interface Foo {

        void serviceMethod();

        Future<String> hello(String s);
    }
}
