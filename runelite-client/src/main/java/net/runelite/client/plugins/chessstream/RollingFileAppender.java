package net.runelite.client.plugins.chessstream;

import static ch.qos.logback.core.CoreConstants.CODES_URL;
import static ch.qos.logback.core.CoreConstants.MORE_INFO_PREFIX;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.rolling.TriggeringPolicy;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.FileNamePattern;
import ch.qos.logback.core.util.ContextUtil;

/**
* <code>RollingFileAppender</code> extends {@link FileAppender} to backup the
* log files depending on {@link RollingPolicy} and {@link TriggeringPolicy}.
* <p/>
* <p/>
* For more information about this appender, please refer to the online manual
* at http://logback.qos.ch/manual/appenders.html#RollingFileAppender
*
* @author Heinz Richter
* @author Ceki G&uuml;lc&uuml;
*/
public class RollingFileAppender<E> extends ch.qos.logback.core.rolling.RollingFileAppender<E> {
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
