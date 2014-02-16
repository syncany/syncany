/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

	public enum VectorClockComparison {
		SMALLER, GREATER, EQUAL, SIMULTANEOUS;
	}	
	
	/**
	 * Increases the component of a unit by 1.
	 * 
	 * @param unit The identifier of the vector element being increased
	 */
	public void incrementClock(String unit) {
		if (this.containsKey(unit)) {
			this.put(unit, this.get(unit).longValue() + 1);
		}
		else {
			this.put(unit, 1L);
		}
	}
	
	/**
	 * Set the component of a unit.
	 * 
	 * @param unit The identifier of the vector element being set
	 * @value value The new value of the unit being set
	 */
	public void setClock(String unit, long value) {
		this.put(unit, value);	
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

		if (lResult == null)
			lResult = 0L;

		return lResult;
	}

	@Override
	public VectorClock clone() {
		return (VectorClock) super.clone();
	}

	@Override
	public String toString() {
		Object[] lIDs = this.keySet().toArray();
		Object[] lRequests = this.values().toArray();

		String lText = "(";

		for (int i = 0; i < lRequests.length; i++) {
			lText += lIDs[i];
			//lText += "-";
			lText += lRequests[i].toString();

			if (i + 1 < lRequests.length) {
				lText += ",";
			}
		}

		lText += ")";

		return lText;
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
