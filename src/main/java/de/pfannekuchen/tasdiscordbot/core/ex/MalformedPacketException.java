package de.pfannekuchen.tasdiscordbot.core.ex;

import java.io.IOException;

public class MalformedPacketException extends IOException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5408076985296964807L;

	public MalformedPacketException(String message) {
		super(message);
	}
	
}
