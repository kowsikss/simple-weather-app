package MyPackage;

import jakarta.servlet.ServletException;
import java.util.Date;
import java.text.SimpleDateFormat;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/MyServlet")
public class MyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public MyServlet() {
		super();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// OpenWeatherMap API setup
		String myApiKey = "f398e8e614936a3e3aa0ec208249b381"; // Replace with your actual API key
		String city = request.getParameter("city");

		if (city == null || city.trim().isEmpty()) {
			request.setAttribute("error", "City name is required!");
			request.getRequestDispatcher("index.jsp").forward(request, response);
			return;
		}

		String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + myApiKey;

		try {
			// API Integration
			System.out.println("Connecting to: " + apiUrl);
			URL url = new URL(apiUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000); // 5 seconds timeout
			connection.setReadTimeout(5000);

			int status = connection.getResponseCode();
			InputStream inputStream;

			if (status == 200) {
				inputStream = connection.getInputStream();
			} else {
				inputStream = connection.getErrorStream();
				Scanner errScanner = new Scanner(inputStream).useDelimiter("\\A");
				String errorMessage = errScanner.hasNext() ? errScanner.next() : "Unknown error";
				errScanner.close();

				System.err.println("API Error Response: " + errorMessage);
				request.setAttribute("error", "Failed to fetch weather data. API responded with error: " + errorMessage);
				request.getRequestDispatcher("index.jsp").forward(request, response);
				return;
			}

			InputStreamReader reader = new InputStreamReader(inputStream);
			StringBuilder responseContent = new StringBuilder();

			Scanner scanner = new Scanner(reader);
			while (scanner.hasNext()) {
				responseContent.append(scanner.nextLine());
			}
			scanner.close();

			// Parse JSON response
			Gson gson = new Gson();
			JsonObject jsonObject = gson.fromJson(responseContent.toString(), JsonObject.class);
			System.out.println("API Response JSON: " + jsonObject);

			// Extract data
			double tempInKelvin = jsonObject.getAsJsonObject("main").get("temp").getAsDouble();
			int tempInCelsius = (int) (tempInKelvin - 273.15);

			int humidity = jsonObject.getAsJsonObject("main").get("humidity").getAsInt();
			double windSpeed = jsonObject.getAsJsonObject("wind").get("speed").getAsDouble();

			int visibilityInMeter = jsonObject.get("visibility").getAsInt();
			int visibility = visibilityInMeter / 1000;

			String weatherCondition = jsonObject.getAsJsonArray("weather").get(0).getAsJsonObject().get("main").getAsString();
			int cloudCover = jsonObject.getAsJsonObject("clouds").get("all").getAsInt();

			// Date & Time formatting
			long dateTimestamp = jsonObject.get("dt").getAsLong() * 1000;
			SimpleDateFormat sdfDate = new SimpleDateFormat("EEE MMM dd yyyy");
			String date = sdfDate.format(new Date(dateTimestamp));

			SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
			String formattedTime = sdfTime.format(new Date());

			// Set request attributes for JSP
			request.setAttribute("date", date);
			request.setAttribute("city", city);
			request.setAttribute("visibility", visibility);
			request.setAttribute("temperature", tempInCelsius);
			request.setAttribute("weatherCondition", weatherCondition);
			request.setAttribute("humidity", humidity);
			request.setAttribute("windSpeed", windSpeed);
			request.setAttribute("cloudCover", cloudCover);
			request.setAttribute("currentTime", formattedTime);
			request.setAttribute("weatherData", responseContent.toString());

			connection.disconnect();

		} catch (IOException e) {
			e.printStackTrace();
			request.setAttribute("error", "Unable to connect to weather service. Please check your internet connection or try again later.");
		}

		// Forward to JSP
		request.getRequestDispatcher("index.jsp").forward(request, response);
	}
}
