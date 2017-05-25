package org.syncany.operations.down;
/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.junit.Before;
import org.junit.Test;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.VectorClock;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class DatabaseVersionHeaderComparatorTest {
    private DatabaseVersionHeader dbvh1, dbvh2;
    private VectorClock vc1, vc2;

    @Before
    public void setUp() {
        // Set up basic DatabaseVersionHeaders and VectorClocks.

        vc1 = new VectorClock();
        vc1.setClock("A", 5);
        vc1.setClock("B", 7);

        dbvh1 = new DatabaseVersionHeader();
        dbvh1.setClient("A");
        dbvh1.setDate(new Date(0xDEADDEAD));
        dbvh1.setVectorClock(vc1);

        vc2 = new VectorClock();
        vc2.setClock("A", 5);
        vc2.setClock("B", 7);

        dbvh2 = new DatabaseVersionHeader();
        dbvh2.setClient("A");
        dbvh2.setDate(new Date(0xDEADDEAD));
        dbvh2.setVectorClock(vc2);
    }

    @Test
    public void testCompareDatabaseVersionHeaderEqual() {
        DatabaseVersionHeaderComparator databaseVersionHeaderComparator = new DatabaseVersionHeaderComparator(true);

        assertThat(databaseVersionHeaderComparator.compare(dbvh1, dbvh2), is(0));
    }

    @Test
    public void testCompareDatabaseVersionHeaderEqualIgnoreTime() {
        DatabaseVersionHeaderComparator databaseVersionHeaderComparator = new DatabaseVersionHeaderComparator(false);

        dbvh2.setDate(new Date(0xFEBEBEBE));

        assertThat(databaseVersionHeaderComparator.compare(dbvh1, dbvh2), is(0));
    }

    @Test
    public void testCompareDatabaseVersionHeaderSimultaneous() {
        DatabaseVersionHeaderComparator databaseVersionHeaderComparator = new DatabaseVersionHeaderComparator(true);
        vc1.setClock("A", 3);
        vc1.setClock("B", 7);

        vc2.setClock("A", 5);
        vc2.setClock("B", 4);

        dbvh2.setDate(new Date(0xFEBEBEBE));

        assertThat(databaseVersionHeaderComparator.compare(dbvh1, dbvh2), is(-1));
    }

    @Test
    public void testCompareDatabaseVersionHeaderSimultaneousIgnoreTime() {
        DatabaseVersionHeaderComparator databaseVersionHeaderComparator = new DatabaseVersionHeaderComparator(false);

        vc1.setClock("A", 3);
        vc1.setClock("B", 7);

        vc2.setClock("A", 5);
        vc2.setClock("B", 4);

        dbvh2.setDate(new Date(0xFEBEBEBE));

        assertThat(databaseVersionHeaderComparator.compare(dbvh1, dbvh2), is(0));
    }

    @Test
    public void testCompareDatabaseVersionHeaderLarger() {
        DatabaseVersionHeaderComparator databaseVersionHeaderComparator = new DatabaseVersionHeaderComparator(false);

        vc1.setClock("A", 3);
        vc1.setClock("B", 7);

        vc2.setClock("A", 1);
        vc2.setClock("B", 7);

        assertThat(databaseVersionHeaderComparator.compare(dbvh1, dbvh2), is(1));
    }

    @Test
    public void testCompareDatabaseVersionHeaderSmaller() {
        DatabaseVersionHeaderComparator databaseVersionHeaderComparator = new DatabaseVersionHeaderComparator(false);

        vc1.setClock("A", 3);
        vc1.setClock("B", 7);

        vc2.setClock("A", 5);
        vc2.setClock("B", 7);

        dbvh2.setDate(new Date(0xFEBEBEBE));

        assertThat(databaseVersionHeaderComparator.compare(dbvh1, dbvh2), is(-1));
    }

    @Test
    public void testCompareDatabaseVersionHeaderDifferentClients() {
        DatabaseVersionHeaderComparator databaseVersionHeaderComparator = new DatabaseVersionHeaderComparator(true);

        dbvh2.setClient("B");

        assertThat(databaseVersionHeaderComparator.compare(dbvh1, dbvh2), is(-1));
    }



}