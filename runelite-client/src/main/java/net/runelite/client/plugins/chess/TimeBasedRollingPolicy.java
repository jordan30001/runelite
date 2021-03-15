package net.runelite.client.plugins.chess;

import static ch.qos.logback.core.CoreConstants.UNBOUND_HISTORY;
import static ch.qos.logback.core.CoreConstants.UNBOUNDED_TOTAL_SIZE_CAP;

import java.io.File;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.helper.ArchiveRemover;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.Compressor;
import ch.qos.logback.core.rolling.helper.FileFilterUtil;
import ch.qos.logback.core.rolling.helper.FileNamePattern;
import ch.qos.logback.core.rolling.helper.RenameUtil;
import ch.qos.logback.core.util.FileSize;

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

