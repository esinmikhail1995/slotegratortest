package com.slotegrator.qa.data;

import com.slotegrator.qa.config.TestConfig;
import com.slotegrator.qa.dto.PlayerRequestDto;
import net.datafaker.Faker;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Generates player payloads that are unique per run, so repeated runs never collide on username/email
 * and a leftover record from an aborted run cannot make a fresh run pass or fail by accident.
 */
public final class PlayerFactory {

    private static final Faker FAKER = new Faker(Locale.ENGLISH);
    private static final String EMAIL_DOMAIN = "qa-automation.test";

    private PlayerFactory() {
    }

    public static PlayerRequestDto player() {
        String unique = uniqueSuffix();
        String name = sanitize(FAKER.name().firstName());
        String surname = sanitize(FAKER.name().lastName());
        String password = TestConfig.playerPassword();
        return new PlayerRequestDto(
                TestConfig.playerCurrencyCode(),
                (name + "." + surname + "." + unique + "@" + EMAIL_DOMAIN).toLowerCase(Locale.ROOT),
                name,
                surname,
                (name + surname + unique).toLowerCase(Locale.ROOT),
                password,
                password);
    }

    public static List<PlayerRequestDto> players(int count) {
        return IntStream.range(0, count).mapToObj(index -> player()).toList();
    }

    private static String uniqueSuffix() {
        // Millis keeps runs apart, the random tail keeps players inside one run apart.
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString(ThreadLocalRandom.current().nextInt(0x1000, 0x10000), 36);
    }

    /** Faker names can contain apostrophes or spaces (O'Connor, Van Dyke); usernames and emails cannot. */
    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z]", "");
    }
}
