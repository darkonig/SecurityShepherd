package servlets.module.challenge;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.owasp.encoder.Encode;


import utils.ShepherdLogManager;
import utils.SqlFilter;
import utils.Validate;
import dbProcs.Database;

/**
 * SQL Injection Challenge Four - Does not use user specific key
 * <br/><br/>
 * This file is part of the Security Shepherd Project.
 * 
 * The Security Shepherd project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.<br/>
 * 
 * The Security Shepherd project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.<br/>
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Security Shepherd project.  If not, see <http://www.gnu.org/licenses/>. 
 * @author Mark Denihan
 *
 */
public class SqlInjection4 extends HttpServlet
{
	//Sql Challenge 4
	private static final long serialVersionUID = 1L;
	private static final org.apache.log4j.Logger log = Logger.getLogger(SqlInjection4.class);
	private static String levelName = "SqlInjection4";
	private static String levelResult = "d316e80045d50bdf8ed49d48f130b4acf4a878c82faef34daff8eb1b98763b6f"; 
	public static String levelHash = "1feccf2205b4c5ddf743630b46aece3784d61adc56498f7603ccd7cb8ae92629";
	/**
	 * Users have to defeat SQL injection that blocks single quotes.
	 * The input they enter is also been filtered.
	 * @param theUserName User name used in database look up.
	 * @param thePassword User password used in database look up
	 */
	public void doPost (HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException
	{
		//Setting IpAddress To Log and taking header for original IP if forwarded from proxy
		ShepherdLogManager.setRequestIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
		HttpSession ses = request.getSession(true);
		
		//Translation Stuff
		Locale locale = new Locale(Validate.validateLanguage(request.getSession()));
		ResourceBundle errors = ResourceBundle.getBundle("i18n.servlets.errors", locale);
		ResourceBundle bundle = ResourceBundle.getBundle("i18n.servlets.challenges.sqli.sqli4", locale);
		if(Validate.validateSession(ses))
		{
			ShepherdLogManager.setRequestIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"), ses.getAttribute("userName").toString());
			log.debug(levelName + " servlet accessed by: " + ses.getAttribute("userName").toString());
			PrintWriter out = response.getWriter();  
			out.print(getServletInfo());
			String htmlOutput = new String();
			
			Connection conn = null;
			PreparedStatement stmt = null;
			ResultSet resultSet = null;
			try
			{
				String theUserName = request.getParameter("theUserName");
				log.debug("User Submitted - " + theUserName);
				theUserName = SqlFilter.levelFour(theUserName);
				log.debug("Filtered to " + theUserName);
				String thePassword = request.getParameter("thePassword");
				thePassword = SqlFilter.levelFour(thePassword);
				String ApplicationRoot = getServletContext().getRealPath("");
				log.debug("Servlet root = " + ApplicationRoot );
				
				log.debug("Getting Connection to Database");
				conn = Database.getChallengeConnection(ApplicationRoot, "SqlChallengeFour");
				
				String query = "SELECT userName FROM users WHERE userName = ? AND userPassword = ?";
				stmt = conn.prepareStatement(query);
				stmt.setString(1, theUserName);
				stmt.setString(2, thePassword);
				log.debug("Gathering result set");
				resultSet = stmt.executeQuery();
		
				int i = 0;
				htmlOutput = "<h2 class='title'>" + bundle.getString("response.loginResults")+ "</h2>";
				
				log.debug("Opening Result Set from query");
				if(resultSet.next())
				{
					log.debug("Signed in as " + resultSet.getString(1));
					htmlOutput += "<p>" + bundle.getString("response.signedInAs")+ "" + Encode.forHtml(resultSet.getString(1)) + "</p>";
					if(resultSet.getString(1).equalsIgnoreCase("admin"))
					{
						htmlOutput += "<p>" + bundle.getString("response.adminResultKey")+ ""
									+ "<a>"	+ Encode.forHtml(levelResult) + "</a>";
					}
					else
					{
						htmlOutput += "<p>" + bundle.getString("response.adminsFun")+ "</p>";
					}
					i++;
				}
				if(i == 0)
				{
					htmlOutput = "<h2 class='title'>" + bundle.getString("response.loginResults")+ "</h2><p>" + bundle.getString("response.superSecure")+ "</p>";
				}
			}
			catch (SQLException e)
			{
				log.debug("SQL Error caught - " + e.toString());
				htmlOutput += "<p>"+errors.getString("error.detected")+"</p>" +
					"<p>" + Encode.forHtml(e.toString()) + "</p>";
			}
			catch(Exception e)
			{
				out.write(errors.getString("error.funky"));
				log.fatal(levelName + " - " + e.toString());
			}finally {
				try {
					if(resultSet != null) {
						resultSet.close();
					}
					
					if(conn != null) {
						conn.close();
					}
					if(stmt != null) {
						stmt.close();
					}
				} catch (Exception e) {
					log.error("Error close connections", e);
				}
			}
			log.debug("Outputting HTML");
			out.write(htmlOutput);
		}
		else
		{
			log.error(levelName + " servlet accessed with no session");
		}
	}
}
