package org.bitrepository.common;

import java.util.StringTokenizer;

import org.apache.commons.lang.WordUtils;

/**
 * Contains a general representation of a Bitrepository module. 
 * 
 */
public class ModuleCharacteristics {
	private String upperCamelCaseName;
	private String lowerCamelCaseName;
	private String lowerCaseNameWithHyphen;
	private String lowerCaseName;
	
	/**
	 * Creates an immutable <code>ModuleCharacteristics</code> instance.
	 * @param lowerCaseNameWithHyphen The protocol name in lowerCaseNameWithHyphen notation
	 */
	public ModuleCharacteristics(String lowerCaseNameWithHyphen) {
		if (containsUpperCaseChar(lowerCaseNameWithHyphen)) 
			throw new IllegalArgumentException("No upper case characters allowed in constructor");
		this.lowerCaseNameWithHyphen = lowerCaseNameWithHyphen;
		constructCamelCaseStrings();
		lowerCaseName = lowerCamelCaseName.toLowerCase();
	}

	/**
	 * Return the Module name in upper camel casing, see http://en.wikipedia.org/wiki/CamelCase for details
	 * 
	 * Example: 'AlarmClient'
	 */
	public String getUpperCamelCaseName() {
		return upperCamelCaseName;
	}

	/**
	 * Return the Module name in lower camel casing, see http://en.wikipedia.org/wiki/CamelCase for details.
	 * 
	 * Example: 'alarmClient'
	 */
    public String getLowerCamelCaseName() {
    	return lowerCamelCaseName;
	}

    /**
	 * Return the Module name in all lower case.
	 * 
	 * Example: 'alarmclient'
	 */
    public String getLowerCaseName() {
    	return lowerCaseName;
	}
    
	/**
	 * Return the Module name in all lower case, with words separated by the '-' char.
	 * 
	 * Example: 'alarm-client'
	 */
    public String getLowerCaseNameWithHyphen() {
    	return lowerCaseNameWithHyphen;
	}
    
    /**
     * Checks the supplied string for upper case characters.
     * @return <code>true</code> if the supplied string contains any upper case characters, else <code>false</code>.
     */
    private boolean containsUpperCaseChar(String stringToCheck) {
    	return (!stringToCheck.toLowerCase().equals(stringToCheck));
    }
    
    private void constructCamelCaseStrings() {
    	StringBuffer upperCamelCaseSB = new StringBuffer();
    	StringBuffer lowerCamelCaseSB = new StringBuffer();
    	StringTokenizer st = new StringTokenizer(lowerCaseNameWithHyphen, "-");
    	boolean firstWord = true;
    	while (st.hasMoreElements()) {
    		String word = st.nextToken();
    		upperCamelCaseSB.append(WordUtils.capitalize(word));
    		if (firstWord) {
    			lowerCamelCaseSB.append(word);
    			firstWord = false;
    		} else {
    			lowerCamelCaseSB.append(WordUtils.capitalize(word));
    		}
    	}
    	upperCamelCaseName = upperCamelCaseSB.toString();
    	lowerCamelCaseName = lowerCamelCaseSB.toString();
    }
}