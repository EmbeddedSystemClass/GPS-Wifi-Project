package CSVPckg;

import javax.crypto.Mac;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Record extends ArrayList<Object> {


	/**
	 * 	<p><b>String: </b> MAC, SSID, AuthMode, Type
	 *	<br><b>Date: </b> FirstSeen
	 *	<br><b>Float: </b> Channel, RSSI, Lat, Lon, Alt, Acc</p>
	 */
	public enum Field {


		MAC(0), SSID(1), AuthMode(2), FirstSeen(3), Channel(4), RSSI(5), Lat(6), Lon(7), Alt(8), Acc(9), Type(10);

		public int column;

		Field(int column) {
			this.column = column;
		}

		public static boolean isFieldFloat(Field field) {
			if(field == RSSI || field == Lat || field == Lon || field == Channel || field == Alt || field == Acc) {
				return true;
			}
			return false;
		}

		public static boolean isFieldString(Field field) {
			if(field == MAC || field == AuthMode || field == SSID || field == Type) {
				return true;
			}
			return false;
		}


		/**
		 * Compares r2 to r1 (and not vice versa).
		 * @param record1
		 * @param record2
		 * @param field
		 * @return the value 0 if record1 is numerically equal to record2; a value less than 0 if record1 is numerically less than record2; and a value greater than 0 if record1 is numerically greater than record2.
		 */
		public static int compareFields(Record record1, Record record2, Field field) {
			Object o1 = record1.get_Field(field);
			Object o2 = record2.get_Field(field);

			if (Field.isFieldFloat(field)) {
				float one = ((Float) (o1)).floatValue();
				float two = ((Float) (o2)).floatValue();
				return Float.compare(two, one);
			}
			if (Field.isFieldString(field)) {
				String one = (String) o1;
				String two = (String) o2;
				return two.compareTo(one);
			} else {
				Date one = (Date) o1;
				Date two = (Date) o2;
				return two.compareTo(one);
			}
		}
	}

	/**
	 * A string which represents lineAsArrayOfStrings with comma seperating string
	 * objects.
	 */
	public String line;
	public String[] lineAsArrayOfStrings;

	/**
	 * 
	 * @param s
	 *            An array of strings representing the CSVPckg line. (Each element is 1
	 *            column)
	 * @throws ParseException
	 */
	public Record(String[] s) throws ParseException {
		super();
		lineAsArrayOfStrings = s;
		line = getFullString(s);
		// after record is created, create object array
		addToObjectArray();
	}

	/**
	 * Add the record String[] array to object[] array
	 * 
	 * @throws ParseException
	 */
	private void addToObjectArray() {
		// MAC(0) , SSID(1) , Auth(2), Type(10) is string
		// FirstSeen(3) is date
		// Channel(4) to Acc(9) are float
		// Type(10) is string

		// add mac, ssid, auth first(strings)
		add(lineAsArrayOfStrings[0]);
		add(lineAsArrayOfStrings[1]);
		add(lineAsArrayOfStrings[2]);

		// add first seen (date)
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy/MM/dd HH:MM");
		SimpleDateFormat formatter3 = new SimpleDateFormat("yyyy/MM/dd HH:MM");
		Date date;
		try {
			date = formatter3.parse(lineAsArrayOfStrings[3]);
			add(date);
		} catch (ParseException e) {
			try {
				date = formatter.parse(lineAsArrayOfStrings[3]);
				add(date);
			} catch (ParseException e1) {
				e1.printStackTrace();
				add("");
			}
		}
		

		// add 4 to 9 floats (not nececeraly float)
		add(Float.parseFloat(lineAsArrayOfStrings[4]));
		add(Float.parseFloat(lineAsArrayOfStrings[5]));
		add(Float.parseFloat(lineAsArrayOfStrings[6]));
		add(Float.parseFloat(lineAsArrayOfStrings[7]));
		add(Float.parseFloat(lineAsArrayOfStrings[8]));
		add(Float.parseFloat(lineAsArrayOfStrings[9]));

		// add type
		add(lineAsArrayOfStrings[10]);

		// check if any null
		// for(int i = 0; i <= 10; i++)
		// if(origionalLine[i] == null)
		// System.out.println("i = " + i);
	}

	/**
	 * 
	 * @return String with each object of s is seperated by comma. <br>
	 * 		Example: S=["Hello","World"] Then return: "Hello,World"
	 */
	public static String getFullString(String[] s) {
		if(s == null)
			return null;
		String result = "";
		for (int i = 0; i < s.length; i++) {
			result += s[i];
			result += ',';
		}
		return result;
	}

	/**
	 * 
	 * @param f
	 *            An enum parameter from Record.Field that represents the chosen
	 *            field
	 * @return The object of the field.
	 */
	public Object get_Field(Field f) {
		return get(f.column);
	}

	/**
	 * Print record.
	 */
	public void print() {
		System.out.println(line);
	}

	public static class GeoPoint {
		public Float latitude, longtitude;

		public GeoPoint(Float latitude, Float longtitude) {
			this.latitude = latitude;
			this.longtitude = longtitude;
		}

		public GeoPoint(float latitude, float longtitude) {
			this.latitude = latitude;
			this.longtitude = longtitude;
		}

		public GeoPoint(double latitude, double longtitude) {
			this.latitude = (float) latitude;
			this.longtitude = (float) longtitude;
		}

		@Override
		public String toString() {
			return "(" + latitude + "," + longtitude + ")";
		}
	}// GeoPoint class

}