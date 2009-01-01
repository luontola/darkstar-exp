/*
 * Copyright (c) 2008-2009, Esko Luontola. All Rights Reserved.
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

package com.sun.sgs.agent;

import net.orfjackal.dimdwarf.aop.conf.AopTransformationChain;

import java.lang.instrument.Instrumentation;

/**
 * See http://java.sun.com/javase/6/docs/api/java/lang/instrument/package-summary.html
 * for details on using agents.
 *
 * @author Esko Luontola
 * @since 9.9.2008
 */
public class AopAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        installTransformations(inst);
    }

//    public static void agentmain(String agentArgs, Instrumentation inst) {
//        installTransformations(inst);
//    }

    private static void installTransformations(Instrumentation inst) {
        inst.addTransformer(new AopTransformationChain(new DarkstarAopApi()));
    }
}
