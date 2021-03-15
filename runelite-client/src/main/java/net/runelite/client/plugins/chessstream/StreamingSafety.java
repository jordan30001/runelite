package net.runelite.client.plugins.chessstream;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class StreamingSafety extends PatternLayout {

	private Pattern multilinePattern;
	private List<String> maskPatterns = new ArrayList<>();

	public void addMaskPattern(String maskPattern) { // invoked for every single entry in the xml
		maskPatterns.add(maskPattern);
		multilinePattern = Pattern.compile(String.join("|", maskPatterns), // build pattern using logical OR
				Pattern.MULTILINE);
	}

	@Override
	public String doLayout(ILoggingEvent event) {
		return maskMessage(super.doLayout(event)); // calling superclass method is required
	}

	private String maskMessage(String message) {
		if (multilinePattern == null) {
			return message;
		}
		StringBuilder sb = new StringBuilder(message);
		Matcher matcher = multilinePattern.matcher(sb);
		while (matcher.find()) {
			if (matcher.group().contains("password") || matcher.group().contains("username")) {
				stripCredentials(sb, matcher);
			} else if (matcher.group().contains("path")) {
				stripPath(sb, matcher);
			}
		}
		return sb.toString();
	}

	private void stripPath(StringBuilder sb, Matcher matcher) {
		String targetExpression = matcher.group();
		String[] split = targetExpression.split("=");
		String pan = "3head";//split[1].chars().mapToObj(c -> "*").collect(Collectors.joining());
		int start = matcher.start() + split[0].length() + 1;
		int end = matcher.end();
		sb.replace(start, end, pan);
	}

	private void stripCredentials(StringBuilder sb, Matcher matcher) {
		String targetExpression = matcher.group();
		String[] split = targetExpression.split("=");
		String pan = "3head";//split[1].chars().mapToObj(c -> "*").collect(Collectors.joining());
		int start = matcher.start() + split[0].length() + 1;
		int end = matcher.end();
		sb.replace(start, end, pan);
	}
}