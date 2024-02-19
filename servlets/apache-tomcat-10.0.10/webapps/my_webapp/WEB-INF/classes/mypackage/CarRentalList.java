package mypackage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Iterator;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class CarRentalList extends HttpServlet {

  int cont = 0;

  public void doGet(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
    res.setContentType("text/html");
    PrintWriter out = res.getWriter();
    String nombre = req.getParameter("userid");
    String password = req.getParameter("password");
    if (nombre.equals("admin") &&  password.equals("admin")) {

      out.println("<html><big><b>Rental List:</b></big><br></html>");

      JSONParser parser = new JSONParser();

      try (Reader reader = new FileReader("/home/alumne/pti/servlets/apache-tomcat-10.0.10/webapps/my_webapp/WEB-INF/classes/mypackage/rentals.json")) {
        JSONObject jsonObject = (JSONObject) parser.parse(reader);
        JSONArray rentals = (JSONArray) jsonObject.get("rentals");

        Iterator<JSONObject> iterator = rentals.iterator();
        while (iterator.hasNext()) {
          JSONObject obj = (JSONObject) iterator.next();
          out.println("<html><big><br>CO2 Rating: "+ obj.get("rating") + "<br>Engine: "+ obj.get("engine")+ "<br>Number of days: "+ obj.get("days") + "<br>Number of units: "+ obj.get("units") +"<br>Discount: "+ obj.get("disc")+"</big></html>");
          
        }

      } catch (IOException e) {
          e.printStackTrace();
      } catch (ParseException e) {
          e.printStackTrace();
      }
    }
    else {
    	cont ++;
    	out.println("<html>Usuario i/o contranseña incorrecto</html>");
    }
  }

  public void doPost(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
    doGet(req, res);
  }
}
