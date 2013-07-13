package org.syncany.database;

import java.util.TreeMap;

/**
 * Implements a VectorClock that records the time stamps of all send and receive
 * events. It contains functions to compare and merge two VectorClocks.
 * 
 * @author Frits de Nijs
 * @author Peter Dijkshoorn
 * @author Philipp C. Heckel
 */
public class VectorClock extends TreeMap<String, Long> {
	private static final long serialVersionUID = 109876543L;

	public enum VectorClockComparison {
		SMALLER, GREATER, EQUAL, SIMULTANEOUS;
	}
	
	/**
	 * Increases the component of pUnit by 1.
	 * 
	 * @param unit
	 *            - The ID of the vector element being increased.
	 */
	public void incrementClock(String unit) {
		// If we have it in the vector, increment.
		if (this.containsKey(unit)) {
			this.put(unit, this.get(unit).longValue() + 1);
		}
		// Else, store with value 1 (starts at 0, +1).
		else {
			this.put(unit, 1L);
		}
	}
	
	/**
	 * Set the component of the pUnit.
	 * 
	 * @param unit
	 *            - The ID of the vector element being increased.
	 */
	public void setClock(String unit, long value) {
		this.put(unit, value);	
	}
		
	public Long getClock(String unit) {
		return get(unit);
	}

	@Override
	public Long get(Object unit) {
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
	 * VectorClock merging operation. Creates a new VectorClock with the maximum
	 * for each element in either clock. Used in Buffer and Process to
	 * manipulate clocks.
	 * 
	 * @param clock1
	 *            - First Clock being merged.
	 * @param clock2
	 *            - Second Clock being merged.
	 * 
	 * @return A new VectorClock with the maximum for each element in either
	 *         clock.
	 */
	public static VectorClock max(VectorClock clock1,	VectorClock clock2) {
		// Create new Clock.
		VectorClock lResult = new VectorClock();

		// Go over all elements in clock One, put them in the new clock.
		for (String lEntry : clock1.keySet()) {
			lResult.put(lEntry, clock1.get(lEntry));
		}

		// Go over all elements in clock Two,
		for (String lEntry : clock2.keySet()) {
			// Insert the Clock Two value if it is not present in One, or if it
			// is higher.
			if (!lResult.containsKey(lEntry)
					|| lResult.get(lEntry) < clock2.get(lEntry)) {
				lResult.put(lEntry, clock2.get(lEntry));
			}
		}

		// Return the merged clock.
		return lResult;
	}

	/**
	 * VectorClock compare operation. Returns one of four possible values
	 * indicating how clock one relates to clock two:
	 * 
	 * VectorComparison.GREATER If One > Two. VectorComparison.EQUAL If One =
	 * Two. VectorComparison.SMALLER If One < Two. VectorComparison.SIMULTANEOUS
	 * If One <> Two.
	 * 
	 * @param clock1
	 *            - First Clock being compared.
	 * @param clock2
	 *            - Second Clock being compared.
	 * 
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
