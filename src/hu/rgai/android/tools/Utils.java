
package hu.rgai.android.tools;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.ocpsoft.prettytime.PrettyTime;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class Utils {

  private static final String randStrings = "abcdefghijlkmnopqrstuvwxyzABCDEFGHIJLKMNOPQRSTUVWXYZ0123456789_";
  
  public static String generateString(int length) {
    StringBuilder sb = new StringBuilder();
    int sl = randStrings.length();
    for (int i = 0; i < length; i++) {
      sb.append(randStrings.indexOf((int)(Math.random() * sl)));
    }
    
    
    return sb.toString();
  }
  
  public static String getPrettyTime(Date date) {
    PrettyTime pt = new PrettyTime();
    return pt.format(date);
  }
  
  public static String getSimplifiedTime(Date date) {
    Calendar now = Calendar.getInstance();
    Calendar comp = new GregorianCalendar();
    comp.setTime(date);
    
    int Y = Calendar.YEAR;
    int M = Calendar.MONTH;
    int D = Calendar.DAY_OF_MONTH;
    int H = Calendar.HOUR_OF_DAY;
    int I = Calendar.MINUTE;
    
    String s = "";
    String time = add0(comp.get(H))+":"+add0(comp.get(I));
    if (now.get(Y) == comp.get(Y)) {
      if (now.get(M) == comp.get(M)) {
        if (now.get(D) == comp.get(D)) {
          s = "Today, " + time;
        } else {
          s = new SimpleDateFormat("MMM dd").format(date) + ", " + time;
        }
      } else {
        s = new SimpleDateFormat("MMM dd").format(date) + ", " + time;
      }
    } else {
      s = new SimpleDateFormat("y MMM dd").format(date) + ", " + time;
    }
    
    return s;
  }
  
  private static String add0(int i) {
    if (i > 9) {
      return i+"";
    } else {
      return "0"+i;
    }
  }
  
}
