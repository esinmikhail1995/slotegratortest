package com.slotegrator.qa.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.slotegrator.qa.client.AuthFeignClient;
import com.slotegrator.qa.config.TestConfig;
import com.slotegrator.qa.dto.CredentialsDto;
import com.slotegrator.qa.dto.LoginResponseDto;
import com.slotegrator.qa.http.ApiResponse;
import com.slotegrator.qa.http.FeignClients;
import com.slotegrator.qa.http.ResponseMapper;

/** Typed facade over {@link AuthFeignClient}. */
public final class AuthApi {

    /** The status the login endpoint actually answers with; the contract documents 200. */
    public static final int LOGIN_SUCCESS_STATUS = 201;

    private static final TypeReference<LoginResponseDto> LOGIN = new TypeReference<>() {
    };

    private final AuthFeignClient client = FeignClients.create(AuthFeignClient.class);

    public ApiResponse<LoginResponseDto> login(CredentialsDto credentials) {
        return ResponseMapper.map(
                FeignClients.basicAuthHeader()
                        .map(header -> client.login(header, credentials))
                        .orElseGet(() -> client.login(credentials)),
                LOGIN);
    }

    /**
     * Login with no {@code Authorization: Basic} header, whatever the configuration says — for checking
     * whether the BasicAuth layer the contract declares is actually enforced.
     */
    public ApiResponse<LoginResponseDto> loginWithoutBasicAuth(CredentialsDto credentials) {
        return ResponseMapper.map(client.login(credentials), LOGIN);
    }

    /** Login with the tester credentials from the configuration. */
    public ApiResponse<LoginResponseDto> loginAsTester() {
        return login(new CredentialsDto(TestConfig.testerEmail(), TestConfig.testerPassword()));
    }
}
