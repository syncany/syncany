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
package org.syncany.database;

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements a vector clock that records the time stamps of all send and receive
 * events. It contains functions to compare and merge two vector clocks.
 *
 * <p>Vector clocks are a mechanism to track events/actions across multiple distributed
 * clients. Using vector clocks, one can determine relationships between different events.
 * In particular:
 *
 * <ul>
 *  <li>Event A happened before / was caused by event B (cause)</li>
 *  <li>Event B happened after / caused event B (effect)</li>
 *  <li>Event A and B happened simultaneously (no cause/effect relationship)</li>
 * </ul>
 *
 * @author Frits de Nijs
 * @author Peter Dijkshoorn
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class VectorClock extends TreeMap<String, Long> {
	private static final long serialVersionUID = 109876543L;	
	
	private static final Pattern CLOCK_PATTERN = Pattern.compile("\\(([^)]*)\\)");
	private static final int CLOCK_PATTERN_GROUP_CONTENT = 1;
	
	private static final Pattern ENTRY_PATTERN = Pattern.compile("([a-zA-Z]+)(\\d+)");
	private static final int ENTRY_PATTERN_GROUP_NAME = 1;
	private static final int ENTRY_PATTERN_GROUP_TIME = 2;

	public static final Pattern MACHINE_PATTERN = Pattern.compile("[a-zA-Z]+");	

	public enum VectorClockComparison {
		SMALLER, GREATER, EQUAL, SIMULTANEOUS;
	}

	/**
	 * Increases the component of a unit by 1.
	 *
	 * @param unit The identifier of the vector element being increased
	 * @return Returns the new clock value for the given unit
	 */
	public Long incrementClock(String unit) {
		validateUnitName(unit);

		Long newValue = (containsKey(unit)) ? get(unit).longValue() + 1 : 1L;
		put(unit, newValue);

		return newValue;
	}

	private void validateUnitName(String unit) {
		if (!MACHINE_PATTERN.matcher(unit).matches()) {
			throw new RuntimeException("Machine name cannot be empty and must be only characters (A-Z).");
		}
	}

	/**
	 * Set the component of a unit.
	 *
	 * @param unit The identifier of the vector element being set
	 * @value value The new value of the unit being set
	 */
	public void setClock(String unit, long value) {
		validateUnitName(unit);
		put(unit, value);
	}

	/**
	 * Retrieve the unit's value
	 *
	 * @param unit The identifier of the vector element being retrieved
	 * @return Returns the value of the unit (if existent), or <tt>null</tt> if it does not exist
	 */
	public Long getClock(String unit) {
		return get(unit);
	}

	@Override
	public Long get(Object unit) { // TODO [low] This should not be used, or shoul it? Why inherit from TreeMap?
		Long lResult = super.get(unit);

		if (lResult == null) {
			lResult = 0L;
		}

		return lResult;
	}

	@Override
	public VectorClock clone() {
		return (VectorClock) super.clone();
	}

	@Override
	public String toString() {
		/*
		 * Please note that this is an incredibly important method.
		 * It is used in hundreds of tests! Don't mess with it!
		 */

		Object[] lIDs = keySet().toArray();
		Object[] lRequests = values().toArray();

		StringBuilder builder = new StringBuilder();
		builder.append('(');
		for (int i = 0; i < lRequests.length; i++) {
			builder.append(lIDs[i]);
			builder.append(lRequests[i].toString());

			if (i + 1 < lRequests.length) {
				builder.append(',');
			}
		}

		builder.append(')');

		return builder.toString();
	}
	
	/**
	 * Converts a serialized vector clock back into a {@link VectorClock} object.
	 * @see #toString()
	 */
	public static VectorClock parseVectorClock(String serializedVectorClock) {
		VectorClock vectorClock = new VectorClock();
		
		Matcher clockMatcher = CLOCK_PATTERN.matcher(serializedVectorClock);
		
		if (clockMatcher.matches()) {
			String clockContents = clockMatcher.group(CLOCK_PATTERN_GROUP_CONTENT);
			String[] clockEntries = clockContents.split(",");
			
			for (String clockEntry : clockEntries) {
				Matcher clockEntryMatcher = ENTRY_PATTERN.matcher(clockEntry);
				
				if (clockEntryMatcher.matches()) {
					String machineName = clockEntryMatcher.group(ENTRY_PATTERN_GROUP_NAME);
					Long clockValue = Long.parseLong(clockEntryMatcher.group(ENTRY_PATTERN_GROUP_TIME));
					
					vectorClock.put(machineName, clockValue);
				}
				else {
					throw new IllegalArgumentException("Not a valid vector clock, entry does not match pattern: " + clockEntry);
				}
			}
			
			return vectorClock;
		}
		else {
			throw new IllegalArgumentException("Not a valid vector clock: " + serializedVectorClock);
		}
	}

	/**
	 * VectorClock compare operation. Returns one of four possible values
	 * indicating how clock one relates to clock two:
	 *
	 * VectorComparison.GREATER If One > Two. VectorComparison.EQUAL If One =
	 * Two. VectorComparison.SMALLER If One < Two. VectorComparison.SIMULTANEOUS
	 * If One != Two.
	 *
	 * @param clock1 First Clock being compared.
	 * @param clock2 Second Clock being compared.
	 * @return VectorComparison value indicating how One relates to Two.
	 */
	public static VectorClockComparison compare(VectorClock clock1, VectorClock clock2) {
		// Initially we assume it is all possible things.
		boolean isEqual = true;
		boolean isGreater = true;
		boolean isSmaller = true;

		// Go over all elements in Clock one.
		for (String lEntry : clock1.keySet()) {
			// Compare if also present in clock two.
			if (clock2.containsKey(lEntry)) {
				// If there is a difference, it can never be equal.
				// Greater / smaller depends on the difference.
				if (clock1.get(lEntry) < clock2.get(lEntry)) {
					isEqual = false;
					isGreater = false;
				}
				if (clock1.get(lEntry) > clock2.get(lEntry)) {
					isEqual = false;
					isSmaller = false;
				}
			}
			// Else assume zero (default value is 0).
			else if (clock1.get(lEntry) != 0) {
				isEqual = false;
				isSmaller = false;
			}
		}

		// Go over all elements in Clock two.
		for (String lEntry : clock2.keySet()) {
			// Only elements we have not found in One still need to be checked.
			if (!clock1.containsKey(lEntry) && (clock2.get(lEntry) != 0)) {
				isEqual = false;
				isGreater = false;
			}
		}

		// Return based on determined information.
		if (isEqual) {
			return VectorClockComparison.EQUAL;
		}
		else if (isGreater && !isSmaller) {
			return VectorClockComparison.GREATER;
		}
		else if (isSmaller && !isGreater) {
			return VectorClockComparison.SMALLER;
		}
		else {
			return VectorClockComparison.SIMULTANEOUS;
		}
	}
}
