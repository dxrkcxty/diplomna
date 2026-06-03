package util;

import dto.*;

public class ValidationUtilTest {
    public static void main(String[] args) {
        try {
            ValidationUtil.validate(new ProductDTO(0, "New", -5.0, 1));
            System.err.println("FAIL: Product negative price");
        } catch (Exception ignored) {
        }

        try {
            ValidationUtil.validate(new UserDTO(0, "bademail", "pw", "USER", "", "", "", 0.0, null, 0));
            System.err.println("FAIL: Bad email");
        } catch (Exception ignored) {
        }

        try {
            ValidationUtil.validate(new CategoryDTO(1, ""));
            System.err.println("FAIL: Empty category");
        } catch (Exception ignored) {
        }

        try {
            ValidationUtil.validate(new ReviewDTO(1, 1, 1, 6, "ok"));
            System.err.println("FAIL: Bad rating");
        } catch (Exception ignored) {
        }
    }
}
