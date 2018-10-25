package utils;
import org.apache.log4j.Logger;

import servlets.module.challenge.BrokenCrypto4;

public class SaveLogs
{
	private static final org.apache.log4j.Logger log = Logger.getLogger(BrokenCrypto4.class);
	public static void saveLog(String classe,Exception stackTrace)
	{
		// implementar gravacao no bigdata
	}
}
