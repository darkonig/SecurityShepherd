package servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import dbProcs.Database;
import utils.InstallationException;
import utils.SaveLogs;
import utils.Validate;

public class Setup extends HttpServlet {
	private static final org.apache.log4j.Logger log = Logger.getLogger(Setup.class);
	private static final long serialVersionUID = -892181347446991016L;

	public void doPost (HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException {
		//Translation Stuff
		Locale locale = new Locale(Validate.validateLanguage(request.getSession()));
		ResourceBundle errors = ResourceBundle.getBundle("i18n.servlets.errors", locale);
		ResourceBundle bundle = ResourceBundle.getBundle("i18n.text", locale);
		
		//Output Stuff
		PrintWriter out = response.getWriter();
		String htmlOutput = new String();
		boolean success = false;
		try 
		{
			//Parameters From Form
			String dbHost = request.getParameter("dbhost");
			String dbPort = request.getParameter("dbport");
			String dbUser = request.getParameter("dbuser");
			String dbPass = request.getParameter("dbpass");
			String dbAuth = request.getParameter("dbauth");
			String dbOverride = request.getParameter("dboverride");

			Path path = utils.FileUtils.getSetupPath();
			
			String auth = new String(Files.readAllBytes(path));

			StringBuffer dbProp = new StringBuffer();
			dbProp.append("databaseConnectionURL=jdbc:mysql://" + dbHost + ":" + dbPort + "/");
			dbProp.append("\n");
			dbProp.append("DriverType=org.gjt.mm.mysql.Driver");
			dbProp.append("\n");
			dbProp.append("databaseSchema=core");
			dbProp.append("\n");
			dbProp.append("bancoDeDadosNomeUsuario=" + dbUser);
			dbProp.append("\n");
			dbProp.append("bancoDeDadosSenha=" + dbPass);
			dbProp.append("\n");

			if (auth != null && !auth.equals(dbAuth)) {
				htmlOutput = bundle.getString("generic.text.setup.authentication.failed");
			} else {
				Path dbpath = utils.FileUtils.getDbProp();
				
				Files.write(dbpath, dbProp.toString().getBytes(), StandardOpenOption.CREATE);	
				if (Database.getDatabaseConnection(null) == null) {
					htmlOutput = bundle.getString("generic.text.setup.connection.failed");
				} else {
					try {
						if (dbOverride.equalsIgnoreCase("overide")) {
							executeSqlScript();
							htmlOutput = bundle.getString("generic.text.setup.success") + " " + bundle.getString("generic.text.setup.success.overwrittendb");
						}
						else if (dbOverride.equalsIgnoreCase("upgrade")) {
							executeUpdateScript();
							htmlOutput = bundle.getString("generic.text.setup.success") + " " + bundle.getString("generic.text.setup.success.updatedb");
						}else {
							htmlOutput = bundle.getString("generic.text.setup.success");
						}
						success = true;
					} catch (InstallationException e) {
						htmlOutput = bundle.getString("generic.text.setup.failed") + ": Install error";
						Files.delete(utils.FileUtils.getDbProp());
					}
					//Clean up File as it is not needed anymore. Will Cause a new one to be generated next time too
					removeAuthFile();
				}

			}
			if(success) {
				htmlOutput = "<h2 class=\"title\" id=\"login_title\">"+bundle.getString("generic.text.setup.response.success")+"</h2><p>"+htmlOutput+" "+bundle.getString("generic.text.setup.response.success.redirecting")+"</p>";
			} else {
				htmlOutput = "<h2 class=\"title\" id=\"login_title\">"+bundle.getString("generic.text.setup.response.failed")+"</h2><p>"+htmlOutput+"</p>";
			}
			out.write(htmlOutput);
		}
		catch (Exception e)
		{
			out.write(errors.getString("error.funky"));
			SaveLogs.saveLog("Error", e);
		}
		out.close();
	}

	public static boolean isInstalled() {
		boolean isInstalled;
		Connection coreConnection = Database.getDatabaseConnection(null);
		if (coreConnection == null) {
			isInstalled = false;
			generateAuth();
		} else {
			isInstalled = true;
		}

		return isInstalled;
	}

	private static void generateAuth() {
		try {
			if (!Files.exists(utils.FileUtils.getSetupPath(), LinkOption.NOFOLLOW_LINKS)) {
				UUID randomUUID = UUID.randomUUID();
				Files.write(utils.FileUtils.getSetupPath(), randomUUID.toString().getBytes(), StandardOpenOption.CREATE);
			}
		} catch (IOException e) {
			log.fatal("Unable to generate auth");
			SaveLogs.saveLog("Error", e);
		}
	}
	
	private static void removeAuthFile() throws IllegalAccessException {
		Path path = utils.FileUtils.getSetupPath();
		if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			log.info("Could not find SETUP AUTH");
		} else {
			try {
				Files.delete(utils.FileUtils.getSetupPath());
			} catch (IOException e) { log.error(e); }
		}
	}

	private synchronized void executeSqlScript() throws InstallationException {

		try {
			File file = new File(getClass().getClassLoader().getResource("/database/coreSchema.sql").getFile());
			String data = FileUtils.readFileToString(file, Charset.defaultCharset() );
			
			Connection databaseConnection = Database.getDatabaseConnection(null, true);
			Statement psProcToexecute = databaseConnection.createStatement();
			psProcToexecute.executeUpdate(data);
			
			file = new File(getClass().getClassLoader().getResource("/database/moduleSchemas.sql").getFile());
			data = FileUtils.readFileToString(file, Charset.defaultCharset() );
			psProcToexecute = databaseConnection.createStatement();
			psProcToexecute.executeUpdate(data);

		} catch (Exception e) {
			SaveLogs.saveLog("Error", e);
			throw new InstallationException(e);
		}
	}

	private synchronized void executeUpdateScript() throws InstallationException {

		try {
			File file = new File(getClass().getClassLoader().getResource("/database/updatev3_0tov3_1.sql").getFile());
			String data = FileUtils.readFileToString(file, Charset.defaultCharset() );

			Connection databaseConnection = Database.getDatabaseConnection(null, true);
			Statement psProcToexecute = databaseConnection.createStatement();
			psProcToexecute.executeUpdate(data);

		} catch (Exception e) {
			SaveLogs.saveLog("Error", e);
			throw new InstallationException(e);
		}
	}
}
