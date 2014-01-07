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

import java.security.InvalidParameterException;


public class PluginSetting {
	   public enum ValueType { STRING, INT, BOOLEAN};

	   private String name;
	   private ValueType type;
	   private boolean mandatory;
	   private boolean sensitive;
	   private String value;
	   private String defaultValue;
	   
	   /**
	    * Standard constructor 
	    * @param name of the setting
	    * @param type STRING/INT/BOOLEAN
	    * @param mandatory boolean
	    * @param sensitive boolean
	    */
	   public PluginSetting(String name, ValueType type, boolean mandatory, boolean sensitive) {
		   this.name = name;
		   this.type = type;
		   this.mandatory = mandatory;
		   this.sensitive = sensitive;
	   }
	   
	   /**
	    * Constructor for optional setting with a default value
	    */
	   public PluginSetting(String name, ValueType type, String defaultValue, boolean sensitive) {
		   this.name = name;
		   this.type = type;
		   this.mandatory = false;
		   this.sensitive = sensitive;
		   this.defaultValue = defaultValue;
	   }
	   
	   public boolean validate() { 
		   return (value != null);
	   }
	   
	   /**
	    * Setter for the value. Throws InvalidParameterException on invalid values.
	    */
	   public void setValue(String str) {
		   if (type == ValueType.STRING) {
			   value = str;
		   }
		   else if (type == ValueType.INT) {
			   try {
				   value = Integer.toString(Integer.parseInt(str));
			   }
			   catch (NumberFormatException e) {
				   throw new InvalidParameterException(str + " is not a valid integer.");
			   }
		   }
		   else if (type == ValueType.BOOLEAN) {
			   //This is false if and only if value.equals("false"). 
			   value = Boolean.toString(Boolean.parseBoolean(str));
		   }
	   }
	   
	   public String getValue() {
		   return value;
	   }
	   
	   public boolean isMandatory() {
		   return mandatory;
	   }
	   
	   public boolean isSensitive()	{
		   return sensitive;
	   }
	   
	   public String getDefaultValue() {
		   return defaultValue;
	   }
	   
	   public String getName() {
		   return name;
	   }
	   
	   public ValueType getType() {
		   return type;
	   }
	   
	   @Override
	   public String toString() {
		   return name;
	   }
}
