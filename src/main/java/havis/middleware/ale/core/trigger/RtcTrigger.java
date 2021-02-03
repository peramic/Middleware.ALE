package havis.middleware.ale.core.trigger;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.config.Config;

import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to provide the rtc trigger as specified in ALE 1.1.1
 * (8.2.4.1). Use a uri in form of
 * 'urn:epcglobal:ale:trigger:rtc:period.offset.timezone' where period is a
 * number in range 1 to 86400000 and offset is greater or equal to zero and less
 * then period. The timezone value is optional an could be in form of Z, +hh:mm
 * or -hh:mm.
 */
public class RtcTrigger extends Trigger {

	/**
	 * Retrieves the regular expression for rtc trigger urn
	 */
	private static Pattern RTC;
	static {
		reset();
	}

	public static void reset() {
		RTC = Pattern.compile(Config.getInstance().getGlobal().getUrn().getTrigger().getRtc());
	}

	protected long period;
	protected long offset;

	/**
	 * Creates a new instance. Validates uri, period, offset and timezone.
	 * 
	 * @param uri
	 *            The trigger uri i.e urn:epcglobal:ale:trigger:rtc:3600000.0
	 * @param callback
	 *            he callback to invoke
	 * @throws ValidationException
	 *             If uri is invalid, period not in range 1 to 86400000, offset
	 *             not less then period or timezone value is invalid.
	 */
	RtcTrigger(String creatorId, String uri, Trigger.Callback callback) throws ValidationException {
		super(creatorId, uri, callback);
		Matcher match = RTC.matcher(uri);
		if (match.matches()) {
			try {
				period = Integer.parseInt(match.group("period"));
			} catch (NumberFormatException e) {
				throw new ValidationException("Could not parse period of rtc trigger");
			}
			if ((period > 0) && (period <= DAY)) {
				try {
					offset = Integer.parseInt(match.group("offset"));
				} catch (NumberFormatException e) {
					throw new ValidationException("Could not parse offset of rtc trigger");
				}
				if ((offset >= 0) && (offset < period)) {
					offset -= getOffsetFromUtc(match.group("timezone"));
				} else {
					throw new ValidationException("Offset of rtc trigger less then zero or not less then period");
				}
			} else {
				throw new ValidationException("Period of rtc trigger not greater then zero or greater then seconds of a day");
			}
			RtcTriggerService.getInstance().add(this);
		} else {
			throw new ValidationException("No period or offset given in rtc trigger");
		}
	}

	protected long getOffsetFromUtc(String timezone) {
		if (timezone != null && timezone.length() > 0) {
			if (timezone.charAt(0) == 'Z') {
				return 0;
			} else {
				return TimeZone.getTimeZone("GMT" + timezone).getRawOffset();
			}
		} else {
			return TimeZone.getDefault().getOffset(System.currentTimeMillis());
		}
	}

	long getNext(long passedMsOfDay) {
		if (passedMsOfDay + offset + period > DAY) {
			return DAY + passedMsOfDay + offset;
		} else {
			return period - (passedMsOfDay - offset) % period;
		}
	}

	@Override
	public void dispose() {
		RtcTriggerService.getInstance().remove(this);
	}
}