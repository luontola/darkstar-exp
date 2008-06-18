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

import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;

/**
 * @author Esko Luontola
 * @since 18.6.2008
 */
@RunWith(JDaveRunner.class)
public class AppRootAsRelativeToAppPropertiesFileSpec extends Specification<Object> {

    public class WhenAppPropertiesFileIsInServerHomeDir {

        public Object create() {
            return null;
        }

        public void absoluteAppRootIsAbsolute() {
            // TODO
        }

        public void relativeAppRootIsRelativeToServerHome() {
            // TODO
        }
    }

    public class WhenAppPropertiesFileIsInAnotherDirectory {

        public Object create() {
            return null;
        }

        public void absoluteAppRootIsAbsolute() {
            // TODO
        }

        public void relativeAppRootIsRelativeToDirectoryOfAppPropertiesFile() {
            // TODO
        }
    }
}
