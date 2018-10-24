package utils;

import java.nio.file.Path;
import java.nio.file.Paths;

import dbProcs.Constants;

public class FileUtils {
	
	public static boolean validateFileAccess(String filename) {
		return filename != null && filename.startsWith(Constants.CATALINA_CONF);
	}
		
	public static Path getSetupPath() {
		return Paths.get(Constants.SETUP_AUTH).normalize();
	}
	
	public static Path getDbProp() {
		return Paths.get(Constants.DBPROP).normalize();
	}
	
}
