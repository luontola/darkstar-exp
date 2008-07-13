/*
 * Copyright (c) 2008, Esko Luontola. All Rights Reserved.
 *
 * This file is part of Darkstar EXP.
 *
 * Darkstar EXP is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation and distributed hereunder to you.
 *
 * Darkstar EXP is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.orfjackal.darkstar.exp;

import com.sun.sgs.app.*;
import jdave.Block;
import jdave.Group;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.jmock.Expectations;
import org.junit.runner.RunWith;

/**
 * @author Esko Luontola
 * @since 13.7.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class DecoupleAppContextFromContextResolverSpec extends Specification<Object> {

    public void destroy() {
        AppContext.setContextResolver(null);
    }


    public class WhenAMockContextResolverIsUsed {

        private AppContextResolver resolver;

        public Object create() {
            resolver = mock(AppContextResolver.class);
            AppContext.setContextResolver(resolver);
            return null;
        }

        public void channelManagerIsMocked() {
            final ChannelManager expected = dummy(ChannelManager.class);
            checking(new Expectations() {{
                one(resolver).getChannelManager(); will(returnValue(expected));
            }});
            specify(AppContext.getChannelManager(), should.equal(expected));
        }

        public void dataManagerIsMocked() {
            final DataManager expected = dummy(DataManager.class);
            checking(new Expectations() {{
                one(resolver).getDataManager(); will(returnValue(expected));
            }});
            specify(AppContext.getDataManager(), should.equal(expected));
        }

        public void taskManagerIsMocked() {
            final TaskManager expected = dummy(TaskManager.class);
            checking(new Expectations() {{
                one(resolver).getTaskManager(); will(returnValue(expected));
            }});
            specify(AppContext.getTaskManager(), should.equal(expected));
        }

        public void allOtherManagersAreMocked() {
            final MyManager expected = dummy(MyManager.class);
            checking(new Expectations() {{
                one(resolver).getManager(MyManager.class); will(returnValue(expected));
            }});
            specify(AppContext.getManager(MyManager.class), should.equal(expected));
        }

        public void mockContextResolverCanBeRemoved() {
            AppContext.setContextResolver(null);
            specify(new Block() {
                public void run() throws Throwable {
                    AppContext.getDataManager();
                }
            }, should.raise(NullPointerException.class));
        }
    }

    private static class MyManager {
    }
}
