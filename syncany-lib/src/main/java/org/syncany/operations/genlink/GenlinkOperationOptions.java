package org.syncany.operations.genlink;

import org.syncany.operations.OperationOptions;

public class GenlinkOperationOptions implements OperationOptions {
		private boolean shortOutput = false;

		public boolean isShortOutput() {
			return shortOutput;
		}

		public void setShortOutput(boolean shortOutput) {
			this.shortOutput = shortOutput;
		}				
	}