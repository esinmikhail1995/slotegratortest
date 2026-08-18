package com.slotegrator.qa.report;

import io.qameta.allure.Allure;

import java.util.function.Supplier;

/**
 * Records a named step in the Allure report around a call.
 *
 * <p>Allure's {@code @Step} annotation would be tidier, but it needs AspectJ load-time weaving, which
 * AspectJ cannot do on JDK 24+ ("the aspect weaver cannot determine any valid method to define auxiliary
 * classes"). The programmatic API needs no javaagent and behaves identically on every JDK, so the framework
 * has no agent to keep working.
 */
public final class Steps {

    private Steps() {
    }

    /** Runs {@code call} inside a report step named {@code name}, and returns its result. */
    public static <T> T of(String name, Supplier<T> call) {
        return Allure.step(name, call::get);
    }

    /** Records a step that has already happened, for context that is not a call. */
    public static void note(String name) {
        Allure.step(name);
    }
}
