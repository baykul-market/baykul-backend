package by.baykulbackend.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator {
    private static final String PHONE_NUMBER_REGEX = "^\\+[1-9]\\d{6,14}$";
    private static final Pattern phoneNumberPattern = Pattern.compile(PHONE_NUMBER_REGEX);

    /**
     * Validates a UUID string.
     *
     * @param uuidString String containing UUID
     * @return true if string is uuid, false otherwise
     */
    public static boolean isValidUUID(String uuidString) {
        if (StringUtils.isBlank(uuidString)) {
            return false;
        }

        try {
            UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    /**
     * Validates a phone number string.
     *
     * @param phoneNumber String containing phone number
     * @return true if string can be a phone number, false otherwise
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (StringUtils.isBlank(phoneNumber)) {
            return false;
        }

        Matcher matcher = phoneNumberPattern.matcher(phoneNumber);

        return matcher.matches();
    }
}
