package mypackage;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class CarRentalNew extends HttpServlet {

  int cont = 0;

  public void doGet(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
    res.setContentType("text/html");
    PrintWriter out = res.getWriter();
    String nombre = req.getParameter("name");
    cont ++;
    
    String rating = req.getParameter("co2_rating");
    String engine = req.getParameter("sub_model_vehicle");
    String days = req.getParameter("dies_lloguer");
    String disc = req.getParameter("descompte");
    String units = req.getParameter("num_vehicles");
    
    
    switch (rating) {
    	case "54":
    		rating = "Extralow";
    		break; 
	    case "71":
	    	rating = "Low";
	    	break;
	    case "82":
	    	rating = "Medium";
	    	break;
	    case "139":
	    	rating = "High";
	    	break;
	    default:
	    	//error
	  }


    JSONObject obj = new JSONObject();
    obj.put("engine", engine);
    obj.put("rating", rating);
    obj.put("days", days);
    obj.put("disc", disc);
    obj.put("units", units);
    
    /*JSONParser parser = new JSONParser();
    
    try (Reader reader = new FileReader("/home/alumne/pti/servlets/apache-tomcat-10.0.10/webapps/my_webapp/WEB-INF/classes/mypackage/rentals.json")) {
		JSONObject jsonObject = (JSONObject) parser.parse(reader);
		//JSONArray jsonArray = jsonObject.toJSONArray();
		//jsonArray.add(obj);
		//obj = (JSONObject) jsonArray;
		jsonObject.add(obj);
		obj = jsonObject;
    }*/
    
    File file = new File("/home/alumne/pti/servlets/apache-tomcat-10.0.10/webapps/my_webapp/WEB-INF/classes/mypackage/rentals.json");

    JSONObject jsonObject = new JSONObject();
    JSONArray rentals = new JSONArray();

    rentals.add(obj);

    try (Reader reader = new FileReader(file)) {
   		JSONParser parser = new JSONParser();
    	jsonObject = (JSONObject) parser.parse(reader);
        rentals = (JSONArray) jsonObject.get("rentals");
        rentals.add(obj);

    } catch (IOException e) {
        e.printStackTrace();
    } catch (ParseException e) {
        e.printStackTrace();
    }


    try (FileWriter filew = new FileWriter("/home/alumne/pti/servlets/apache-tomcat-10.0.10/webapps/my_webapp/WEB-INF/classes/mypackage/rentals.json")) {
            JSONObject object = new JSONObject();
            object.put("rentals", rentals);
            filew.write(object.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    System.out.print(obj);
    
    out.println("<html><big>Rating: "+ rating + "<br>Engine: "+ engine+ "<br>Number of days: "+ days + "<br>Number of units: "+ units +"<br>Discount: "+disc+"</big></html>");
    
  }

  public void doPost(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
    doGet(req, res);
  }
}
