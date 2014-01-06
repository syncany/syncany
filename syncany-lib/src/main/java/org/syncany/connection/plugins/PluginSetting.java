/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.connection.plugins;


public class PluginSetting {
	   public enum ValueType { STRING, INT, BOOLEAN};

	   public String name;
	   public ValueType type;
	   public boolean mandatory;
	   public boolean sensitive;
	   private String value;
	   
	   public PluginSetting(String name, ValueType type, boolean mandatory, boolean sensitive) {
		   this.name = name;
		   this.type = type;
		   this.mandatory = mandatory;
		   this.sensitive = sensitive;
	   }
	   
	   public boolean validate() { 
		   return (value != null);
	   }
	   
	   public void setValue(String str) {
		   if (type == ValueType.STRING) {
			   value = str;
		   }
		   else if (type == ValueType.INT) {
			   try {
				   value = Integer.toString(Integer.parseInt(str));
			   }
			   catch (NumberFormatException e) {
				   value = null;
			   }
		   }
		   else if (type == ValueType.BOOLEAN) {
			   value = Boolean.toString(Boolean.parseBoolean(str));
		   }
	   }
	   
	   public String getString() {
		   return value;
	   }
	   
	   @Override
	   public String toString() {
		   return name;
	   }
}
