package net.runelite.client.plugins.chess;

/**
* <code>TimeBasedRollingPolicy</code> is both easy to configure and quite
* powerful. It allows the roll over to be made based on time. It is possible to
* specify that the roll over occur once per day, per week or per month.
* 
* <p>For more information, please refer to the online manual at
* http://logback.qos.ch/manual/appenders.html#TimeBasedRollingPolicy
* 
* @author Ceki G&uuml;lc&uuml;
*/
public class TimeBasedRollingPolicy<E> extends ch.qos.logback.core.rolling.TimeBasedRollingPolicy<E> {

   public void start() {
       super.start();
   }
   
   public void addInfo(String msg) {
   }

   public void addInfo(String msg, Throwable ex) {
   }

   public void addWarn(String msg) {
   }

   public void addWarn(String msg, Throwable ex) {
   }

   public void addError(String msg) {
   }

   public void addError(String msg, Throwable ex) {
   }
}

