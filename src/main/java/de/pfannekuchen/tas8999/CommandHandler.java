package de.pfannekuchen.tas8999;

import java.io.File;

import org.slf4j.Logger;

import de.pfannekuchen.tas8999.util.Storable;

public class CommandHandler extends Storable{

	public CommandHandler(String name, File directory, Logger logger) {
		super(name, directory, logger);
	}

}
